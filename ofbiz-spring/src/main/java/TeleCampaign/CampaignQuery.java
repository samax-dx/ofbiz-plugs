package TeleCampaign;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityQuery;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CampaignQuery {
    private Delegator delegator;

    public static CampaignQuery use(Delegator delegator) {
        CampaignQuery value = new CampaignQuery();
        value.delegator = delegator;
        return value;
    }

    public GenericValue storeCampaign(Map<String, Object> campaignData) throws GenericEntityException {
        return storeCampaign(campaignData, false);
    }

    public GenericValue storeCampaign(Map<String, Object> campaignData, boolean isRandom) throws GenericEntityException {
        Map<Object, Object> campaignDataCopy = UtilMisc.toMap(campaignData);
        String mobileNumbers = (String) campaignDataCopy.remove("phoneNumbers");

        GenericValue campaign = delegator.makeValue("Campaign", campaignDataCopy);
        campaign.put("campaignId", delegator.getNextSeqId(campaign.getEntityName()));
        campaign.put("runCount", 0L);

        if (campaign.getOrDefault("campaignName", "").equals("")) {
            campaign.put("campaignName", String.valueOf(System.currentTimeMillis()));
        }

        if (isRandom) {
            campaign.remove("campaignName");
        }

        List<GenericValue> tasks = CampaignTaskProvider
                .create(
                        (String) campaign.get("campaignId"),
                        mobileNumbers,
                        EntityQuery.use(delegator).from("DialPlanActivePrioritizedView").queryList()
                )
                .inboundTasks
                .values()
                .stream()
                .map(campaignTask -> delegator.makeValue("CampaignTask", campaignTask.toMap()))
                .collect(Collectors.toList());

        boolean beganTransaction = TransactionUtil.begin();
        try {
            delegator.create(campaign);
            delegator.storeAll(tasks);

            if (beganTransaction) {
                TransactionUtil.commit();
            }
            return campaign;
        } catch (GenericEntityException ex) {
            if (beganTransaction) {
                TransactionUtil.commit();
            }
            throw ex;
        }
    }
}
