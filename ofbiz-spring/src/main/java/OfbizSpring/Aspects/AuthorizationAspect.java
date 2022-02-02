package OfbizSpring.Aspects;

import OfbizSpring.Util.MapUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.ServletContext;
import java.util.Map;

@Aspect
public class AuthorizationAspect {
    @Around("@annotation(OfbizSpring.Annotations.Authorize)")
    public Object authorize(ProceedingJoinPoint joinPoint) {
        try {
            ServletContext context = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                    .getRequest()
                    .getServletContext();

            Map<String, Object> user = MapUtil.remap(context.getAttribute("OfbizSpring.user"));
            if (user == null) return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

            return joinPoint.proceed();
        } catch (Throwable e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
