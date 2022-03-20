package OfbizSpring.Controller;

import OfbizSpring.Annotations.Authorize;
import OfbizSpring.Util.ServiceContextUtil;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/SmsTask")
public class SmsTask {
    @Autowired
    private Delegator delegator;

    @Autowired
    private LocalDispatcher dispatcher;

    @Resource(name = "SmsSenderServiceName")
    private String smsSenderServiceName;

    @Resource(name = "SmsGatewayConfig")
    private Map<String, Object> smsGatewayConfig;

    @Authorize
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/sendSms",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object sendSms(HttpServletRequest request, HttpServletResponse response, @RequestAttribute Map<String, String> signedParty) throws GenericServiceException {
//        Map<String, Object> smsSenderServiceArgs = Stream
//                .concat(smsSenderServiceConfig.entrySet().stream(), payload.entrySet().stream())
//                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
//        new GsonJsonParser().parseMap(request.getReader().lines().collect(Collectors.joining()))

        HashMap<String, Object> smsSenderServiceArgs = new HashMap<>();

        smsSenderServiceArgs.put("SmsGatewayConfig", smsGatewayConfig);
        smsSenderServiceArgs.put("SmsConsumerPartyId", signedParty.get("partyId"));
        smsSenderServiceArgs.put("request", request);
        smsSenderServiceArgs.put("response", response);

        return dispatcher.runSync(smsSenderServiceName, ServiceContextUtil.authorizeContext(smsSenderServiceArgs));
    }
}
