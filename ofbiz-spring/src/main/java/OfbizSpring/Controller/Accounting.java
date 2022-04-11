package OfbizSpring.Controller;

import OfbizSpring.Annotations.Authorize;
import OfbizSpring.Annotations.OfbizService;
import OfbizSpring.Util.QueryUtil;
import OfbizSpring.Util.ServiceContextUtil;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/Accounting")
public class Accounting {
    @Autowired
    private Delegator delegator;

    @Autowired
    private LocalDispatcher dispatcher;

    @Authorize(groups = { "FULLADMIN"})
    @OfbizService
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/addPartyBalance",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object addPartyBalance(@RequestBody Map<String, Object> payload) throws GenericServiceException {
        return dispatcher.runSync("spAddPartyBalance",  ServiceContextUtil.authorizeContext(payload));
    }

    @Authorize
    @OfbizService
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/addPartyBalanceRequest",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object addPartyBalanceRequest(@RequestBody Map<String, Object> payload, @RequestAttribute Map<String, String> signedParty) throws GenericServiceException {
        payload.put("partyId", signedParty.get("partyId"));
        return dispatcher.runSync("spAddPartyBalanceRequest",  ServiceContextUtil.authorizeContext(payload));
    }

    @Authorize(groups = { "FULLADMIN" })
    @OfbizService
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/addPartyBalanceConfirm",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object addPartyBalanceConfirm(@RequestBody Map<String, Object> payload) throws GenericServiceException {
        return dispatcher.runSync("spAddPartyBalanceConfirm",  ServiceContextUtil.authorizeContext(payload));
    }

    @Authorize(groups = { "FULLADMIN" })
    @OfbizService
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/addPartyBalanceCancel",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object addPartyBalanceCancel(@RequestBody Map<String, Object> payload) throws GenericServiceException {
        return dispatcher.runSync("spAddPartyBalanceCancel",  ServiceContextUtil.authorizeContext(payload));
    }

    @Authorize
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/listPartyBalanceRequests",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object listPartyBalanceRequests(@RequestBody Map<String, Object> payload, @RequestAttribute Map<String, String> signedParty) throws GenericServiceException, GenericEntityException {
        payload.put("party.partyId", signedParty.get("partyId"));

        payload.put("billingType", "payment");
        payload.put("billingType_op", "contains");

        Map<String, Object> result = QueryUtil.find(dispatcher, "PaymentLookupView", payload);
        return UtilMisc.toMap("payments", result.get("list"), "count", result.get("listSize"));
    }

    @Authorize(groups = { "FULLADMIN" })
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/listBalanceRequests",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object listBalanceRequests(@RequestBody Map<String, Object> payload) throws GenericServiceException, GenericEntityException {
        payload.put("billingType", "payment");
        payload.put("billingType_op", "contains");

        Map<String, Object> result = QueryUtil.find(dispatcher, "PaymentLookupView", payload);
        return UtilMisc.toMap("payments", result.get("list"), "count", result.get("listSize"));
    }
}
