import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.service.ModelService
import org.apache.ofbiz.service.ModelParam
import org.apache.ofbiz.service.ServiceUtil

import com.sun.xml.internal.ws.resources.UtilMessages

import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.entity.util.EntityListIterator

def runService() {
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
			svc_output = error(ServiceUtil.getErrorMessage(result))
			svc_output.error = result
		} else {
			svc_output = success()
			svc_output.result = result
			
			if (result.listIt instanceof EntityListIterator) {
				result.listIt = result.listIt.getCompleteList()
			}
		}
	} catch (Exception e) {
		svc_output = error(e.message)
		svc_output.error = e.message
	}
	return svc_output
}

def insertXPair() {
	try {
		def result = success()
		
		def xpair = delegator.makeValue("xpair", [
			"xpairId": delegator.getNextSeqId("xpair"),
			"itemKey": parameters.get("itemKey"),
			"itemValue": parameters.get("itemValue"),
		])
		
		result.data = xpair.create()
		
		return result
	} catch (Exception e) {
		return failure(e.message)
	}
}

def getXPairs() {
	try {
		def result = success()
		
		result.data = from("xpair")
			.queryList()
		
		return result
	} catch (Exception e) {
		return failure(e.message)
	}
}

def getXPair() {
	try {
		def result = success()
		
		result.data = from("xpair")
			.where("xpairId", parameters.get("xpairId"))
			.queryOne()
		
//		System.out.print("\n\n start \n")
//		System.out.print( from("xpair").where("xpairId", parameters.get("xpairId")).queryOne() )
//		System.out.print("\n end \n\n")
	
		return result
	} catch (Exception e) {
		return failure(e.message)
	}
}

def updateXPair() {
	String itemId = parameters.get("xpairId")
	String itemKey = parameters.get("itemKey")
	String itemValue = parameters.get("itemValue")
	
	try {
		def result = success()
		
		def xpair = from("xpair")
			.where("xpairId", itemId)
			.queryOne()
		
		if (xpair == null) {
			throw new Exception("xpair not found")
		}
		
		if (itemKey != null) {
			xpair.put("itemKey", itemKey)
		}
		
		if (itemValue != null) {
			xpair.put("itemValue", itemValue)
		}
		
		xpair.store()
		result.data = xpair
		
		return result
	} catch (Exception e) {
		return failure(e.message)
	}
}

def deleteXPair() {
	String itemId = parameters.get("xpairId")
	
	try {
		def result = success()
		
		def xpair = from("xpair")
			.where("xpairId", itemId)
			.queryOne()
		
		if (xpair == null) {
			throw new Exception("xpair not found")
		}
		
		xpair.remove()
		result.data = xpair.clone()
		result.data.put("xpairId", null)
		
		return result
	} catch (Exception e) {
		return failure(e.message)
	}
}
