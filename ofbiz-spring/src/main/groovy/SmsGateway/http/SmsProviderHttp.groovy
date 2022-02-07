package SmsGateway.http

import SmsGateway.ISmsProvider
import SmsGateway.SmsTaskException

public class SmsProviderHttp implements ISmsProvider {
    private final IEndpoint endpoint;

    public SmsProviderHttp(IEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    public String sendSms(Map<String, Object> smsTask) throws SmsTaskException {
        return endpoint.post(smsTask);
    }
}
