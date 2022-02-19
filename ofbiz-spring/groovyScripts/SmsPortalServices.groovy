import SmsGateway.ISmsProvider
import SmsGateway.SmsTaskException
import SmsGateway.http.SmsProviderHttp
import SmsGateway.http.EndpointBrilliant
import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.order.shoppingcart.CheckOutEvents
import org.apache.ofbiz.order.shoppingcart.ShoppingCartEvents
import org.apache.ofbiz.service.ServiceUtil

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import static OfbizSpring.Util.MapUtil.remap

Map<String, Object> spCreateParty() {
	Map<String, Object> svcOut
	try {
		Map<String, Object> party = runService("createPartyGroup", UtilMisc.toMap(
			"groupName", parameters.get("name")
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
			"description", "${party.partyId} Billing Account"
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
				"currencyUomId", parameters.get("currency"),
				"isDepositWithDrawPayment", "Y",
				"finAccountTransTypeId", "DEPOSIT",
				"partyIdTo", parameters.get("companyId") == null ? "Company" : parameters.get("companyId"),
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
				"billingAccountId", from("BillingAccountRole").where(UtilMisc.toMap("partyId", parameters.get("partyId"))).queryFirst().get("billingAccountId")
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

Map<String, Object> spCreateOrder() {
	HttpServletRequest request = parameters.get("request");
	HttpServletResponse response = parameters.get("response");

	request.setAttribute("dispatcher", dispatcher);
	request.setAttribute("delegator", delegator);
	request.setAttribute("servletContext", request.getServletContext());
	request.setAttribute("security", security);
	request.getSession().setAttribute("userLogin", userLogin);
	request.getServletContext().setAttribute("delegator", delegator);

	String status = null;
	status = ShoppingCartEvents.initializeOrderEntry(request, response);
	status = ShoppingCartEvents.setOrderCurrencyAgreementShipDates(request, response);
	status = ShoppingCartEvents.addToCart(request, response);
	status = CheckOutEvents.setQuickCheckOutOptions(request, response);
//	status = CheckOutEvents.createOrder(request, response);
	return status.equals("success") ? ServiceUtil.returnSuccess() : ServiceUtil.returnFailure();
}

Map<String, Object> spSendSmsBrilliant() {
	Map<String, Object> endpointConfig = remap(parameters.get("config"))
	Map<String, Object> requestPayload = remap(parameters.get("payload"))
	ISmsProvider smsProvider = new SmsProviderHttp(new EndpointBrilliant(endpointConfig))

	Map<String, Object> svcOut = remap(success(null))
	try {
		svcOut.report = smsProvider.sendSms(requestPayload)
		svcOut.report == null && (svcOut.report = "")
	} catch (Exception e) {
		svcOut.report = e.getMessage()
	}
	return svcOut
}

/*
def runServiceV1() {
	try {
		def svc_name = parameters.get("method")
		def svc_params = (HashMap) parameters.get("args")
		def model_svc = dctx.getModelService(svc_name)
		
		def svc_context = new HashMap()
		if (svc_params != null) {
			def svcInputIterator = model_svc.getInParamNames().iterator()
			
			while (svcInputIterator.hasNext()) {
				def name = svcInputIterator.next()
				def value = svc_params.get(name)
				
				if (value == null) {
					continue
				}
	
				if (value instanceof String && ((String) value).length() == 0) {
					value = null
				}
	
				svc_context.put(name, value)
			}
		}
		svc_context.put("userLogin", parameters.userLogin)
		
		def result = dispatcher.runSync(svc_name, svc_context)
		
		def svc_output = null
		if (ServiceUtil.isError(result)) {
			svc_output = failure()
			svc_output.error = result
		} else {
			if (result.listIt instanceof EntityListIterator) {
				result.listIt = result.listIt.getCompleteList()
			}
			svc_output = success()
			svc_output.result = result
		}
		return svc_output
	} catch (Exception e) {
		def svc_output = failure()
		svc_output.error = e.message
		return svc_output
	}
}

def runServiceV2() {
	def svc_output = null
	try {
		def svc_name = parameters.get("method")
		def svc_params = (HashMap) parameters.get("args")
		
		def svc_context = new HashMap()
		if (svc_params != null) {
			for (ModelParam param : dctx.getModelService(svc_name).getInModelParamList()) {
				def name = param.getName()
				def value = svc_params.get(name)
				
				if (value == null) {
					continue
				}
				
				if (value instanceof String && ((String) value).length() == 0) {
					value = null
				}
				
				svc_context.put(name, value)
			}
		}
		svc_context.put("userLogin", parameters.userLogin)
		
		def result = dispatcher.runSync(svc_name, svc_context)
		
		if (ServiceUtil.isError(result)) {
			svc_output = failure()
			svc_output.error = result
		} else {
			svc_output = success()
			svc_output.result = result
			
			if (result.listIt instanceof EntityListIterator) {
				result.listIt = result.listIt.getCompleteList()
			}
		}
	} catch (Exception e) {
		svc_output = failure()
		svc_output.error = e.message
	}
	return svc_output
}
*/
