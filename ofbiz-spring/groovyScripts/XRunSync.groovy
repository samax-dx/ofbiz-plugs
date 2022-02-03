import org.apache.ofbiz.service.ServiceUtil
import static OfbizSpring.Util.MapUtil.remap
import static OfbizSpring.Util.MapUtil.remapIt


Map<String, Object> execute() {
    Map<String, Object> svc_output

    try {
        String serviceName = parameters.get("method")
        Map<String, Object> serviceParams = (HashMap) parameters.get("params")

        serviceParams.put("login.username", "admin")
        serviceParams.put("login.password", "ofbiz")
        serviceParams.put("method",serviceName)
        //serviceParams.put("params",)

        Map<String, Object> result = dispatcher.runSync(serviceName, serviceParams)

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
