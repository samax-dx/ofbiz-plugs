package SmsGateway.http

public interface IEndpointConfig {
    Object get(String key);
    Object get(String key, String targetType);
}
