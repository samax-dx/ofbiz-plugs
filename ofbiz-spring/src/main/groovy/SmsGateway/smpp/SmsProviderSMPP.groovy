package SmsGateway.smpp;

import SmsGateway.ISmsProvider
import SmsGateway.SmsTaskException;

public class SmsProviderSMPP implements ISmsProvider {
    @Override
    String sendSms(Map<String, Object> smsTask) throws SmsTaskException {
        throw new UnsupportedOperationException();
    }
}
