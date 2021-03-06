package TeleCampaign.Models;

import java.util.HashMap;
import java.util.Map;

public class CampaignTask {
    public String phoneNumber;
    public String campaignId;
    public String status;
    public String statusText;
    public String packageId;

    public static CampaignTask create(String phoneNumber, String campaignId, String status, String statusText, String packageId) {
        CampaignTask instance = new CampaignTask();
        instance.phoneNumber = phoneNumber;
        instance.campaignId = campaignId;
        instance.status = status;
        instance.statusText = statusText;
        instance.packageId = packageId;
        return instance;
    }

    public static <T extends Map<String, Object>> CampaignTask fromMap(T campaignTask) {
        return CampaignTask.create(
                (String) campaignTask.get("phoneNumber"),
                (String) campaignTask.get("campaignId"),
                (String) campaignTask.get("status"),
                (String) campaignTask.get("statusText"),
                (String) campaignTask.get("packageId")
        );
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("phoneNumber", phoneNumber);
        map.put("campaignId", campaignId);
        map.put("status", status);
        map.put("statusText", statusText);
        map.put("packageId", packageId);
        return map;
    }
}
