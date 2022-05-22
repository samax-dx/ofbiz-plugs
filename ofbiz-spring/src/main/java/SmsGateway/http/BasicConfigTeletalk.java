package SmsGateway.http;

import SmsGateway.util.BasicConfig;

import java.util.Map;


public class BasicConfigTeletalk extends BasicConfig {
    public BasicConfigTeletalk(Map<String, Object> config) {
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

    public final String getAccountCode() {
        return get("AccountCode");
    }

    public final String getPassword() {
        return get("Password");
    }
}
