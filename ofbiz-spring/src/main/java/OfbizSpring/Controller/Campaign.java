package OfbizSpring.Controller;

import OfbizSpring.Annotations.Authorize;
import OfbizSpring.Util.QueryUtil;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
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
        Map<String, Object> campaignData = UtilMisc.toMap(payload);
        String mobileNumbers = (String) campaignData.remove("phoneNumbers");

        GenericValue campaign = delegator.makeValue("Campaign", campaignData);
        campaign.put("campaignId", delegator.getNextSeqId(campaign.getEntityName()));
        campaign.put("runCount", 0L);

        List<GenericValue> tasks = Arrays
                .stream(mobileNumbers.split("\\s*,\\s*"))
                .map(e -> {
                    GenericValue campaignTask = delegator.makeValue("CampaignTask");
//                    campaignTask.put("campaignTaskId", delegator.getNextSeqId(campaignTask.getEntityName()));
                    campaignTask.put("phoneNumber", e);
                    campaignTask.put("campaignId", campaign.get("campaignId"));
                    campaignTask.put("status", "0");
                    campaignTask.put("report", "pending");
                    return campaignTask;
                })
                .collect(Collectors.toList());

        boolean beganTransaction = TransactionUtil.begin();
        try {
            delegator.create(campaign);
            delegator.storeAll(tasks);

            if (beganTransaction) {
                TransactionUtil.commit();
            }
            return UtilMisc.toMap("successMessage", "success", "campaignId", campaign.get("campaignId"));
        } catch (Exception ex) {

            if (beganTransaction) {
                TransactionUtil.commit();
            }
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

        return createCampaign(payload);
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
        Map<String, Object> result = QueryUtil.find(dispatcher, "CampaignTaskReport", payload);
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

        Map<String, Object> result = QueryUtil.find(dispatcher, "CampaignTaskReport", payload);
        return UtilMisc.toMap("campaigns", result.get("list"), "count", result.get("listSize"));
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

        Map<String, Object> campaign = QueryUtil.findOne(dispatcher, "CampaignTaskReport", UtilMisc.toMap(
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

        Map<String, Object> campaign = QueryUtil.findOne(dispatcher, "CampaignTaskReport", UtilMisc.toMap(
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
