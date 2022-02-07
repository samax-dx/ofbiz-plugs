package SmsGateway.http

class EndpointBrilliantConfig extends EndpointConfigBase {
    private final Map<String, Object> config;

    public EndpointBrilliantConfig(Map<String, Object> config) {
        this.config = config;
    }

    public final String getBaseUrl() {
        return (String) get("BaseUrl", "java.lang.String");
    }

    public final String getUrlSuffix() {
        return (String) get("UrlSuffix", "java.lang.String");
    }

    public final String getClientId() {
        return (String) get("ClientId", "java.lang.String");
    }

    public final String getApiKey() {
        return (String) get("ApiKey", "java.lang.String");
    }

    @Override
    public Object get(String key) {
        return this.config.get(key);
    }
}
