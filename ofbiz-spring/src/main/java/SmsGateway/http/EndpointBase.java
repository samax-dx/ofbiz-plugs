package SmsGateway.http;

import SmsGateway.SmsTaskException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.Map;


public abstract class EndpointBase implements IEndpoint {
    @Override
    public String post(Map<String, Object> payload) throws SmsTaskException {
        try {
            ResponseEntity<String> response =  new RestTemplate().exchange(
                    baseUrl() + urlSuffix(),
                    HttpMethod.POST,
                    new HttpEntity<String>(
                            new ObjectMapper().writeValueAsString(payload),
                            new HttpHeaders() {{
                                add("Content-Type", "application/json");
                            }}
                    ),
                    String.class
            );

            int status = response.getStatusCode().value();
            if (status >= 400 && status <= 500) {
                throw new SmsTaskException("Error: ${status}; ${response.getBody()};");
            }

            return response.getBody();
        } catch (JsonProcessingException e) {
            throw new SmsTaskException("bad request arguments");
        } catch (RestClientException e) {
            throw new SmsTaskException(e.getMessage());
        }
    }

    @Override
    public String get(Map<String, Object> payload) throws SmsTaskException {
        throw new UnsupportedOperationException();
    }
}
