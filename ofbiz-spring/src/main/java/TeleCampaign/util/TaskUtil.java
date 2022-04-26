package TeleCampaign.util;

import TeleCampaign.Models.CampaignTask;
import TeleCampaign.Models.CampaignTaskExec;

import java.util.*;

public class TaskUtil {
    public static CampaignTaskExec createTaskExc(String campaignId, String taskDoc, List<Map<String, Object>> dialPlans) {
        Map<String, CampaignTask> inboundTasks = new HashMap<>();
        Map<String, CampaignTask> outboundTasks = new HashMap<>();
        Map<String, List<String>> routeTasks = new HashMap<>();

        dialPlans.forEach(plan -> routeTasks.put((String) plan.get("routeId"), new ArrayList<>()));

        Arrays.stream(taskDoc.replaceAll("[^,\\d]", "")
                        .split(","))
                .forEach(task -> {
                    if (inboundTasks.containsKey(task)) {
                        return;
                    }

                    inboundTasks.put(task, CampaignTask.create(task, campaignId, "0", "invalid_number"));

                    for (Map<String, Object> plan : dialPlans) {
                        String planId = (String) plan.get("dialPlanId");
                        String routeId = (String) plan.get("routeId");

                        String egressPrefix = (String) plan.getOrDefault("egressPrefix", "");
                        try {
                            egressPrefix = plan.get("egressPrefix").toString().trim();
                        } catch (Exception ignore) {
                            egressPrefix = "";
                        }

                        int cutDigit;
                        try {
                            cutDigit = Integer.parseInt(plan.get("cutDigit").toString());
                        } catch (Exception ignore) {
                            cutDigit = 0;
                        }

                        if (task.startsWith(planId)) {
                            String draftTask = egressPrefix.concat(task.substring(cutDigit));

                            if (outboundTasks.containsKey(draftTask)) {
                                inboundTasks.remove(task);
                                break;
                            }

                            inboundTasks.get(task).report = "package_not_found";
                            outboundTasks.put(draftTask, inboundTasks.get(task));
                            routeTasks.get(routeId).add(draftTask);
                            break;
                        }
                    }
                });

        return CampaignTaskExec.create(inboundTasks, outboundTasks, routeTasks);
    }
}
