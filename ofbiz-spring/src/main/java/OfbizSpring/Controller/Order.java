package OfbizSpring.Controller;

import OfbizSpring.Annotations.OfbizService;
import OfbizSpring.Util.MapUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.order.shoppingcart.CheckOutEvents;
import org.apache.ofbiz.order.shoppingcart.ShoppingCartEvents;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.security.SecurityFactory;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ReadListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/Order")
public class Order {
    @Autowired
    private Delegator delegator;

    @Autowired
    private LocalDispatcher dispatcher;

    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/createOrder",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object createOrder(HttpServletRequest request, HttpServletResponse response) {
        try {
            request = new HttpServletRequestWrapper(request) {
                private final byte[] requestBody;
                private final Map<String, String[]> requestParams;

                {
                    JsonNode payload = new ObjectMapper()
                            .readValue(super.getReader().lines().collect(Collectors.joining()), JsonNode.class);

                    requestBody = formToUrl(payload).getBytes(StandardCharsets.UTF_8);
                    requestParams = MapUtil.remap(new ObjectMapper().convertValue(payload, Map.class))
                            .entrySet()
                            .stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                                Object v = e.getValue();
                                return v.getClass().isArray() ? (String[]) v : new String[]{(String) v};
                            }));

//                    setAttribute("dispatcher", dispatcher);
//                    setAttribute("delegator", delegator);
//                    setAttribute("security", security);
//                    setAttribute("servletContext", servletContext);
//
//                    getSession().setAttribute("userLogin", userLogin);
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
                    return param == null || param.length == 0 ? null : param[0];
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
                    return flatten(data, "", new HashMap<String, Object>() {{
                        put("ignoreArrayItems", true);
                        put("pivot", ".");
                    }});
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

            return dispatcher.runSync("spCreateOrder", UtilMisc.toMap(
                    "request", request, "response", response, "login.username", "admin", "login.password", "ofbiz"
            ));
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }
    }
}
