import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.base.util.UtilMisc
import xCrud.SvcGeneratorData

def spCreateParty() {
	def link = SvcGeneratorData.spd()
	def svcOut = null
	try {
		def party = runService("createPartyGroup", UtilMisc.toMap(
			"groupName",
			parameters.get("name")
		))
		
		def role = runService("createPartyRole", UtilMisc.toMap(
			"roleTypeId",
			parameters.get("role.roleTypeId"),
			"partyId",
			party.partyId
		))
		
		def tel = runService("createPartyTelecomNumber", UtilMisc.toMap(
			"countryCode",
			parameters.get("contactMech.countryCode"),
			"areaCode",
			parameters.get("contactMech.areaCode"),
			"contactNumber",
			parameters.get("contactMech.contactNumber"),
			"extension",
			parameters.get("contactMech.extension"),
			"partyId",
			party.partyId
		))
		
		svcOut = success()
		svcOut.partyId = party.partyId
		throw new Exception("intentional")
	} catch (Exception e) {
		svcOut = error(e.message)
		svcOut.partyId = null
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
