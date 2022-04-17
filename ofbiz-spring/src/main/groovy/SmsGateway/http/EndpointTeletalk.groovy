package SmsGateway.http

import SmsGateway.SmsTaskException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.ofbiz.base.util.UtilMisc

import java.util.stream.Collectors

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
                "username", config.getClientId().split("::").first(),
                "password", config.getApiKey(),
                "acode", config.getClientId().split("::").last()
        );
        Map<String, Object> campaignPayload = UtilMisc.toMap(
                "message", payload.get("Message"),
                "masking", payload.get("SenderId"),
                "msisdn", payload.get("MobileNumbers").toString().split("\\s*,\\s*")
        )

        String response = super.post(UtilMisc.toMap(
                "auth", authPayload,
                "smsInfo", campaignPayload
        ));

        Map<String, Object> report = UtilMisc.toMap((Map<?, ?>) new ObjectMapper().readValue(response, Map.class));
        Map<String, Object> reportOut = UtilMisc.toMap("ErrorCode", report.get("error_code"));

        if (reportOut.get("ErrorCode").toString().equals("0")) {
            def tasks = Arrays
                    .asList(report.get("smsInfo"))
                    .stream()
                    .map({ v -> UtilMisc.toMap("MobileNumber", v["msisdn"][0])})
                    .collect(Collectors.toList());

            reportOut.put("Data", tasks);
            return new ObjectMapper().convertValue(reportOut, JsonNode.class).toString();
        } else {
            reportOut.put("ErrorDescription", report.get("description"));
            throw new SmsTaskException(new ObjectMapper().convertValue(reportOut, JsonNode.class).toString());
        }
    }
}
