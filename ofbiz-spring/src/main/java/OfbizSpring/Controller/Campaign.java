package OfbizSpring.Controller;

import OfbizSpring.Annotations.Authorize;
import OfbizSpring.Util.QueryUtil;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/Campaign")
public class Campaign {
    @Autowired
    private Delegator delegator;

    @Autowired
    private LocalDispatcher dispatcher;

    @Authorize(groups = { "FULLADMIN" })
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/listCampaigns",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object listCampaigns(@RequestBody Map<String, Object> payload) throws GenericServiceException {
        Map<String, Object> result = QueryUtil.find(dispatcher, "ProductLookupView", payload);
        return UtilMisc.toMap("products", result.get("list"), "count", result.get("listSize"));
    }

    @Authorize
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/listPartyCampaigns",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object listPartyCampaigns(@RequestBody Map<String, Object> payload, @RequestAttribute Map<String, String> signedParty) throws GenericServiceException {
        Map<String, Object> result = QueryUtil.find(dispatcher, "ProductLookupView", payload);
        return UtilMisc.toMap("products", result.get("list"), "count", result.get("listSize"));
    }
}
