package SmsGateway;

public interface ISmsProvider {
    String sendSms(Map<String, Object> smsTask) throws SmsTaskException;
}
