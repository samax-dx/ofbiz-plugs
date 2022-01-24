package OfbizSpring.Controller;

import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.GenericServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

@RestController
public class OfbizServiceProxy {
    @Autowired
    private Delegator delegator;

    @Autowired
    private LocalDispatcher dispatcher;

    @RequestMapping(
            value = "runService",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Map<String, Object> execute(@RequestBody Map<String, Object> params) throws GenericServiceException {
        params.put("login.username", "admin");
        params.put("login.password", "ofbiz");
        return dispatcher.runSync("xRunSync", params);
    }
}
