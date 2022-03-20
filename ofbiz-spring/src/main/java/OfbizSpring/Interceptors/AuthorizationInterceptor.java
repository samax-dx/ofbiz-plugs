package OfbizSpring.Interceptors;

import OfbizSpring.Util.JwtHelper;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;

public class AuthorizationInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authHeader = request.getHeader("authorization");
        String[] headerParts = (authHeader == null ? "" : authHeader).trim().split(" ");

        if (headerParts.length == 2) {
            request.setAttribute("signedParty", new JwtHelper().getTokenUser(headerParts[1]));
        } else {
            request.setAttribute("signedParty", new HashMap<String, String>());
        }

        return true;
    }
}
