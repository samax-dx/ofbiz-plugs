package SmsGateway.util;

import java.util.Map;

public class BasicConfig {
    Map<String, Object> source;

    public BasicConfig(Map<String, Object> source) {
        this.source = source;
    }

    public <T> T get(String key, Class<T> valueType) {
        try {
            return valueType.cast(source.get(key));
        } catch (Exception e) {
            return null;
        }
    }
}
