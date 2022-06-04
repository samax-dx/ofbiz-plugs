package SmsGateway.http;

import SmsGateway.SmsTaskException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.ofbiz.base.util.UtilMisc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class EndpointBanglalink extends Endpoint {
    private final BasicConfigBanglalink config;

    public EndpointBanglalink(Map<String, Object> config) {
        super("application/x-www-form-urlencoded");
        this.config = new BasicConfigBanglalink(config);
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
            String responseText = super.post(UtilMisc.toMap(
                    "userID", config.getUserId(),
                    "passwd", config.getPassword(),
                    "sender", "8801969904256",
                    "msisdn", payload.get("MobileNumbers"),
                    "message", payload.get("Message")
            ));
            response = new XmlMapper().readValue(responseText, Map.class); // {"error_code":0,"contact":0,"creditDeducted":2,"currentBalance":"40","description":"Success","smsInfo":[{"smsID":"2022042415503762651d6d0d356","msisdn":"8801717590703"},{"smsID":"2022042415503762651d6d0d986","msisdn":"8801796019535"}]}
//            response = new ObjectMapper().readValue("{\"error_code\":0,\"contact\":0,\"creditDeducted\":2,\"currentBalance\":\"40\",\"description\":\"Success\",\"smsInfo\":[{\"smsID\":\"2022042415503762651d6d0d356\",\"msisdn\":\"8801717590703\"},{\"smsID\":\"2022042415503762651d6d0d986\",\"msisdn\":\"8801796019535\"}]}", Map.class);
        } catch (JsonProcessingException e) {
            Map<String, Object> report = new HashMap<>();
            report.put("ErrorCode", -1);
            report.put("ErrorDescription", "acknowledging");
            throw new SmsTaskException(new ObjectMapper().convertValue(report, JsonNode.class).toString());
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
            throw new SmsTaskException(new ObjectMapper().convertValue(report, JsonNode.class).toString());
        }
    }
}
