package TeleCampaign;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityFindOptions;
import org.apache.ofbiz.entity.util.EntityQuery;

import java.util.Arrays;
import java.util.Collections;
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

    public List<GenericValue> getIncompleteCampaigns() {
        try {
            return delegator.findList(
                    "CampaignReport",
//                    EntityCondition.makeCondition(
//                            EntityCondition.makeCondition("firstRun", EntityOperator.NOT_EQUAL,null),
//                            EntityOperator.AND,
//                            EntityCondition.makeCondition("pending", EntityOperator.GREATER_THAN,0L)
//                    ),
                    EntityCondition.makeCondition(
                            EntityCondition.makeCondition("pending", EntityOperator.GREATER_THAN,0L),
                            EntityCondition.makeCondition("inActiveHours", "1")
                    ),
                    null,
                    Arrays.asList("pending"),
                    new EntityFindOptions(),
                    true
            );
        } catch (GenericEntityException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<GenericValue> getIncompleteTasks(GenericValue campaign) {
        try {
            return delegator.findList(
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
            );
        } catch (GenericEntityException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
