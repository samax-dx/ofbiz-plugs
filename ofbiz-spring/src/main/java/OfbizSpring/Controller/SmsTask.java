package OfbizSpring.Controller;

import OfbizSpring.Annotations.Authorize;
import OfbizSpring.Services.SmsProvider;
import OfbizSpring.Util.ServiceContextUtil;
import SmsGateway.ISmsProvider;
import TeleCampaign.CampaignQuery;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/SmsTask")
public class SmsTask {
    @Autowired
    private Delegator delegator;

    @Autowired
    private LocalDispatcher dispatcher;

    private Map<String, Object> loadCampaign(String campaignId) {
        try {
            GenericValue campaign = EntityQuery.use(delegator).from("Campaign")
                    .where("campaignId", campaignId)
                    .queryFirst();

            return UtilMisc.toMap(
                    "campaignId", campaign.get("campaignId"),
                    "campaignName", campaign.get("campaignName"),
                    "senderId", campaign.get("senderId"),
                    "message", campaign.get("message"),
                    "tasks", EntityQuery.use(delegator).from("CampaignTask")
                            .where("campaignId", campaignId, "status", "0")
                            .queryList()
            );
        } catch (GenericEntityException e) {
            return null;
        }
    }

    private Map<String, Object> loadCampaign(Map<String, Object> campaignData) {
        try {
            GenericValue campaign = CampaignQuery.use(delegator).storeCampaign(campaignData, true);

            return UtilMisc.toMap(
                    "campaignId", campaign.get("campaignId"),
                    "campaignName", campaign.get("campaignName"),
                    "senderId", campaign.get("senderId"),
                    "message", campaign.get("message"),
                    "tasks", EntityQuery.use(delegator).from("CampaignTask")
                            .where("campaignId", campaign.get("campaignId"), "status", "0")
                            .queryList()
            );
        } catch (Exception ex) {
            return null;
        }
//        return UtilMisc.toMap(
//                "campaignName", campaignData.get("campaignName"),
//                "senderId", campaignData.get("senderId"),
//                "message", campaignData.get("message"),
//                "tasks", Arrays.stream(((String) campaignData.get("phoneNumbers"))
//                        .replaceAll("[^,\\d]", "")
//                        .split(","))
//                        .map(phoneNumber -> UtilMisc.toMap("phoneNumber", phoneNumber, "status", "0", "statusText", "unknown task"))
//                        .collect(Collectors.toList())
//        );
    }

    @Authorize
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/sendSms",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object sendSms(@RequestBody Map<String, Object> payload, @RequestAttribute Map<String, String> signedParty) throws GenericServiceException {
//        Map<String, Object> smsSenderServiceArgs = Stream
//                .concat(smsSenderServiceConfig.entrySet().stream(), payload.entrySet().stream())
//                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
//        new GsonJsonParser().parseMap(request.getReader().lines().collect(Collectors.joining()))

//        UtilMisc.toMap("campaignName", "Test", "senderId", "Shabbir038", "message", "Hello", "contacts", "17175,17183");

        String partyId = signedParty.get("partyId");
        payload.put("partyId", partyId);

        Map<String, Object> campaign = payload.containsKey("campaignId") ? loadCampaign((String) payload.get("campaignId")) : loadCampaign(payload);
        Function<String, ISmsProvider> serviceProvider = SmsProvider::getService;

        return dispatcher.runSync("spRunCampaign", ServiceContextUtil.authorizeContext(UtilMisc.toMap(
                "partyId", partyId,
                "campaign", campaign,
                "campaignTasks", campaign == null ? new ArrayList<>() : campaign.remove("tasks"),
                "SmsProvider", serviceProvider
        )));
    }

    @Authorize(groups = { "FULLADMIN" })
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/getEndpoints",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object getEndpoints(HttpServletRequest request, HttpServletResponse response) throws GenericServiceException {
        return Arrays.asList(
                UtilMisc.toMap(
                        "id", "brilliant",
                        "description", "",
                        "version", "v1"
                ),
                UtilMisc.toMap(
                        "id", "teletalk",
                        "description", "",
                        "version", "v1"
                ),
                UtilMisc.toMap(
                        "id", "dummy",
                        "description", "",
                        "version", "v1"
                )
        );
    }
}
