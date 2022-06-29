package SmsGateway.http;

import SmsGateway.SmsTaskException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ofbiz.base.util.UtilMisc;

import java.util.*;
import java.util.stream.Collectors;


public class EndpointBanglalink extends Endpoint {
    private final BasicConfigBanglalink config;
    private final long batchSize;

    public EndpointBanglalink(Map<String, Object> config) {
        super("application/x-www-form-urlencoded");
        this.config = new BasicConfigBanglalink(config);
        batchSize = 1;
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
        Map<?, ?> response;

        try {
            List<String> tasks = (List<String>) payload.get("MobileNumbers");

            String responseText = /*super.post(UtilMisc.toMap(
                    "userID", config.getUserId(),
                    "passwd", config.getPassword(),
                    "sender", "8801969904256",
                    "msisdn", tasks.stream().limit(this.batchSize).collect(Collectors.toList()),
                    "message", payload.get("Message")
            ));*/"success: 1 and failure: 0";
            boolean isSuccess = Integer.parseInt(responseText.split("and")[0].split(":")[1].trim()) > 0;

            response = UtilMisc.toMap(
                    "error_code", isSuccess ? 0 : -1,
                    "smsInfo", tasks
            );
//            response = new XmlMapper().readValue(responseText, Map.class);
        } catch (Exception e/*JsonProcessingException e*/) {
            Map<String, Object> report = new HashMap<>();
            report.put("ErrorCode", -1);
            report.put("ErrorDescription", "acknowledging");
            throw new SmsTaskException(new ObjectMapper().convertValue(report, JsonNode.class).toString());
        }

        if (response.get("error_code").toString().equals("0")) {
            List<Object> tasks = ((List<String>) response.get("smsInfo")).stream()
                    .map(task -> UtilMisc.toMap("MobileNumber", task, "MessageErrorCode", "0"))
                    .collect(Collectors.toList());

            Map<String, Object> report = new HashMap<>();
            report.put("ErrorCode", response.get("error_code"));
            report.put("Data", tasks);
            return new ObjectMapper().convertValue(report, JsonNode.class).toString();
        } else {
            Map<String, Object> report = new HashMap<>();
            report.put("ErrorCode", response.get("error_code"));
            report.put("ErrorDescription", response.get("description"));
            throw new SmsTaskException(new ObjectMapper().convertValue(report, JsonNode.class).toString());
        }
    }
}
