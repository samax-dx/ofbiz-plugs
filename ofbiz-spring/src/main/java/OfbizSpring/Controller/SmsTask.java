package OfbizSpring.Controller;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;

import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public class SmsTask {
    @Autowired
    private Delegator delegator;

    @Autowired
    private LocalDispatcher dispatcher;

    @Resource(name = "SmsSenderServiceName")
    private String smsSenderServiceName;

    @Resource(name = "SmsSenderServiceConfig")
    private Map<String, Object> smsSenderServiceConfig;

    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/sendSms",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Map<String, Object> sendSms(@RequestBody Map<String, Object> payload) throws GenericServiceException {
//        Map<String, Object> smsSenderServiceArgs = Stream
//                .concat(smsSenderServiceConfig.entrySet().stream(), payload.entrySet().stream())
//                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        HashMap<String, Object> smsSenderServiceArgs = new HashMap<>();
        smsSenderServiceArgs.put("config", smsSenderServiceConfig);
        smsSenderServiceArgs.put("payload", payload);

        return dispatcher.runSync(smsSenderServiceName, smsSenderServiceArgs);
    }
}
