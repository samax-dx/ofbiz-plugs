package SmsGateway.smpp;

import SmsGateway.ISmsProvider;
import SmsGateway.SmsTaskException;

import java.util.Map;


public class SmsProviderSMPP implements ISmsProvider {
    @Override
    public String sendSms(Map<String, Object> smsTask) throws SmsTaskException {
        throw new UnsupportedOperationException();
    }
}
