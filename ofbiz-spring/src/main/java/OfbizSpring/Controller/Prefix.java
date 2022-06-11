package OfbizSpring.Controller;

import OfbizSpring.Annotations.Authorize;
import OfbizSpring.Util.QueryUtil;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/Prefix")
public class Prefix {
    @Autowired
    private Delegator delegator;

    @Autowired
    private LocalDispatcher dispatcher;

    @Authorize(groups = { "FULLADMIN" })
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/listPrefixes",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object listPrefixes(@RequestBody Map<String, Object> payload) throws GenericServiceException {
        Map<String, Object> result = QueryUtil.find(dispatcher, "Prefix", payload);
        return UtilMisc.toMap("prefixes", result.get("list"), "count", result.get("listSize"));
    }

    @Authorize(groups = { "FULLADMIN" })
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/savePrefix",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object savePrefix(@RequestBody Map<String, Object> payload) throws GenericEntityException {
        GenericValue value = delegator.makeValue("Prefix", payload);
        return UtilMisc.toMap("prefix", delegator.createOrStore(value));
    }

    @Authorize(groups = { "FULLADMIN" })
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/listRoutes",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object listRoutes(@RequestBody Map<String, Object> payload) throws GenericServiceException {
        Map<String, Object> result = QueryUtil.find(dispatcher, "Route", payload);
        return UtilMisc.toMap("routes", result.get("list"), "count", result.get("listSize"));
    }

    @Authorize(groups = { "FULLADMIN" })
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/saveRoute",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object saveRoute(@RequestBody Map<String, Object> payload) throws GenericEntityException {
        GenericValue value = delegator.makeValue("Route", payload);
        return UtilMisc.toMap("route", delegator.createOrStore(value));
    }

    @Authorize(groups = { "FULLADMIN" })
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/listDialPlans",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object listDialPlans(@RequestBody Map<String, Object> payload) throws GenericServiceException {
        Map<String, Object> result = QueryUtil.find(dispatcher, "DialPlan", payload);
        return UtilMisc.toMap("dialPlans", result.get("list"), "count", result.get("listSize"));
    }

    @Authorize(groups = { "FULLADMIN" })
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/saveDialPlan",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object saveDialPlan(@RequestBody Map<String, Object> payload) throws GenericEntityException {
        long priority;
        long digitCut;

        try {
            priority = Long.parseLong(payload.getOrDefault("priority", "1").toString());
        } catch (Exception ignore) {
            priority = 1L;
        }

        try {
            digitCut = Long.parseLong(payload.getOrDefault("digitCut", "0").toString());
        } catch (Exception ignore) {
            digitCut = 0L;
        }

        payload.put("priority", priority);
        payload.put("digitCut", digitCut);

        GenericValue value = delegator.makeValue("DialPlan", payload);
        return UtilMisc.toMap("dialPlan", delegator.createOrStore(value));
    }

    @Authorize(groups = { "FULLADMIN" })
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/listPackages",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object listPackages(@RequestBody Map<String, Object> payload) throws GenericServiceException {
        Map<String, Object> result = QueryUtil.find(dispatcher, "PackagePrefix", payload);
        return UtilMisc.toMap("packages", result.get("list"), "count", result.get("listSize"));
    }

    @Authorize(groups = { "FULLADMIN" })
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/savePackage",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object savePackage(@RequestBody Map<String, Object> payload) throws GenericEntityException {
        GenericValue value = delegator.makeValue("PackagePrefix", payload);
        return UtilMisc.toMap("package", delegator.createOrStore(value));
    }
}
