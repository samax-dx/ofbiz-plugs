package SmsGateway.http;

import SmsGateway.SmsTaskException;

import java.util.Map;


public class EndpointBrilliant extends Endpoint {
    private final BasicConfigBrilliant config;

    public EndpointBrilliant(Map<String, Object> config) {
        super("application/json");
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
        payload.put("ClientId", config.getClientId());
        payload.put("ApiKey", config.getApiKey());

        payload.put("SenderId", "8809638010035");
        String response = super.post(payload); // {"ErrorCode":0,"ErrorDescription":null,"Data":[{"MessageErrorCode":1081,"MessageErrorDescription":"Country not found in master data","MobileNumber":"01717590703","MessageId":"","Custom":""},{"MessageErrorCode":0,"MessageErrorDescription":"Success","MobileNumber":"8801796019535","MessageId":"9a32a8b0-7f73-4625-b98b-4981f3d1347e","Custom":null},{"MessageErrorCode":1083,"MessageErrorDescription":"Price not found","MobileNumber":"11717590703","MessageId":"","Custom":""}]}
//        String response = "{\"ErrorCode\":0,\"ErrorDescription\":null,\"Data\":[{\"MessageErrorCode\":1081,\"MessageErrorDescription\":\"Country not found in master data\",\"MobileNumber\":\"01717590703\",\"MessageId\":\"\",\"Custom\":\"\"},{\"MessageErrorCode\":0,\"MessageErrorDescription\":\"Success\",\"MobileNumber\":\"8801796019535\",\"MessageId\":\"9a32a8b0-7f73-4625-b98b-4981f3d1347e\",\"Custom\":null},{\"MessageErrorCode\":1083,\"MessageErrorDescription\":\"Price not found\",\"MobileNumber\":\"11717590703\",\"MessageId\":\"\",\"Custom\":\"\"}]}";

        if (response.contains("\"ErrorCode\":0")) {
            return response;
        } else {
            throw new SmsTaskException(response);
        }
    }
}
