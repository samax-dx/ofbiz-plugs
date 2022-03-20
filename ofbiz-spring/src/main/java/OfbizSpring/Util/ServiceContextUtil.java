package OfbizSpring.Util;

import java.util.Map;

public class ServiceContextUtil {
    public static Map<String, Object> authorizeContext(Map<String, Object> context) {
        context.put("login.username", "admin");
        context.put("login.password", "ofbiz");
        return context;
    }
}
