package SmsGateway.http;

import SmsGateway.util.BasicConfig;

import java.util.Map;


public class BasicConfigBrilliant extends BasicConfig {
    public BasicConfigBrilliant(Map<String, Object> config) {
        super(config);
    }

    public String get(String key) {
        return get(key, String.class);
    }

    public final String getBaseUrl() {
        return get("BaseUrl");
    }

    public final String getUrlSuffix() {
        return get("UrlSuffix");
    }

    public final String getClientId() {
        return get("ClientId");
    }

    public final String getApiKey() {
        return get("ApiKey");
    }
}
