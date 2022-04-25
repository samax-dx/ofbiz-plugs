package SmsGateway.http;

import SmsGateway.SmsTaskException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ofbiz.base.util.UtilMisc;

import java.util.*;
import java.util.stream.Collectors;


public class EndpointTeletalk extends EndpointBase {
    private final BasicConfigBrilliant config;

    public EndpointTeletalk(Map<String, Object> config) {
        this.config = new BasicConfigBrilliant(config);
    }

    @Override
    public String baseUrl() {
        return config.getBaseUrl();
    }

    @Override
    public String urlSuffix() {
        return config.getUrlSuffix();
    }

    @Override
    public String post(Map<String, Object> payload) throws SmsTaskException {
        Map<String, Object> authPayload = UtilMisc.toMap(
                "username", config.getClientId().split("::")[0],
                "password", config.getApiKey(),
                "acode", config.getClientId().split("::")[1]
        );

        Map<String, Object> campaignPayload = UtilMisc.toMap(
                "message", payload.get("Message"),
                "masking", payload.get("SenderId"),
                "msisdn", payload.get("MobileNumbers").toString().split("\\s*,\\s*")
        );

        Map<?, ?> response;

        try {
            response = new ObjectMapper().readValue(super.post(UtilMisc.toMap(
                    "auth", authPayload,
                    "smsInfo", campaignPayload
            )), Map.class); // {"error_code":0,"contact":0,"creditDeducted":2,"currentBalance":"40","description":"Success","smsInfo":[{"smsID":"2022042415503762651d6d0d356","msisdn":"8801717590703"},{"smsID":"2022042415503762651d6d0d986","msisdn":"8801796019535"}]}
//            response = new ObjectMapper().readValue("{\"error_code\":0,\"contact\":0,\"creditDeducted\":2,\"currentBalance\":\"40\",\"description\":\"Success\",\"smsInfo\":[{\"smsID\":\"2022042415503762651d6d0d356\",\"msisdn\":\"8801717590703\"},{\"smsID\":\"2022042415503762651d6d0d986\",\"msisdn\":\"8801796019535\"}]}", Map.class);
        } catch (JsonProcessingException e) {
            Map<String, Object> report = new HashMap<>();
            report.put("ErrorCode", -1);
            report.put("ErrorDescription", "acknowledging");
            throw new SmsTaskException(new ObjectMapper().convertValue(report, String.class));
        }

        if (response.get("error_code").toString().equals("0")) {
            List<Object> tasks = ((List<?>) response.get("smsInfo"))
                    .stream()
                    .map(v -> {
                        Map<String, Object> task = UtilMisc.toMap(v);
                        return UtilMisc.toMap("MobileNumber", task.get("msisdn"), "MessageErrorCode", "0");
                    })
                    .collect(Collectors.toList());

            Map<String, Object> report = new HashMap<>();
            report.put("ErrorCode", response.get("error_code"));
            report.put("Data", tasks);
            return new ObjectMapper().convertValue(report, JsonNode.class).toString();
        } else {
            Map<String, Object> report = new HashMap<>();
            report.put("ErrorCode", response.get("error_code"));
            report.put("ErrorDescription", response.get("description"));
            throw new SmsTaskException(new ObjectMapper().convertValue(report, String.class));
        }
    }
}
