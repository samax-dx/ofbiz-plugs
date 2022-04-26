package TeleCampaign.Models;

public class CampaignTask {
    public String phoneNumber;
    public String campaignId;
    public String status;
    public String report;

    public static CampaignTask create(String phoneNumber, String campaignId, String status, String report) {
        CampaignTask campaignTask = new CampaignTask();
        campaignTask.phoneNumber = phoneNumber;
        campaignTask.campaignId = campaignId;
        campaignTask.status = status;
        campaignTask.report = report;
        return campaignTask;
    }
}
