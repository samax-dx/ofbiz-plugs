package TeleCampaign;

import OfbizSpring.Services.SmsProvider;
import OfbizSpring.Util.ServiceContextUtil;
import SmsGateway.ISmsProvider;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityFindOptions;
import org.apache.ofbiz.service.LocalDispatcher;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class CampaignTaskScheduler {
    private static class ScheduleRecordCache {
        private static final ConcurrentHashMap<String, Boolean> cache = new ConcurrentHashMap<>();

        private String getCampaignId(GenericValue campaign) {
            return campaign.get("campaignId").toString();
        }

        public void put(GenericValue campaign) {
            cache.put(getCampaignId(campaign), true);
        }

        public void remove(GenericValue campaign) {
            cache.remove(getCampaignId(campaign));
        }

        public boolean contains(GenericValue campaign) {
            return cache.containsKey(getCampaignId(campaign));
        }
    }

    private final Delegator delegator;
    private final LocalDispatcher dispatcher;
    private final ScheduleRecordCache runningCampaigns;

    public CampaignTaskScheduler(Delegator delegator, LocalDispatcher dispatcher) {
        this.delegator = delegator;
        this.dispatcher = dispatcher;
        this.runningCampaigns = new ScheduleRecordCache();
    }

    private static void runCampaign(GenericValue campaign, Delegator delegator, LocalDispatcher dispatcher) {
        try {
            Map<String, Object> svcPayload = new HashMap<>();
            svcPayload.put("campaign", campaign);
            svcPayload.put("campaignTasks", delegator.findList(
                    "CampaignTask",
                    EntityCondition.makeCondition(
                            EntityCondition.makeCondition("campaignId", campaign.get("campaignId").toString()),
                            EntityOperator.AND,
                            EntityCondition.makeCondition("status", "0")
                    ),
                    null,
                    Collections.singletonList("campaignId"),
                    new EntityFindOptions(),
                    true
            ));
            svcPayload.put("partyId", campaign.get("partyId").toString());
            svcPayload.put("SmsProvider", (Function<String, ISmsProvider>) SmsProvider::getService);

            dispatcher.runSync("spRunCampaign", ServiceContextUtil.authorizeContext(svcPayload));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void scheduleIncompleteTasks() {
        CampaignQuery.use(delegator)
                .getIncompleteCampaigns()
                .stream()
                .parallel()
                .forEach(campaign -> {
                    if (runningCampaigns.contains(campaign)) {
                        return;
                    }

                    runningCampaigns.put(campaign);
                    runCampaign(campaign, delegator, dispatcher);
                    runningCampaigns.remove(campaign);
                });
    }
}
