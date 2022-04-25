import OfbizSpring.Util.HttpUtil
import SmsGateway.ISmsProvider
import SmsGateway.SmsTaskException
import SmsGateway.util.SmsUtil
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

static Map<String, Object> runCampaign(ISmsProvider smsProvider, Map<String, Object> campaign, Map<String, Map<String, Object>> campaignTasks) {
	String taskDoc = campaignTasks
			.entrySet()
			.stream()
			.map({ v -> v.getKey() })
			.collect(Collectors.toList())
			.join(",")

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

static boolean packageCanDial(Map<String, Object> campaignPackage, String dialNumber) {
	String largest = "${campaignPackage.egressPrefix}${campaignPackage.dialPlanId}".toString()
	String shorter = (String) campaignPackage.dialPlanId
	return dialNumber.startsWith(largest) || dialNumber.startsWith(shorter)
}

List<GenericValue> getServicePlanPackages(List<String> servicePlanPackages) {
	return from("PackageDialPlanPriorityView")
			.where(EntityCondition.makeCondition("packageId", EntityOperator.IN, servicePlanPackages))
			.queryList()
}
boolean hasCampaignBalance(String partyId, int campaignTasks, String campaignMessage, String campaignPackageId) {
	int requestSmsQuantity = campaignTasks * SmsUtil.smsCount((String) campaignMessage)
	BigDecimal partySmsBalance  = BillingAccountWorker.getBillingAccountAvailableBalance(getPartyServiceBillingAccount(partyId, campaignPackageId)) * -1
	return partySmsBalance >= requestSmsQuantity
}
GenericValue makeCampaignTask(Map<String, Object> task, String status, String report) {
	task.status = status
	task.report = report
	return delegator.makeValue("CampaignTask", task)
}

Map<String, Object> spRunCampaign() {
	String partyId = parameters.partyId;
	Map<String, Object> campaign = (Map<String, Object>) parameters.campaign
	Function<String, ISmsProvider> serviceProviders = (Function<String, ISmsProvider>) parameters.serviceProviders
	List<GenericValue> servicePlanPackages = getServicePlanPackages((List<String>) parameters.servicePlanPackages)

	servicePlanPackages.eachWithIndex({ GenericValue p, int iP ->
		String routeId = (String) p.routeId
		String packageId = (String) p.packageId

		Map<String, Map<String, Object>> packageTasks = ((List<Map<String, Object>>) parameters.campaignTasks)
				.stream()
				.map({ task -> packageCanDial(p, (String) task.phoneNumber) ? task : null })
				.filter({ task -> task != null })
				.collect(Collectors.toMap({ v -> (String) v.phoneNumber }, { v -> v }))

		if (packageTasks.size() == 0) {
			return
		}

		if (hasCampaignBalance(partyId, packageTasks.size(), (String) campaign.message, packageId)) {
			Map<String, Object> report = runCampaign(serviceProviders.apply(routeId), campaign, packageTasks)
			boolean isErrorReport = Integer.parseInt(report.get("ErrorCode").toString()) != 0

			List<GenericValue> completeTasks = isErrorReport ? new ArrayList<GenericValue>() : ((List<Map<String, Object>>) report.get("Data"))
					.stream()
					.map({ task ->
						if (String.valueOf(task.MessageErrorCode) == "0") {
							Map<String, Object> completeTask = packageTasks.get(task.MobileNumber)
							return completeTask == null ? null : makeCampaignTask(completeTask, "1", "success") // null check is not necessary for real-world payload
						} else {
							return null
						}
					})
					.filter({ task -> task != null })
					.collect(Collectors.toList())

			if (completeTasks.size() > 0) {
				addPartyBalance(UtilMisc.toMap(
						"partyId", partyId,
						"amount", String.valueOf(completeTasks.size() * -1),
						"billingAccountId", getPartyServiceBillingAccount(partyId, packageId).billingAccountId,
						"balanceType", packageId
				))

				if ((String) campaign.campaignId != null) {
					delegator.storeAll(completeTasks)
				}
			}
		} else {
			servicePlanPackages.remove(iP)
		}
	})

	List<GenericValue> absentServicePlanPackages = from("DialPlanPriorityView")
			.where(EntityCondition.makeCondition(
					"dialPlanId",
					EntityOperator.NOT_IN,
					servicePlanPackages.stream().map({ v -> v.dialPlanId }).collect(Collectors.toList())
			))
			.queryList()

	absentServicePlanPackages.forEach({ p ->
		List<String> errorTasks = new ArrayList<>()
		((List<Map<String, Object>>) parameters.campaignTasks).eachWithIndex { Map<String, Object> task, int i ->
			if (packageCanDial(p, (String) task.phoneNumber)) {
				task.report = "no purchases support ${p.egressPrefix}${p.dialPlanId}".toString()
				errorTasks.push((String) task.phoneNumber)
			}
		}
		errorTasks.size()
	})

	Map<String, Object> svcOut = ServiceUtil.returnSuccess()
	svcOut.put("reports", parameters.campaignTasks)

	return svcOut
}
