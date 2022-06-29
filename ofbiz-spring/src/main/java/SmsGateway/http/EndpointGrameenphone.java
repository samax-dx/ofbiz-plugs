package SmsGateway.http;

import SmsGateway.SmsTaskException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ofbiz.base.util.UtilMisc;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class EndpointGrameenphone extends Endpoint {
    private final BasicConfigGrameenphone config;
    private final long batchSize;

    public EndpointGrameenphone(Map<String, Object> config) {
        super("application/json");
        this.config = new BasicConfigGrameenphone(config);
        batchSize = 1000;
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

            String responseText = super.post(UtilMisc.toMap(
                    "username", config.getUserId(),
                    "password", config.getPassword(),
                    "apicode", "6",
                    "msisdn", tasks.stream().limit(this.batchSize).collect(Collectors.toList()),
                    "countrycode", "880",
                    "cli", config.getCli(),
                    "messagetype", "1",
                    "message", payload.get("Message"),
                    "messageid", "0"

            ));
//            String responseText = "{\"statusCode\":\"200\",\"message\":\"20220629-7187-310430017237-01309943338-02\"}";

            JsonNode reports = new ObjectMapper().readValue(responseText, JsonNode.class);

            if (!reports.get("statusCode").asText().equals("200")) {
                throw new Exception(reports.get("statusCode").asText());
            }

            List<Map<String, Object>> taskReports = new ArrayList<>();
            for (int i = 0, total = tasks.size(); i < total; ++i) {
                String task = tasks.get(i);
                JsonNode report = reports;
                Map<String, Object> taskReport = Stream.of(
                        new AbstractMap.SimpleEntry<>("MobileNumber", task),
                        new AbstractMap.SimpleEntry<>("MessageErrorCode", "0")
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                taskReports.add(taskReport);
            }

            Map<String, Object> report = new HashMap<>();
            report.put("ErrorCode", reports.get("statusCode"));
            report.put("Data", taskReports);
            return new ObjectMapper().convertValue(report, JsonNode.class).toString();
        } catch (JsonProcessingException e) {
            Map<String, Object> report = new HashMap<>();
            report.put("ErrorCode", -1);
            report.put("ErrorDescription", "acknowledging");
            throw new SmsTaskException(new ObjectMapper().convertValue(report, JsonNode.class).toString());
        } catch (Exception e) {
            Map<String, Object> report = new HashMap<>();
            report.put("ErrorCode", -1);
            report.put("ErrorDescription", e.getMessage());
            throw new SmsTaskException(new ObjectMapper().convertValue(report, JsonNode.class).toString());
        }

//        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ArrayOfServiceClass><ServiceClass><MessageId>1656172890299311</MessageId><Status>0</Status><StatusText>success</StatusText><ErrorCode>0</ErrorCode><ErrorText></ErrorText><SMSCount>1</SMSCount><CurrentCredit>1.38</CurrentCredit></ServiceClass></ArrayOfServiceClass>",
//        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ArrayOfServiceClass><ServiceClass><MessageId>1656172890299311</MessageId><Status>0</Status><StatusText>success</StatusText><ErrorCode>0</ErrorCode><ErrorText></ErrorText><SMSCount>1</SMSCount><CurrentCredit>1.38</CurrentCredit></ServiceClass><ServiceClass><MessageId>1656172890300584</MessageId><Status>0</Status><StatusText>success</StatusText><ErrorCode>0</ErrorCode><ErrorText></ErrorText><SMSCount>1</SMSCount><CurrentCredit>1.38</CurrentCredit></ServiceClass></ArrayOfServiceClass>",
//        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ArrayOfServiceClass><ServiceClass><MessageId>0</MessageId><Status>-1</Status><StatusText>Error occurred</StatusText><ErrorCode>1501</ErrorCode><ErrorText>balance_is_not_sufficient</ErrorText><SMSCount>1</SMSCount><CurrentCredit>0</CurrentCredit></ServiceClass></ArrayOfServiceClass>",
    }
}
