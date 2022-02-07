package SmsGateway.http

import SmsGateway.SmsTaskException

public interface IEndpoint {
    String baseUrl();
    String urlSuffix();
    String post(Map<String, Object> payload) throws SmsTaskException;
    String get(Map<String, Object> payload) throws SmsTaskException;
}
