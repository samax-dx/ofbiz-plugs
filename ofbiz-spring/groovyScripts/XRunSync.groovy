import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.service.ServiceUtil
import static OfbizSpring.Util.MapUtil.remapIt

Map<String, Object> execute() {
    Map<String, Object> svc_output
    try {
        String svc_name = parameters.get("method")
        Map<String, Object> svc_params = (HashMap) parameters.get("params")

        svc_params.put("login.username", "admin")
        svc_params.put("login.password", "ofbiz")

        Map<String, Object> result = dispatcher.runSync(svc_name, svc_params)

        if (ServiceUtil.isError(result)) {
            String errorMessage = ServiceUtil.getErrorMessage(result)
            svc_output = UtilMisc.toMap(error(errorMessage))
            svc_output.error = result
        } else {
            String successMessage = null
            svc_output = UtilMisc.toMap(success(successMessage))
            svc_output.result = remapIt(result)
        }
    } catch (Exception e) {
        svc_output = UtilMisc.toMap(error(e.message))
        svc_output.error = e.message
    }
    return svc_output
}
