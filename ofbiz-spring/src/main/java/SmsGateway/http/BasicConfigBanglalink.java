package SmsGateway.http;

import SmsGateway.util.BasicConfig;

import java.util.Map;


public class BasicConfigBanglalink extends BasicConfig {
    public BasicConfigBanglalink(Map<String, Object> config) {
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

    public final String getUserId() {
        return get("UserId");
    }

    public final String getPassword() {
        return get("Password");
    }
}
