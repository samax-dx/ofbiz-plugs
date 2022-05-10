package TeleCampaign;

import TeleCampaign.Models.CampaignTask;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class CampaignTaskProvider {
    public Map<String, CampaignTask> inboundTasks;
    public Map<String, CampaignTask> outboundTasks;
    public Map<String, List<String>> routeTasks;

    private static CampaignTaskProvider create(
            Map<String, CampaignTask> inboundTasks,
            Map<String, CampaignTask> outboundTasks,
            Map<String, List<String>> routeTasks
    ) {
        CampaignTaskProvider instance = new CampaignTaskProvider();
        instance.inboundTasks = inboundTasks;
        instance.outboundTasks = outboundTasks;
        instance.routeTasks = routeTasks;
        return instance;
    }

    public static CampaignTaskProvider create(
            String campaignId,
            String tasks,
            List<? extends Map<String, Object>> dialPlans
    ) {
        return create(campaignId, tasks, dialPlans, dialPlans);
    }

    public static CampaignTaskProvider create(
            String campaignId,
            String tasks,
            List<? extends Map<String, Object>> dialPlans,
            List<? extends Map<String, Object>> packagePlans
    ) {
        List<Map<String, Object>> taskList = Arrays.stream(tasks.replaceAll("[^,\\d]", "").split(","))
                .map(task -> CampaignTask.create(task, campaignId, "0", "invalid_number").toMap())
                .collect(Collectors.toList());

        return create(taskList, dialPlans, packagePlans == null ? dialPlans : packagePlans);
    }

    private static <T extends Map<String, Object>> Object prefixPlanAttr(T plan, String attrName, Object defaultValue) {
        try {
            return plan.getOrDefault(attrName, defaultValue);
        } catch (Exception ignore) {
            return "";
        }
    }

    private static <T extends Map<String, Object>> String planPackageId(T plan) {
        return String.valueOf(prefixPlanAttr(plan, "packageId", ""));
    }

    private static <T extends Map<String, Object>> String planRouteId(T plan) {
        return String.valueOf(prefixPlanAttr(plan, "routeId", ""));
    }

    private static <T extends Map<String, Object>> String planPrefixId(T plan) {
        return String.valueOf(prefixPlanAttr(plan, "dialPlanId", ""));
    }

    private static <T extends Map<String, Object>> String planEgressPrefix(T plan) {
        return String.valueOf(prefixPlanAttr(plan, "egressPrefix", ""));
    }

    private static <T extends Map<String, Object>> int planCutDigit(T plan) {
        try {
            return Integer.parseInt(prefixPlanAttr(plan, "digitCut", 0).toString());
        } catch (Exception ignore) {
            return 0;
        }
    }

    public static CampaignTaskProvider create(
            List<? extends Map<String, Object>> tasks,
            List<? extends Map<String, Object>> dialPlans,
            List<? extends Map<String, Object>> packagePlans
    ) {
        Map<String, CampaignTask> inboundTasks = new HashMap<>();
        Map<String, CampaignTask> outboundTasks = new HashMap<>();
        Map<String, List<String>> routeTasks = new HashMap<>();/*packagePlans.stream()
                .map(plan -> new AbstractMap.SimpleEntry<>(planPackageId(plan) + ":" + planRouteId(plan), new ArrayList<String>()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (r1, r2) -> r1));*/

        Map<String, ? extends Map<String, Object>> packagePlanValues = packagePlans.stream()
                .map(plan -> new AbstractMap.SimpleEntry<>(planPrefixId(plan), plan))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (r1, r2) -> r1));

        tasks.forEach(taskGeneric -> {
            CampaignTask task = CampaignTask.fromMap(taskGeneric);

            if (inboundTasks.containsKey(task.phoneNumber)) {
                return;
            }

            inboundTasks.put(task.phoneNumber, task);

            for (Map<String, Object> plan : dialPlans) {
                String planId = planPrefixId(plan);
                if (!task.phoneNumber.startsWith(planId)) {
                    continue;
                }

                String taskId_ob = planEgressPrefix(plan).concat(task.phoneNumber.substring(planCutDigit(plan)));
                if (outboundTasks.containsKey(taskId_ob)) {
                    inboundTasks.remove(task.phoneNumber);
                    break;
                }

                inboundTasks.get(task.phoneNumber).statusText = "task_enqueued";

                Map<String, Object> pkgPlan = Optional.<Map<String, Object>>ofNullable(packagePlanValues.get(planId))
                        .orElse(plan);

                routeTasks.merge(
                        planPackageId(pkgPlan) + ":" + planRouteId(pkgPlan),
                        Collections.singletonList(taskId_ob),
                        (l, r) -> Stream.concat(l.stream(), r.stream()).collect(Collectors.toList())
                );

//                if (packagePlanValues.containsKey(planId)) {
//                    inboundTasks.get(task.phoneNumber).statusText = "task_enqueued";
//
//                    Map<String, Object> pkgPlan = packagePlanValues.get(planId);
//                    routeTasks.get(planPackageId(pkgPlan) + ":" + planRouteId(pkgPlan)).add(taskId_ob);
//                } else {
//                    inboundTasks.get(task.phoneNumber).statusText = "package_not_found";
//                }

                outboundTasks.put(taskId_ob, inboundTasks.get(task.phoneNumber));
                break;
            }
        });

        return CampaignTaskProvider.create(inboundTasks, outboundTasks, routeTasks);
    }
}
