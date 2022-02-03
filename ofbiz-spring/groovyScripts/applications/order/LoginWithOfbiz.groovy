package applications.order

import org.apache.ofbiz.service.ServiceUtil
import static OfbizSpring.Util.MapUtil.remap
import static OfbizSpring.Util.MapUtil.remapIt
//import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.base.util.UtilMisc;
import java.util.HashMap
Map<String, Object> authenticate() {
    Map<String, Object> svc_output
    try {
        String serviceName = parameters.get("method")
        Object params =parameters.get("params")
        HashMap<String, Object> testMap = new HashMap<String,Object>()

        testMap.put("testVal", UtilMisc.toMap("a","aa"))
        Map<String, Object> result = dispatcher.runSync(serviceName, params)

        if (ServiceUtil.isError(result)) {
            String errorMessage = ServiceUtil.getErrorMessage(result)
            svc_output = remap(error(errorMessage))
            svc_output.error = result
        } else {
            String successMessage = null
            svc_output = remap(success(successMessage))
            svc_output.result = remapIt(result)
        }
    } catch (Exception e) {
        svc_output = remap(error(e.message))
        svc_output.error = e.message
    }
    return svc_output
}
