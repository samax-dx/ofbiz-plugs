package OfbizSpring.Controller;

import OfbizSpring.Annotations.Authorize;
import OfbizSpring.Annotations.OfbizService;
import OfbizSpring.Util.JwtHelper;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.GenericServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class OfbizServiceProxy {
    @Autowired
    private Delegator delegator;

    @Autowired
    private LocalDispatcher dispatcher;

    @OfbizService
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/runService",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Map<String, Object> execute(@RequestBody Map<String, Object> params) throws GenericServiceException {
        return dispatcher.runSync("xRunSync", params);
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/login",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object login(HttpServletRequest request) throws GenericServiceException {
        return UtilMisc.toMap("token", new JwtHelper().getUserToken(UtilMisc.toMap("partyId", "10000", "loginId", "admin")));
    }

    @Authorize
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/profile",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object profile(HttpServletRequest request, @RequestAttribute Map<String, String> signedParty) throws GenericServiceException {
        return signedParty;
    }
}
