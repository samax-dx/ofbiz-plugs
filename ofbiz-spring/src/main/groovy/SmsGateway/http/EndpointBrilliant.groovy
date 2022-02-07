package SmsGateway.http

import SmsGateway.SmsTaskException

public class EndpointBrilliant extends EndpointBase {
    private final EndpointBrilliantConfig config;

    public EndpointBrilliant(Map<String, Object> config) {
        this.config = new EndpointBrilliantConfig(config);
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
//        Map<String, Object> brilliantPayload = Stream
//                .concat(payloadTemplate.entrySet().stream(), payload.entrySet().stream())
//                .collect(Collectors.toMap(Map.Entry.getKey, Map.Entry.getValue));

        payload.put("ClientId", config.getClientId());
        payload.put("ApiKey", config.getApiKey());
        return super.post(payload);
    }
}
