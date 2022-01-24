import org.apache.ofbiz.entity.util.EntityListIterator
import org.apache.ofbiz.service.ServiceUtil

Map<String, Object> execute() {
    Map<String, Object> svc_output
    try {
        String svc_name = parameters.get("method")
        Map<String, Object> svc_params = (HashMap) parameters.get("params")

        Map<String, Object> result = dispatcher.runSync(svc_name, svc_params)

        if (ServiceUtil.isError(result)) {
            String errorMessage = ServiceUtil.getErrorMessage(result)
            svc_output = _remap(error(errorMessage))
            svc_output.error = result
        } else {
            String successMessage = null
            svc_output = _remap(success(successMessage))
            svc_output.result = _remapIt(result)
        }
    } catch (Exception e) {
        svc_output = _remap(error(e.message))
        svc_output.error = e.message
    }
    return svc_output
}

static Map<String, Object> _remapIt(Map<String, Object> data) {
    if (data.listIt instanceof EntityListIterator) {
        EntityListIterator i = (EntityListIterator) data.listIt
        data.listIt = i.getCompleteList()
    }
    return data
}

static Map<String, Object> _remap(Object data) {
    return (Map<String, Object>) data
}
