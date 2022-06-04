import OfbizSpring.Util.HttpUtil
import SmsGateway.ISmsProvider
import SmsGateway.SmsTaskException
import SmsGateway.util.SmsUtil
import TeleCampaign.Models.CampaignTask
import TeleCampaign.CampaignTaskProvider

import org.apache.ofbiz.accounting.payment.BillingAccountWorker
import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.order.shoppingcart.CheckOutEvents
import org.apache.ofbiz.order.shoppingcart.ShoppingCart
import org.apache.ofbiz.order.shoppingcart.ShoppingCartEvents
import org.apache.ofbiz.order.OrderManagerEvents
import org.apache.ofbiz.service.ServiceUtil
import com.fasterxml.jackson.databind.ObjectMapper

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.function.Function
import java.util.stream.Collectors
import java.util.stream.Stream

import static OfbizSpring.Util.MapUtil.remap


GenericValue getPartyServiceBillingAccount(String partyId, String service) { // <lineup = Product's Inventory Selection Tag> <retrievable from `product-attributes` and `billing-account-description`>
	GenericValue billingAccount = from("PartyAttribute")
			.where(
					EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId),
					EntityCondition.makeCondition("attrName", EntityOperator.EQUALS, "${service}_billing".toString())
			)
			.queryFirst()

	if (billingAccount == null) {
		return null
	}

	return from("BillingAccount").where("billingAccountId", billingAccount.get("attrValue")).queryFirst()
}

String getProductAttribute(String productId, String attrName) {
	GenericValue product = from("ProductAttribute").where("productId", productId, "attrName", attrName).queryFirst()
	return product == null ? null : (String) product["attrValue"]
}

Number getProductUnitPoint(String productId) {
	return NumberFormat.getInstance().parse(getProductAttribute(productId, "unitpoint"))
}

String getProductLineup(String productId) {
	return getProductAttribute(productId, "lineup")
}

String getProductStore(String productId, String forcedStore) {
	if (forcedStore == null || forcedStore.trim().length() == 0) {
		return getProductAttribute(productId, "store")
	}
	return forcedStore
}


///TODO: Implement party `roleTypeId` choice functionality
Map<String, Object> spCreateParty() {
	Map<String, Object> svcOut
	try {
		Map<String, Object> party = runService("createPartyGroup", UtilMisc.toMap(
				"groupName", parameters.get("name")
		))

		Map<String, Object> login = runService("createUserLogin", UtilMisc.toMap(
				"partyId", party.partyId,
				"userLoginId", parameters.get("loginId"),
				"currentPassword", parameters.get("password"),
				"currentPasswordVerify", parameters.get("passwordConfirm")
		))

		Map<String, Object> role = runService("createPartyRole", UtilMisc.toMap(
				"roleTypeId", "CUSTOMER",
				"partyId", party.partyId
		))

		Map<String, Object> tel = runService("createPartyTelecomNumber", UtilMisc.toMap(
				"countryCode", parameters.get("contactMech.countryCode"),
				"areaCode", parameters.get("contactMech.areaCode"),
				"contactNumber", parameters.get("contactMech.contactNumber"),
				"partyId", party.partyId
		))

		Map<String, Object> billingAc = runService("createBillingAccountAndRole", UtilMisc.toMap(
				"roleTypeId", "BILL_TO_CUSTOMER",
				"availableBalance", "0",
				"accountLimit", 0,
				"accountCurrencyUomId", "USD",
				"partyId", party.partyId,
				"description", "Payment billing account"
		))

		Map<String, Object> partyBillingAttrOut = runService("createPartyAttribute", UtilMisc.toMap(
				"partyId", party.partyId,
				"attrName", "payment_billing",
				"attrValue", billingAc.billingAccountId,
				"attrDescription", "payment"
		))

		svcOut = remap(success(null))
		svcOut.partyId = party.partyId
	} catch (Exception e) {
		svcOut = remap(error(e.message))
		svcOut.partyId = null
	}
	return svcOut
}

Map<String, Object> addPartyBalance(Map<String, Object> parameters) {
	Map<String, Object> svcOut
	try {
		Map<String, Object> payment = runService("createPaymentAndFinAccountTrans", UtilMisc.toMap(
				"statusId", "PMNT_NOT_PAID",
				"currencyUomId", "USD",
				"isDepositWithDrawPayment", "Y",
				"finAccountTransTypeId", "DEPOSIT",
				"partyIdTo", "Company",
				"partyIdFrom", parameters.get("partyId"),
				"paymentTypeId", "CUSTOMER_DEPOSIT",
				"paymentMethodId", "PETTY_CASH",
				"amount", parameters.get("amount")
		))

		Map<String, Object> paymentAttrOut = runService("createPaymentAttribute", UtilMisc.toMap(
				"paymentId", payment.paymentId,
				"attrName", "balanceType",
				"attrValue", parameters.balanceType
		))

		Map<String, Object> paymentReceiveStatus = runService("setPaymentStatus", UtilMisc.toMap(
				"statusId", "PMNT_RECEIVED",
				"paymentId", payment.paymentId
		))

		Map<String, Object> paymentApplication = runService("createPaymentApplication", UtilMisc.toMap(
				"paymentId", payment.paymentId,
				"billingAccountId", parameters.billingAccountId
		))

		Map<String, Object> paymentConfirmStatus = runService("setPaymentStatus", UtilMisc.toMap(
				"statusId", "PMNT_CONFIRMED",
				"paymentId", payment.paymentId
		))

		svcOut = ServiceUtil.returnSuccess()
		svcOut.amount = parameters.get("amount")
		svcOut.paymentId = payment.paymentId
		svcOut.partyId = parameters.get("partyId")
	} catch (Exception e) {
		svcOut = ServiceUtil.returnError(e.message)
		svcOut.paymentId = null
	}
	return svcOut
}

// TODO: retrieve `balanceType` from `party-attributes`
Map<String, Object> spAddPartyBalance() {
	parameters.put("billingAccountId", getPartyServiceBillingAccount((String) parameters.partyId, "payment").billingAccountId)
	parameters.put("balanceType", "payment_billing")
	return addPartyBalance(parameters)
}

Map<String, Object> spAddPartyBalanceRequest() {
	Map<String, Object> svcOut
	try {
		Map<String, Object> payment = runService("createPaymentAndFinAccountTrans", UtilMisc.toMap(
				"statusId", "PMNT_NOT_PAID",
				"currencyUomId", "USD",
				"isDepositWithDrawPayment", "Y",
				"finAccountTransTypeId", "DEPOSIT",
				"partyIdTo", "Company",
				"partyIdFrom", parameters.get("partyId"),
				"paymentTypeId", "CUSTOMER_DEPOSIT",
				"paymentMethodId", "PETTY_CASH",
				"amount", parameters.get("amount")
		))

		Map<String, Object> paymentAttrOut = runService("createPaymentAttribute", UtilMisc.toMap(
				"paymentId", payment.paymentId,
				"attrName", "balanceType",
				"attrValue", "payment_billing"
		))

		svcOut = remap(success(null))
		svcOut.paymentId = payment.paymentId
	} catch (Exception e) {
		svcOut = remap(error(e.message))
		svcOut.paymentId = null
	}
	return svcOut
}

Map<String, Object> spAddPartyBalanceConfirm() {
	Map<String, Object> svcOut
	try {
		Map<String, Object> payment = from("Payment")
				.where(UtilMisc.toMap("paymentId", parameters.paymentId))
				.queryFirst()

		Map<String, Object> paymentReceiveStatus = runService("setPaymentStatus", UtilMisc.toMap(
				"statusId", "PMNT_RECEIVED",
				"paymentId", payment.paymentId
		))

		Map<String, Object> paymentApplication = runService("createPaymentApplication", UtilMisc.toMap(
				"paymentId", payment.paymentId,
				"billingAccountId", getPartyServiceBillingAccount(payment.get("partyIdFrom").toString(), "payment").billingAccountId
		))

		Map<String, Object> paymentConfirmStatus = runService("setPaymentStatus", UtilMisc.toMap(
				"statusId", "PMNT_CONFIRMED",
				"paymentId", payment.paymentId
		))

		svcOut = remap(success(null))
		svcOut.paymentId = payment.paymentId
	} catch (Exception e) {
		svcOut = remap(error(e.message))
		svcOut.paymentId = null
	}
	return svcOut
}

Map<String, Object> spAddPartyBalanceCancel() {
	Map<String, Object> svcOut
	try {
		Map<String, Object> payment = parameters;

		Map<String, Object> paymentConfirmStatus = runService("setPaymentStatus", UtilMisc.toMap(
				"statusId", "PMNT_CANCELLED",
				"paymentId", payment.paymentId
		))

		svcOut = remap(success(null))
		svcOut.paymentId = payment.paymentId
	} catch (Exception e) {
		svcOut = remap(error(e.message))
		svcOut.paymentId = null
	}
	return svcOut
}

Map<String, Object> createOrder(request, response, currentBalance = -1.0) {
	Map<String, Object> svcOut = ServiceUtil.returnSuccess()

	ShoppingCartEvents.destroyCart(request, response);
	ShoppingCartEvents.initializeOrderEntry(request, response);
	ShoppingCartEvents.setOrderCurrencyAgreementShipDates(request, response);
	ShoppingCartEvents.addToCart(request, response);

	ShoppingCart cart = (ShoppingCart) request.getSession().getAttribute("shoppingCart");

	svcOut.totalPrice = new DecimalFormat("0.00").format(cart.getGrandTotal());
	if (currentBalance > -1 && currentBalance < new DecimalFormat().parse((String) svcOut.totalPrice)) {
		return ServiceUtil.returnFailure("Insufficient balance");
	}

	CheckOutEvents.setQuickCheckOutOptions(request, response);
	CheckOutEvents.createOrder(request, response);
	svcOut.orderId = cart.getOrderId();

	request.setParam("orderId", cart.getOrderId());
	OrderManagerEvents.receiveOfflinePayment(request, response);

	return svcOut;
}

///TODO: Retrieve `CURRENT_CATALOG_ID, currencyUomId` from configuration
Map<String, Object> spCreateOrder() {
	def request = HttpUtil.toWebRequest(parameters.request, userLogin, delegator, dispatcher)
	def response = HttpUtil.toWebResponse(parameters.response)

//	String partyId = request.getAttribute("signedParty").get("partyId")
	String partyId = parameters.partyId == null ? request.getParameter("partyId") : parameters.partyId
	String productId = request.getParameter("productId")
	GenericValue billingAccount = getPartyServiceBillingAccount(partyId, "payment")

	request.setParam("productStoreId", getProductStore(productId, (String) request.getParameter("storeId")))
	request.setParam("CURRENT_CATALOG_ID", "DemoCatalog")
	request.setParam("add_product_id", productId)
	request.setParam("orderName", "${productId} ${System.currentTimeMillis()}".toString())
	request.setParam("orderMode", "SALES_ORDER")
	request.setParam("billingAccountId", billingAccount.billingAccountId)
	request.setParam("currencyUomId", "USD")
	request.setParam("hasAgreements", "N")
	request.setParam("shipping_method", "NO_SHIPPING@_NA_")
	request.setParam("may_split", "false")
	request.setParam("is_gift", "false")
	request.setParam("shipToCustomerPartyId", partyId)
	request.setParam("checkoutpage", "quick")
	request.setParam("BACK_PAGE", "quickcheckout")
	request.setParam("quantity", request.getParameter("quantity"))

	Map<String, Object> svcOut = createOrder(request, response, BillingAccountWorker.getBillingAccountAvailableBalance(billingAccount) * -1)

	svcOut.put("partyId", partyId)
	svcOut.put("productId", productId)
	svcOut.put("quantity", request.getParameter("quantity"))

	return svcOut
}

Map<String, Object> spAddPartyProductBalanceForOrder() {
	String productLineup = getProductLineup((String) parameters.productId)
	String orderQuantity = from("OrderItem")
			.where(UtilMisc.toMap("orderId", parameters.orderId, "productId", parameters.productId))
			.queryFirst()
			.get("quantity")
			.toString()

	GenericValue billingAccount = getPartyServiceBillingAccount((String) parameters.partyId, productLineup)

	if (billingAccount == null) {
		Map<String, Object> billingAccountNew = runService("createBillingAccountAndRole", UtilMisc.toMap(
				"roleTypeId", "BILL_TO_CUSTOMER",
				"availableBalance", "0",
				"accountLimit", 0,
				"accountCurrencyUomId", "USD",
				"partyId", parameters.partyId,
				"description", "[${productLineup}] Inventory account".toString()
		))

		Map<String, Object> partyBillingAttrOut = runService("createPartyAttribute", UtilMisc.toMap(
				"partyId", parameters.partyId,
				"attrName", "${productLineup}_billing".toString(),
				"attrValue", billingAccountNew.billingAccountId,
				"attrDescription", productLineup
		))

		billingAccount = from("BillingAccount")
				.where(EntityCondition.makeCondition("billingAccountId", billingAccountNew.billingAccountId))
				.queryFirst()
	}

	parameters.amount = NumberFormat.getInstance().parse(orderQuantity.toString()) * getProductUnitPoint((String) parameters.productId)
	parameters.billingAccountId = billingAccount.billingAccountId
	parameters.balanceType = "${productLineup}_billing"

	Map<String, Object> partyProductBalance = addPartyBalance(parameters)
	partyProductBalance.partyId = parameters.partyId
	partyProductBalance.productId = parameters.productId
	partyProductBalance.amount = parameters.amount.toString()

	return partyProductBalance
}

static Map<String, Object> runCampaign(ISmsProvider smsProvider, Map<String, Object> campaign, List<String> campaignTasks) {
	String taskDoc = campaignTasks.join(",");

	String reportDoc
	try {
		reportDoc = smsProvider.sendSms(UtilMisc.toMap(
				"CampaignName", campaign.campaignName,
				"SenderId", campaign.senderId,
				"MobileNumbers", taskDoc,
				"Message", campaign.message
		))
	} catch (Exception e) {
		reportDoc = e.getMessage()
	}

	return UtilMisc.toMap((Map<?, ?>) new ObjectMapper().readValue(reportDoc, Map.class))
}

boolean hasCampaignBalance(String partyId, int campaignTasks, String message, String campaignPackageId) {
	int requestSmsQuantity = campaignTasks * SmsUtil.smsCount(message)
	BigDecimal partySmsBalance  = BillingAccountWorker.getBillingAccountAvailableBalance(getPartyServiceBillingAccount(partyId, campaignPackageId)) * -1
	return partySmsBalance >= requestSmsQuantity
}

void addAllFailedTasks(CampaignTaskProvider campaignTaskProvider, List<String> routeTask, String errorCode, List<GenericValue> acc) {
	routeTask.forEach({task ->
		CampaignTask taskOb = campaignTaskProvider.outboundTasks.get(task)
		if (taskOb == null) {
			return
		}
		CampaignTask taskIb = campaignTaskProvider.inboundTasks.get(taskOb.phoneNumber)
		taskIb.statusText = errorCode

		acc.push(delegator.makeValue("CampaignTask", taskIb.toMap()))
	})
}

Map<String, Object> spRunCampaign() {
	Map<String, Object> campaign = (Map<String, Object>) parameters.campaign

	CampaignTaskProvider campaignTaskProvider = CampaignTaskProvider.create(
			(String) campaign.campaignId,
			((List<Object>) parameters.campaignTasks)
					.stream()
					.map({ task -> task instanceof Map ? task.get("phoneNumber") : (String) task })
					.collect(Collectors.joining(",")),
			from("DialPlanActivePrioritizedView").queryList(),
			from("PartyDialPlanActivePrioritizedView").where("partyId", parameters.partyId).queryList()
	)

	Object report = campaignTaskProvider.routeTasks.entrySet().stream().reduce(
			new Object() {
				public List<GenericValue> handledTasks = new ArrayList<>()
				public int taskCount = 0
				public int success = 0
			},
			{ acc, routeTask ->
				String[] pkg_route = routeTask.getKey().split(":")

				String partyId = (String) parameters.partyId
				String campaignPackage = pkg_route[0]
				List<String> campaignTasks = routeTask.getValue()

				acc.taskCount += campaignTasks.size()

				if (campaignPackage == "") {
					addAllFailedTasks(campaignTaskProvider, campaignTasks, "package_not_found", acc.handledTasks)
					return acc
				}

				if (campaignTasks.size() == 0) {
					return acc
				}

				if (!hasCampaignBalance(partyId, campaignTasks.size(), (String) campaign.message, campaignPackage)) {
					addAllFailedTasks(campaignTaskProvider, campaignTasks, "insufficient_balance", acc.handledTasks)
					return acc
				}

				ISmsProvider smsProvider = ((Function<String, ISmsProvider>) parameters.SmsProvider).apply(pkg_route[1])

				if (smsProvider == null) {
					addAllFailedTasks(campaignTaskProvider, campaignTasks, "route_not_found", acc.handledTasks)
					return acc
				}

				Map<String, Object> report = runCampaign(smsProvider, campaign, campaignTasks)
//				Map<String, Object> report = UtilMisc.toMap("ErrorCode", 0, "Data", campaignTasks.stream().map({
//					t -> UtilMisc.toMap("MessageErrorCode", 0, "MobileNumber", t)
//				}).collect(Collectors.toList()))

				if (Integer.parseInt(report.get("ErrorCode").toString()) != 0) {
					addAllFailedTasks(campaignTaskProvider, campaignTasks, (String) report.get("ErrorCode"), acc.handledTasks)
					return acc
				}

				List<Map<String, Object>> reportTasks = (List<Map<String, Object>>) report.get("Data")

				if (reportTasks.size() == 0) {
					addAllFailedTasks(campaignTaskProvider, campaignTasks, "no_response", acc.handledTasks)
					return acc
				}

				acc.success += reportTasks.stream().reduce(
						0,
						{acc_task, task ->
							CampaignTask taskOb = campaignTaskProvider.outboundTasks.get(task.MobileNumber)
							if (taskOb == null) {
								return acc_task
							}
							CampaignTask taskIb = campaignTaskProvider.inboundTasks.get(taskOb.phoneNumber)

							if (String.valueOf(task.MessageErrorCode) == "0") {
								taskIb.status = "1"
								taskIb.statusText = "success"
								acc.handledTasks.push(delegator.makeValue("CampaignTask", taskIb.toMap()))
								return acc_task + 1
							} else {
								taskIb.status = "0"
								taskIb.statusText = task.MessageErrorCode
								acc.handledTasks.push(delegator.makeValue("CampaignTask", taskIb.toMap()))
								return acc_task + 0
							}
						},
						{ acc_o, acc_n -> acc_n + acc_o }
				)

				if (acc.success == 0) {
					return acc
				}

				addPartyBalance(UtilMisc.toMap(
						"partyId", partyId,
						"amount", String.valueOf(acc.success * -1),
						"billingAccountId", getPartyServiceBillingAccount(partyId, campaignPackage).billingAccountId,
						"balanceType", campaignPackage
				))

				return acc
			},
			{ acc_o, acc_n ->
				acc_n.handledTasks.addAll(0, acc_o.handledTasks)
				acc_n.taskCount += acc_o.taskCount
				acc_n.success += acc_o.success
				return acc_n
			}
	)

	delegator.storeAll(campaign.campaignId == null ? new ArrayList<GenericValue>() : report.handledTasks)

	Map<String, Object> svcOut = ServiceUtil.returnSuccess()
	svcOut.put("report", UtilMisc.toMap(
			"taskCount", report.taskCount,
			"success", report.success,
			"failure", report.taskCount - report.success
	))
	return svcOut
}
