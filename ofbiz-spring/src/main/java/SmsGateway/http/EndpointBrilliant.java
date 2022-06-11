package SmsGateway.http;

import SmsGateway.SmsTaskException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class EndpointBrilliant extends Endpoint {
    private final BasicConfigBrilliant config;
    private final long batchSize;

    public EndpointBrilliant(Map<String, Object> config) {
        super("application/json");
        this.config = new BasicConfigBrilliant(config);
        batchSize = 10000;
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
        payload.put("ClientId", config.getClientId());
        payload.put("ApiKey", config.getApiKey());

        String senderId = "8809638010035";
        List<String> msisdn = ((List<String>) payload.get("MobileNumbers")).stream().limit(this.batchSize).collect(Collectors.toList());

        payload.put("SenderId", senderId);
        payload.put("MobileNumbers", String.join(",",  msisdn));
        String response = super.post(payload); // {"ErrorCode":0,"ErrorDescription":null,"Data":[{"MessageErrorCode":1081,"MessageErrorDescription":"Country not found in master data","MobileNumber":"01717590703","MessageId":"","Custom":""},{"MessageErrorCode":0,"MessageErrorDescription":"Success","MobileNumber":"8801796019535","MessageId":"9a32a8b0-7f73-4625-b98b-4981f3d1347e","Custom":null},{"MessageErrorCode":1083,"MessageErrorDescription":"Price not found","MobileNumber":"11717590703","MessageId":"","Custom":""}]}
//        String response = "{\"ErrorCode\":0,\"ErrorDescription\":null,\"Data\":[{\"MessageErrorCode\":1081,\"MessageErrorDescription\":\"Country not found in master data\",\"MobileNumber\":\"01717590703\",\"MessageId\":\"\",\"Custom\":\"\"},{\"MessageErrorCode\":0,\"MessageErrorDescription\":\"Success\",\"MobileNumber\":\"8801796019535\",\"MessageId\":\"9a32a8b0-7f73-4625-b98b-4981f3d1347e\",\"Custom\":null},{\"MessageErrorCode\":1083,\"MessageErrorDescription\":\"Price not found\",\"MobileNumber\":\"11717590703\",\"MessageId\":\"\",\"Custom\":\"\"}]}";

        if (response.contains("\"ErrorCode\":0")) {
            return response;
        } else {
            throw new SmsTaskException(response);
        }
    }
}
