package SmsGateway;

import java.util.Map;


public interface ISmsProvider {
    String sendSms(Map<String, Object> smsTask) throws SmsTaskException;
}
