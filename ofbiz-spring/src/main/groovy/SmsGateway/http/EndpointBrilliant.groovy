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
        payload.put("ClientId", config.getClientId());
        payload.put("ApiKey", config.getApiKey());

        String response = super.post(payload);

        if (response.indexOf("\"ErrorCode\":0") >= 0) {
            return response;
        } else {
            throw new SmsTaskException(response);
        }
    }
}
