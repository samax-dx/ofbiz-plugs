import org.apache.ofbiz.entity.GenericEntityException;


def createOfbizDemo() {
    result = [:];
    try {
        ofbizDemo = delegator.makeValue("kvlist");
        // Auto generating next sequence of kvlistId primary key
        ofbizDemo.setNextSeqId();
        // Setting up all non primary key field values from context map
        ofbizDemo.setNonPKFields(context);
        // Creating record in database for OfbizDemo entity for prepared value
        ofbizDemo = delegator.create(ofbizDemo);
        result.kvlistId = ofbizDemo.kvlistId;
        logInfo("==========This is my first Groovy Service implementation in Apache OFBiz. OfbizDemo record "
                  +"created successfully with kvlistId: "+ofbizDemo.getString("kvlistId"));
      } catch (GenericEntityException e) {
          logError(e.getMessage());
          return error("Error in creating record in OfbizDemo entity ........");
      }
      return result;
}