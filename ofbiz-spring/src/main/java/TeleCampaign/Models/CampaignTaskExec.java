package TeleCampaign.Models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CampaignTaskExec {
    public Map<String, CampaignTask> inboundTasks = new HashMap<>();
    public Map<String, CampaignTask> outboundTasks = new HashMap<>();
    public Map<String, List<String>> routeTasks = new HashMap<>();

    public static CampaignTaskExec create(Map<String, CampaignTask> inboundTasks, Map<String, CampaignTask> outboundTasks, Map<String, List<String>> routeTasks) {
        CampaignTaskExec routeTask = new CampaignTaskExec();
        routeTask.inboundTasks = inboundTasks;
        routeTask.outboundTasks = outboundTasks;
        routeTask.routeTasks = routeTasks;
        return routeTask;
    }
}
