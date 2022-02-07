package SmsGateway.http

public abstract class EndpointConfigBase implements IEndpointConfig {
    @Override
    public Object get(String key, String targetType) {
        try {
            return Class.forName(targetType).cast(get(key));
        } catch (Exception e) {
            return null;
        }
    }
}
