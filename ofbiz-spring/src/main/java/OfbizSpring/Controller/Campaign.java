package OfbizSpring.Controller;

import OfbizSpring.Annotations.Authorize;
import OfbizSpring.Util.QueryUtil;
import TeleCampaign.CampaignQuery;
import TeleCampaign.CampaignTaskProvider;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

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
            value = "/createCampaign",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object createCampaign(@RequestBody Map<String, Object> payload) throws GenericServiceException, GenericEntityException {
        try {
            GenericValue campaign = CampaignQuery.use(delegator).storeCampaign(payload);
            return UtilMisc.toMap("successMessage", "success", "campaignId", campaign.get("campaignId"));
        } catch (Exception ex) {
            return UtilMisc.toMap("errorMessage", ex.getMessage());
        }
    }

    @Authorize
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/saveCampaign",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object saveCampaign(@RequestBody Map<String, Object> payload, @RequestAttribute Map<String, String> signedParty) throws GenericServiceException, GenericEntityException {
        payload.put("partyId", signedParty.get("partyId"));

        try {
            GenericValue campaign = CampaignQuery.use(delegator).storeCampaign(payload);
            return UtilMisc.toMap("successMessage", "success", "campaignId", campaign.get("campaignId"));
        } catch (Exception ex) {
            return UtilMisc.toMap("errorMessage", ex.getMessage());
        }
    }

    @Authorize
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/deleteCampaignTask",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object deleteCampaignTask(@RequestBody Map<String, Object> payload, @RequestAttribute Map<String, String> signedParty) throws GenericServiceException, GenericEntityException {
        GenericValue task = delegator.findOne("CampaignTask", UtilMisc.toMap("phoneNumber", payload.get("phoneNumber"), "campaignId", payload.get("campaignId")), false);
        if (Objects.equals(task.getString("status"), "1")) {
            return ServiceUtil.returnError("Can not delete completed task");
        }
        return UtilMisc.toMap("deleted", delegator.removeValue(task));
    }

    @Authorize(groups = { "FULLADMIN" })
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/listCampaigns",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object listCampaigns(@RequestBody Map<String, Object> payload) throws GenericServiceException {
        Map<String, Object> result = QueryUtil.find(dispatcher, "CampaignReport", payload);
        return UtilMisc.toMap("campaigns", result.get("list"), "count", result.get("listSize"));
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
        payload.put("partyId", signedParty.get("partyId"));

        Map<String, Object> result = QueryUtil.find(dispatcher, "CampaignReport", payload);
        return UtilMisc.toMap("campaigns", result.get("list"), "count", result.get("listSize"));
    }

    @Authorize
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/getPartyCampaignTaskReports",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object getPartyCampaignTaskReports(@RequestBody Map<String, Object> payload, @RequestAttribute Map<String, String> signedParty) throws GenericServiceException {
        payload.put("partyId", signedParty.get("partyId"));

        Map<String, Object> result = QueryUtil.find(dispatcher, "CampaignTaskReport", payload);
        return UtilMisc.toMap("taskReports", result.get("list"), "count", result.get("listSize"));
    }

    @Authorize
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/getCampaignPendingTasks",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object getCampaignPendingTasks(@RequestBody Map<String, Object> payload) throws GenericServiceException {
        String campaignId = (String) payload.getOrDefault("campaignId", "");

        if (campaignId.equals("")) {
            return UtilMisc.toMap("campaign", null, "tasks", null);
        }

        Map<String, Object> campaign = QueryUtil.findOne(dispatcher, "CampaignReport", UtilMisc.toMap(
                "campaignId", campaignId
        ));

        Map<String, Object> tasks = QueryUtil.find(dispatcher, "CampaignTask", UtilMisc.toMap(
                "campaignId", campaignId,
                "status", "0",
                "page", payload.getOrDefault("page", 1),
                "limit", payload.getOrDefault("limit", 10000)
        ));

        return UtilMisc.toMap(
                "campaign", campaign.get("item"),
                "tasks", tasks.get("list"),
                "count", tasks.get("listSize")
        );
    }

    @Authorize
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/getCampaignTasks",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object getCampaignTasks(@RequestBody Map<String, Object> payload) throws GenericServiceException {
        String campaignId = (String) payload.getOrDefault("campaignId", "");

        if (campaignId.equals("")) {
            return UtilMisc.toMap("campaign", null, "tasks", null);
        }

        Map<String, Object> campaign = QueryUtil.findOne(dispatcher, "CampaignReport", UtilMisc.toMap(
                "campaignId", campaignId
        ));

        Map<String, Object> tasks = QueryUtil.find(dispatcher, "CampaignTask", UtilMisc.toMap(
                "campaignId", campaignId,
                "page", payload.getOrDefault("page", 1),
                "limit", payload.getOrDefault("limit", 10000)
        ));

        return UtilMisc.toMap(
                "campaign", campaign.get("item"),
                "tasks", tasks.get("list"),
                "count", tasks.get("listSize")
        );
    }
}
