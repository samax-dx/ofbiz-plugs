import OfbizSpring.Util.HttpUtil
import SmsGateway.SmsTaskException
import SmsGateway.http.SmsProviderHttp
import SmsGateway.http.EndpointBrilliant
import SmsGateway.util.SmsUtil
import org.apache.ofbiz.accounting.payment.BillingAccountWorker
import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.entity.GenericEntityException
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
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.stream.Collectors

import static OfbizSpring.Util.MapUtil.remap

List<String> getPartyBillingAccountIds(String partyId) {
	return from("BillingAccountRole")
			.where(UtilMisc.toMap("partyId", partyId))
			.queryList()
			.stream()
			.map({ v -> v.get("billingAccountId") })
			.collect(Collectors.toList())
}

///TODO: Retrieve `externalAccountId` from configuration
GenericValue getPartyBillingAccount(String partyId) {
	ArrayList<String> partyBillingAccounts = getPartyBillingAccountIds(partyId)

	try {
		return from("BillingAccount")
				.where(
						EntityCondition.makeCondition("billingAccountId", EntityOperator.IN, partyBillingAccounts),
						EntityCondition.makeCondition("externalAccountId", EntityOperator.EQUALS, "Main")
				)
				.queryFirst()
	} catch (GenericEntityException e) {
		return null
	}
}

///TODO: Decide whether to use `from BillingAccount where description like partyId-pisTag` or not
GenericValue getPartyProductBillingAccount(String partyId, String pisTag) { // <pisTag = Product's Inventory Selection Tag> <retrievable from `product-comment` and `billing-account-description`>
	ArrayList<String> partyBillingAccounts = getPartyBillingAccountIds(partyId)

	return from("BillingAccount")
			.where(
					EntityCondition.makeCondition("billingAccountId", EntityOperator.IN, partyBillingAccounts),
					EntityCondition.makeCondition("description", EntityOperator.LIKE, "%${pisTag}%".toString())
			)
			.queryFirst()
}

///TODO: Decide whether to use `from BillingAccount where description like partyId-pisTag` or not
GenericValue getOrCreatePartyProductBillingAccount(String partyId, String pisTag) {
	ArrayList<String> partyBillingAccounts = getPartyBillingAccountIds(partyId)

	def account = from("BillingAccount")
			.where(
					EntityCondition.makeCondition("billingAccountId", EntityOperator.IN, partyBillingAccounts),
					EntityCondition.makeCondition("description", EntityOperator.LIKE, "%${pisTag}%".toString())
			)
			.queryFirst()

	if (account == null) {
		def accountId = runService("createBillingAccountAndRole", UtilMisc.toMap(
				"roleTypeId", "BILL_TO_CUSTOMER",
				"availableBalance", "0",
				"accountLimit", 0,
				"accountCurrencyUomId", "USD",
				"partyId", partyId,
				"description", "${partyId}_${pisTag}".toString()
		)).billingAccountId as String

		account = from("BillingAccount")
				.where(EntityCondition.makeCondition("billingAccountId", EntityOperator.EQUALS, accountId))
				.queryFirst()
	}

	return account
}

String getProductAttributeFromComment(String productId, String attrName) {
	GenericValue product = from("Product").where("productId", parameters.productId).queryFirst()

	if (product == null) {
		return null
	}

	Matcher matcher = Pattern
			.compile("(?<=\\[" + attrName + "=).*?(?=\\])")
			.matcher((String) product.get("comments"))

	if (matcher.find()) {
		return matcher.group(0)
	}

	return null
}

Number getProductUnitPoint(String productId) {
	return NumberFormat.getInstance().parse(getProductAttributeFromComment(productId, "unitpoint"))
}

String getProductPisTag(String productId) {
	return getProductAttributeFromComment(productId, "pistag")
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

Map<String, Object> addPartyProductBalance(parameters) {
	Map<String, Object> svcOut
	try {
		Number amount = NumberFormat.getInstance().parse(parameters.soldQuantity as String) * getProductUnitPoint((String) parameters.productId)

		Map<String, Object> payment = runService("createPaymentAndFinAccountTrans", UtilMisc.toMap(
				"statusId", "PMNT_NOT_PAID",
				"currencyUomId", "USD",
				"isDepositWithDrawPayment", "Y",
				"finAccountTransTypeId", "DEPOSIT",
				"partyIdTo", "Company",
				"partyIdFrom", parameters.get("partyId"),
				"paymentTypeId", "CUSTOMER_DEPOSIT",
				"paymentMethodId", "PETTY_CASH",
				"amount", amount
		))

		Map<String, Object> paymentReceiveStatus = runService("setPaymentStatus", UtilMisc.toMap(
				"statusId", "PMNT_RECEIVED",
				"paymentId", payment.get("paymentId")
		))

		Map<String, Object> paymentApplication = runService("createPaymentApplication", UtilMisc.toMap(
				"paymentId", payment.paymentId,
				"billingAccountId", getOrCreatePartyProductBillingAccount((String) parameters.partyId, getProductPisTag((String) parameters.productId)).billingAccountId
		))

		Map<String, Object> paymentConfirmStatus = runService("setPaymentStatus", UtilMisc.toMap(
				"statusId", "PMNT_CONFIRMED",
				"paymentId", payment.get("paymentId")
		))

		svcOut = remap(success(null))
		svcOut.partyId = parameters.partyId
		svcOut.productId = parameters.productId
		svcOut.amount = amount.toString()
	} catch (Exception e) {
		svcOut = remap(error(e.message))
		svcOut.partyId = null
		svcOut.productId = null
		svcOut.amount = null
	}
	return svcOut
}

///TODO: Retrieve `externalAccountId` from configuration
Map<String, Object> spCreateParty() {
	Map<String, Object> svcOut
	try {
		Map<String, Object> party = runService("createPartyGroup", UtilMisc.toMap(
			"groupName", parameters.get("name")
		))

		Map<String, Object> login = runService("createUserLogin", UtilMisc.toMap(
				"partyId", party.partyId,
				"userLoginId", parameters.get("username"),
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
			"extension", parameters.get("contactMech.extension"),
			"partyId", party.partyId
		))

		Map<String, Object> billingAc = runService("createBillingAccountAndRole", UtilMisc.toMap(
			"roleTypeId", "BILL_TO_CUSTOMER",
			"availableBalance", "0",
			"accountLimit", 0,
			"accountCurrencyUomId", "USD",
			"partyId", party.partyId,
			"description", "Main Billing Account",
			"externalAccountId", "Main"
		))

		svcOut = remap(success(null))
		svcOut.partyId = party.partyId
	} catch (Exception e) {
		svcOut = remap(error(e.message))
		svcOut.partyId = null
	}
	return svcOut
}

Map<String, Object> spAddPartyBalance() {
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

		Map<String, Object> paymentReceiveStatus = runService("setPaymentStatus", UtilMisc.toMap(
				"statusId", "PMNT_RECEIVED",
				"paymentId", payment.paymentId
		))

		Map<String, Object> paymentApplication = runService("createPaymentApplication", UtilMisc.toMap(
				"paymentId", payment.paymentId,
				"billingAccountId", getPartyBillingAccount(parameters.get("partyId").toString()).billingAccountId
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
				"billingAccountId", getPartyBillingAccount(payment.get("partyIdFrom").toString()).billingAccountId
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

Map<String, Object> spAddPartyProductBalance() {
	return addPartyProductBalance(parameters)
}

Map<String, Object> spAddPartyProductBalanceForOrder() {
	Map<String, Object> orderItem = from("OrderItem")
			.where(UtilMisc.toMap("orderId", parameters.orderId, "productId", parameters.productId))
			.queryFirst()

	parameters.soldQuantity = orderItem.quantity

	Map<String, Object> balance = addPartyProductBalance(parameters)
	return balance
}

///TODO: Retrieve `productStoreId, CURRENT_CATALOG_ID, currencyUomId` from configuration
Map<String, Object> spCreateOrder() {
	def request = HttpUtil.toWebRequest(parameters.request, userLogin, delegator, dispatcher);
	def response = HttpUtil.toWebResponse(parameters.response);

	String partyId = request.getParameter("partyId")
	String productId = request.getParameter("productId")
	GenericValue billingAccount = getPartyBillingAccount(partyId)

	request.setParam("productStoreId", request.getParameter("storeId"))
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

	return createOrder(request, response);
}

///TODO: Retrieve `productStoreId, CURRENT_CATALOG_ID, currencyUomId` from configuration
Map<String, Object> spCreateSmsPackageOrder() {
	def request = HttpUtil.toWebRequest(parameters.request, userLogin, delegator, dispatcher)
	def response = HttpUtil.toWebResponse(parameters.response)

	String partyId = request.getAttribute("signedParty").get("partyId")
	String productId = request.getParameter("productId")
	GenericValue billingAccount = getPartyBillingAccount(partyId)

	request.setParam("productStoreId", request.getParameter("storeId"))
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

	return svcOut
}

///TODO: Retrieve `productStoreId, CURRENT_CATALOG_ID, currencyUomId` from configuration
Map<String, Object> createSmsUnitOrder(request, response) {
	String partyId = request.getAttribute("signedParty").get("partyId")
	String productId = "SMS_UNITPOINT_V1"
	GenericValue billingAccount = getPartyProductBillingAccount(partyId, (String) request.getParameter("pisTag"))

	request.setParam("productStoreId", request.getParameter("storeId"))
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

	return svcOut
}

Map<String, Object> spCreateSmsUnitOrder() {
	def request = HttpUtil.toWebRequest(parameters.request, userLogin, delegator, dispatcher)
	def response = HttpUtil.toWebResponse(parameters.response)

	request.setParam("pisTag", "domestic")

	return createSmsUnitOrder(request, response)
}

Map<String, Object> spSendSmsBrilliant() {
	def request = HttpUtil.toWebRequest(parameters.request, userLogin, delegator, dispatcher);
	def response = HttpUtil.toWebResponse(parameters.response);

	String smsConsumerPartyId = parameters.SmsConsumerPartyId as String
	Map<String, Object> smsGatewayConfig = parameters.SmsGatewayConfig as Map<String, Object>
	Map<String, Object> requestPayload = request.getParams()

	int requestSmsQuantity = SmsUtil.contactCount((String) requestPayload.MobileNumbers) * SmsUtil.smsCount((String) requestPayload.Message)
	BigDecimal partySmsBalance  = BillingAccountWorker.getBillingAccountAvailableBalance(getPartyProductBillingAccount(smsConsumerPartyId, (String) requestPayload.get("pisTag"))) * -1

	if (requestSmsQuantity > partySmsBalance) {
		return ServiceUtil.returnFailure("Insufficient balance")
	}

	Map<String, Object> svcOut;
	try {
		svcOut = ServiceUtil.returnSuccess()
		svcOut.report = new SmsProviderHttp(new EndpointBrilliant(smsGatewayConfig)).sendSms(requestPayload)

		if (svcOut.report == null) {
			svcOut.report = ""
			return svcOut
		}

		request.clearParams()
		request.setParams(UtilMisc.toMap(
				"partyId", smsConsumerPartyId,
				"storeId", requestPayload.get("storeId"),
				"pisTag", requestPayload.get("pisTag"),
				"quantity", new ObjectMapper().readValue(svcOut.report, Map.class).getAt("Data").size().toString()
		))

		createSmsUnitOrder(request, response)
	} catch (Exception e) {
		svcOut = ServiceUtil.returnFailure(e.getMessage())
		svcOut.report = e.getMessage()
	}
	return svcOut
}
