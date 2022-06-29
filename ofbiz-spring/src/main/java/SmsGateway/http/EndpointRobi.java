package SmsGateway.http;

import SmsGateway.SmsTaskException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.ofbiz.base.util.UtilMisc;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class EndpointRobi extends Endpoint {
    private final BasicConfigRobi config;
    private final long batchSize;

    public EndpointRobi(Map<String, Object> config) {
        super("text/xml");
        this.config = new BasicConfigRobi(config);
        batchSize = 200;
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

            String responseText = super.get(UtilMisc.toMap(
                    "Username", config.getUserId(),
                    "Password", config.getPassword(),
                    "From", "8801847546653",
                    "To", tasks.stream().limit(this.batchSize).collect(Collectors.joining(",")),
                    "Message", payload.get("Message")
            ));
//            String responseText = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ArrayOfServiceClass><ServiceClass><MessageId>1656172890299311</MessageId><Status>0</Status><StatusText>success</StatusText><ErrorCode>0</ErrorCode><ErrorText></ErrorText><SMSCount>1</SMSCount><CurrentCredit>1.38</CurrentCredit></ServiceClass><ServiceClass><MessageId>1656172890300584</MessageId><Status>0</Status><StatusText>success</StatusText><ErrorCode>0</ErrorCode><ErrorText></ErrorText><SMSCount>1</SMSCount><CurrentCredit>1.38</CurrentCredit></ServiceClass></ArrayOfServiceClass>";

            List<JsonNode> reports = Stream.of(new XmlMapper().readTree(responseText).get("ServiceClass"))
                    .reduce(
                            new ArrayList<>(),
                            (acc, v) -> {
                                if (v.isArray()) {
                                    v.forEach(acc::add);
                                } else {
                                    acc.add(v);
                                }
                                return acc;
                            },
                            (acc_o, acc_n) -> {
                                acc_o.addAll(acc_n);
                                return acc_o;
                            }
                    );

            List<Map<String, Object>> taskReports = new ArrayList<>();
            for (int i = 0, total = tasks.size(); i < total; ++i) {
                String task = tasks.get(i);
                JsonNode report = reports.get(i);
                Map<String, Object> taskReport = Stream.of(
                        new AbstractMap.SimpleEntry<>("MobileNumber", task),
                        new AbstractMap.SimpleEntry<>("MessageErrorCode", report.get("Status"))
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                taskReports.add(taskReport);
            }

            Map<String, Object> report = new HashMap<>();
            report.put("ErrorCode", 0);
            report.put("Data", taskReports);
            return new ObjectMapper().convertValue(report, JsonNode.class).toString();
        } catch (JsonProcessingException e) {
            Map<String, Object> report = new HashMap<>();
            report.put("ErrorCode", -1);
            report.put("ErrorDescription", "acknowledging");
            throw new SmsTaskException(new ObjectMapper().convertValue(report, JsonNode.class).toString());
        }

//        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ArrayOfServiceClass><ServiceClass><MessageId>1656172890299311</MessageId><Status>0</Status><StatusText>success</StatusText><ErrorCode>0</ErrorCode><ErrorText></ErrorText><SMSCount>1</SMSCount><CurrentCredit>1.38</CurrentCredit></ServiceClass></ArrayOfServiceClass>",
//        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ArrayOfServiceClass><ServiceClass><MessageId>1656172890299311</MessageId><Status>0</Status><StatusText>success</StatusText><ErrorCode>0</ErrorCode><ErrorText></ErrorText><SMSCount>1</SMSCount><CurrentCredit>1.38</CurrentCredit></ServiceClass><ServiceClass><MessageId>1656172890300584</MessageId><Status>0</Status><StatusText>success</StatusText><ErrorCode>0</ErrorCode><ErrorText></ErrorText><SMSCount>1</SMSCount><CurrentCredit>1.38</CurrentCredit></ServiceClass></ArrayOfServiceClass>",
//        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ArrayOfServiceClass><ServiceClass><MessageId>0</MessageId><Status>-1</Status><StatusText>Error occurred</StatusText><ErrorCode>1501</ErrorCode><ErrorText>balance_is_not_sufficient</ErrorText><SMSCount>1</SMSCount><CurrentCredit>0</CurrentCredit></ServiceClass></ArrayOfServiceClass>",
    }
}
