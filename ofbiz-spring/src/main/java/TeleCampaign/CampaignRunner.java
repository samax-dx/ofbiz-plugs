package TeleCampaign;

import OfbizSpring.Services.SmsProvider;
import OfbizSpring.Util.ServiceContextUtil;
import SmsGateway.ISmsProvider;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.LocalDispatcher;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CampaignRunner {
    private static class RecordCache {
        private static final ConcurrentHashMap<String, Boolean> cache = new ConcurrentHashMap<>();

        private static String recordId(GenericValue campaign) {
            return campaign.get("campaignId").toString();
        }

        public void put(GenericValue campaign) {
            cache.put(recordId(campaign), true);
        }

        public void remove(GenericValue campaign) {
            cache.remove(recordId(campaign));
        }

        public boolean contains(GenericValue campaign) {
            return cache.containsKey(recordId(campaign));
        }
    }

    private final Delegator delegator;
    private final LocalDispatcher dispatcher;

    public CampaignRunner(Delegator delegator, LocalDispatcher dispatcher) {
        this.delegator = delegator;
        this.dispatcher = dispatcher;
    }

    public Map<String, GenericValue> runCampaigns() {
        return CampaignQuery.use(delegator)
                .getIncompleteCampaigns()
                .parallelStream()
                .collect(Collectors.toMap(campaign -> {
                    try {
                        Map<String, Object> svcPayload = new HashMap<>();
                        svcPayload.put("campaign", campaign);
                        svcPayload.put("campaignTasks", CampaignQuery.use(delegator).getIncompleteTasks(campaign));
                        svcPayload.put("partyId", campaign.get("partyId").toString());
                        svcPayload.put("SmsProvider", (Function<String, ISmsProvider>) SmsProvider::getService);

                        dispatcher.runSync("spRunCampaign", ServiceContextUtil.authorizeContext(svcPayload));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return campaign.getString("campaignId");
                }, campaign -> campaign));
    }
}
