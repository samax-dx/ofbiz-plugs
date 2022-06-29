package OfbizSpring.Util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.security.SecurityConfigurationException;
import org.apache.ofbiz.security.SecurityFactory;
import org.apache.ofbiz.service.LocalDispatcher;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HttpUtil {
    public static HttpServletRequestWrapper toWebRequest(HttpServletRequest request, GenericValue userLogin, Delegator delegator, LocalDispatcher dispatcher) throws IOException, SecurityConfigurationException {
        return new HttpServletRequestWrapper(request) {
            private byte[] requestBody;
            private final Map<String, String[]> requestParams;

            {
                JsonNode payload = new ObjectMapper()
                        .readValue(super.getReader().lines().collect(Collectors.joining()), JsonNode.class);

                requestBody = formToUrl(payload).getBytes(StandardCharsets.UTF_8);
                requestParams = UtilMisc.<String, Object>toMap(new ObjectMapper().convertValue(payload, Map.class))
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                            Object v = e.getValue();
                            return v instanceof Object[] ? (String[]) v : new String[]{String.valueOf(v)};
                        }));

                super.getSession().setAttribute("userLogin", userLogin);
                super.getServletContext().setAttribute("delegator", delegator);

                super.setAttribute("dispatcher", dispatcher);
                super.setAttribute("delegator", delegator);
                super.setAttribute("servletContext", request.getServletContext());
                super.setAttribute("security", SecurityFactory.getInstance(delegator));
            }

            @Override
            public ServletInputStream getInputStream() throws IOException {
                return new ServletInputStream() {
                    ReadListener listener = null;
                    final ByteArrayInputStream buffer = new ByteArrayInputStream(requestBody);

                    @Override
                    public int read() throws IOException {
                        int data = buffer.read();
                        if (listener != null & data == -1) {
                            listener.onAllDataRead();
                        }
                        return data;
                    }

                    @Override
                    public boolean isFinished() {
                        return buffer.available() == 0;
                    }

                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setReadListener(ReadListener listener) {
                        this.listener = listener;
                    }
                };
            }

            @Override
            public BufferedReader getReader() throws IOException {
                return new BufferedReader(new InputStreamReader(getInputStream()));
            }

            @Override
            public String getHeader(String name) {
                return name.equalsIgnoreCase("content-type") ? "application/x-www-form-urlencoded" : super.getHeader(name);
            }

            @Override
            public String getParameter(String name) {
                String[] param = requestParams.get(name);
                return param == null ? null : String.join(",", param);
            }

            public String getParam(String name) {
                String[] param = requestParams.get(name);
                return param == null ? null : String.join(",", param);
            }

            public void setParam(String name, String value) {
                requestParams.put(name, Objects.toString(value, "").split(","));
                requestBody = formToUrl(new ObjectMapper().convertValue(requestParams, JsonNode.class)).getBytes(StandardCharsets.UTF_8);
            }

            public Map<String, Object> getParams() {
                return requestParams
                        .entrySet()
                        .stream()
                        .collect(HashMap::new, (acc, v ) -> {
                            String[] value = v.getValue();
                            acc.put(v.getKey(), value == null ? null : String.join(",", value));
                        }, HashMap::putAll);
            }

            public void setParams(Map<String, String> params) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    requestParams.put(entry.getKey(), Objects.toString(entry.getValue(), "").split(","));
                }
                requestBody = formToUrl(new ObjectMapper().convertValue(requestParams, JsonNode.class)).getBytes(StandardCharsets.UTF_8);
            }

            public void clearParams() {
                requestParams.clear();
                requestBody = "".getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public Enumeration<String> getParameterNames() {
                return Collections.enumeration(requestParams.keySet());
            }

            @Override
            public String[] getParameterValues(String name) {
                return requestParams.get(name);
            }

            @Override
            public Map<String, String[]> getParameterMap() {
                return requestParams;
            }

            Map<String, JsonNode> flatten(JsonNode item, String name, Map<String, Object> cfg) {
                Map<String, JsonNode> items = new HashMap<>();
                if (item.isArray()) {
                    if ((boolean) cfg.get("ignoreArrayItems")) {
                        items.put(name, item);
                    } else {
                        String p = (String) cfg.get("pivot");
                        for (int i = 0, total = item.size(); i < total; ++i) {
                            String currentName = name.length() > 0 ? (name + p + i) : ("" + i); // String.join(p, (String[]) Arrays.stream( new String[]{name, String.valueOf(i)}).filter(String::isEmpty).toArray());
                            items.putAll(flatten(item.get(i), currentName, cfg));
                        }
                    }
                } else if (item.isObject()) {
                    String p = (String) cfg.get("pivot");
                    item.fields().forEachRemaining(e -> {
                        String currentName = name.length() > 0 ? (name + p + e.getKey()) : e.getKey();
                        items.putAll(flatten(item.get(e.getKey()), currentName, cfg));
                    });
                } else {
                    items.put(name, item);
                }
                return items;
            }

            Map<String, JsonNode> flattenForm(JsonNode data) {
                return flatten(data, "", Stream.of(
                        new AbstractMap.SimpleEntry<>("ignoreArrayItems", true),
                        new AbstractMap.SimpleEntry<>("pivot", ".")
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            }

            String formToUrl(JsonNode data) {
                return String.join("&\n", flattenForm(data).entrySet().stream().reduce(
                        new ArrayList<String>(),
                        (acc, entry) -> {
                            String k = entry.getKey();
                            JsonNode v = entry.getValue();
                            if (v.isArray()) {
                                v.forEach(iv -> acc.add(String.format("%s[]=%s", k, iv)));
                            } else {
                                acc.add(String.format("%s=%s", k, v));
                            }
                            return acc;
                        },
                        (s0, s1) -> (ArrayList<String>) Stream.concat(s0.stream(), s1.stream()).collect(Collectors.toList())
                ));
            }
        };
    }

    public static HttpServletResponseWrapper toWebResponse(HttpServletResponse response) {
        return new HttpServletResponseWrapper(response);
    }
}
