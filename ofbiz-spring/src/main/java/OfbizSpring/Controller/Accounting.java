package OfbizSpring.Controller;

import OfbizSpring.Annotations.OfbizService;
import org.apache.ofbiz.entity.Delegator;
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

    @OfbizService
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/addPartyBalance",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Map<String, Object> addPartyBalance(@RequestBody Map<String, Object> payload) throws GenericServiceException {
        return dispatcher.runSync("spAddPartyBalance",  payload);
    }

    @OfbizService
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/addPartyBalanceRequest",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Map<String, Object> addPartyBalanceRequest(@RequestBody Map<String, Object> payload) throws GenericServiceException {
        return dispatcher.runSync("spAddPartyBalanceRequest",  payload);
    }

    @OfbizService
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/addPartyBalanceConfirm",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Map<String, Object> addPartyBalanceConfirm(@RequestBody Map<String, Object> payload) throws GenericServiceException {
        return dispatcher.runSync("spAddPartyBalanceConfirm",  payload);
    }

    @OfbizService
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/addPartyBalanceCancel",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Map<String, Object> addPartyBalanceCancel(@RequestBody Map<String, Object> payload) throws GenericServiceException {
        return dispatcher.runSync("spAddPartyBalanceCancel",  payload);
    }
}
