package SmsGateway.http;

import SmsGateway.SmsTaskException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public abstract class Endpoint implements IEndpoint {
    private final String messageFormat;

    public Endpoint(String messageFormat) {
        this.messageFormat = messageFormat;
    }

    private static String requestPayload(Map<String, Object> payload, String messageFormat) throws JsonProcessingException {
        if (messageFormat.equals("application/json")) {
            return new ObjectMapper().writeValueAsString(payload);
        }

        List<String> payloadItems = payload
                .entrySet()
                .stream()
                .reduce(
                        new ArrayList<>(),
                        (acc, v) -> {
                            String key = v.getKey();
                            Object value = v.getValue();

                            try {
                                if (key.length() > 0 && value != null) {
                                    acc.add(key + "=" + URLEncoder.encode(value.toString(), StandardCharsets.UTF_8.name()));
                                }
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }

                            return acc;
                        },
                        (acc_o, acc_n) -> {
                            acc_n.addAll(0, acc_o);
                            return acc_n;
                        }
                );

        return String.join("&", payloadItems);
    }

    @Override
    public String post(Map<String, Object> payload) throws SmsTaskException {
        try {
            ResponseEntity<String> response =  new RestTemplate().exchange(
                    baseUrl() + urlSuffix(),
                    HttpMethod.POST,
                    new HttpEntity<>(
                            requestPayload(payload, messageFormat),
                            new LinkedMultiValueMap<>(Stream.of(
                                    new AbstractMap.SimpleEntry<>("Content-Type", Arrays.asList(messageFormat)),
                                    new AbstractMap.SimpleEntry<>("Accept", Arrays.asList(messageFormat))
                            ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                    ),
                    String.class
            );

            int status = response.getStatusCode().value();
            if (status >= 400 && status <= 500) {
                throw new SmsTaskException(String.format("Error: %s; %s;", status, response.getBody()));
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
        try {
            ResponseEntity<String> response =  new RestTemplate().exchange(
                    UriComponentsBuilder.fromHttpUrl(baseUrl() + urlSuffix())
                            .queryParams(new LinkedMultiValueMap<>(
                                    payload.entrySet().stream().collect(
                                            Collectors.toMap(Map.Entry::getKey, en -> Arrays.asList(en.getValue().toString()))
                                    )
                            ))
                            .build().encode().toUri(),
                    HttpMethod.GET,
                    new HttpEntity<>(
                            new LinkedMultiValueMap<>(Stream.of(
                                    new AbstractMap.SimpleEntry<>("Content-Type", Arrays.asList(messageFormat)),
                                    new AbstractMap.SimpleEntry<>("Accept", Arrays.asList(messageFormat))
                            ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                    ),
                    String.class
            );

            int status = response.getStatusCode().value();
            if (status >= 400 && status <= 500) {
                throw new SmsTaskException(String.format("Error: %s; %s;", status, response.getBody()));
            }

            return response.getBody();
        } catch (RestClientException e) {
            throw new SmsTaskException(e.getMessage());
        }
    }
}
