package OfbizSpring.Interceptors;

import OfbizSpring.Util.JwtHelper;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AuthorizationInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        ServletContext context = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest()
                .getServletContext();

        String authHeader = request.getHeader("authorization");
        String[] headerParts = authHeader == null ? new String[]{} : authHeader.split(" ");
        String token = headerParts.length == 2 ? headerParts[1] : "";

        context.setAttribute("OfbizSpring.user", new JwtHelper().getTokenUser(token));

        return true;
    }
}
