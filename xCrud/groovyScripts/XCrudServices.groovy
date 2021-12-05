
def insertXPair() {
	try {
		def xpair = delegator.makeValue("xpair", [
			"xpairId": delegator.getNextSeqId("xpair"),
			"itemKey": parameters.get("itemKey"),
			"itemValue": parameters.get("itemValue"),
		])
		
		xpair.create()
		
		return success(xpair.toString())
	} catch (Exception e) {
		return failure(e.message)
	}
}

def getXPairs() {
	try {
		def xpairs = from("xpair")
			.queryList()
		
		return success(xpairs.toString())
	} catch (Exception e) {
		return failure(e.message)
	}
}

def getXPair() {
	try {
		def xpair = from("xpair")
			.where("xpairId", parameters.get("xpairId"))
			.queryOne()
		
//		System.out.print("\n\n start \n")
//		System.out.print( from("xpair").where("xpairId", parameters.get("xpairId")).queryOne() )
//		System.out.print("\n end \n\n")
	
		return success(xpair.toString())
	} catch (Exception e) {
		return failure(e.message)
	}
}

def updateXPair() {
	String itemId = parameters.get("xpairId")
	String itemKey = parameters.get("itemKey")
	String itemValue = parameters.get("itemValue")
	
	try {
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
		
		return success(itemId)
	} catch (Exception e) {
		return failure(e.message)
	}
}

def deleteXPair() {
	String itemId = parameters.get("xpairId")
	
	try {
		def xpair = from("xpair")
			.where("xpairId", itemId)
			.queryOne()
		
		if (xpair == null) {
			throw new Exception("xpair not found")
		}
		
		xpair.remove()
		
		return success()
	} catch (Exception e) {
		return failure(e.message)
	}
}
