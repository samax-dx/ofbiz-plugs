package SmsGateway.http

import SmsGateway.SmsTaskException
import org.apache.ofbiz.base.util.HttpClient
import org.apache.ofbiz.base.util.HttpClientException;
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.core.JsonProcessingException

public abstract class EndpointBase implements IEndpoint {
    @Override
    public String post(Map<String, Object> payload) throws SmsTaskException {
        HttpClient client = new HttpClient();
        client.setHeader("Content-Type", "application/json");
        client.setUrl("${baseUrl()}${urlSuffix()}");
        client.setParameters(payload);

        try {
            String responseData = client.post(new ObjectMapper().writeValueAsString(payload));
            int responseCode = client.getResponseCode();
            if (responseCode >= 400 || responseCode <= 500) {
                client.postStream().close();
                throw new SmsTaskException("Error: ${responseCode}; ${client.getResponseContent()};")
            }
            return responseData;
        } catch (JsonProcessingException e) {
            throw new SmsTaskException("bad request arguments");
        } catch (HttpClientException e) {
            throw new SmsTaskException(e.getMessage());
        }
    }

    @Override
    public String get(Map<String, Object> payload) throws SmsTaskException {
        throw new UnsupportedOperationException();
    }
}
