package OfbizSpring.Aspects;

import OfbizSpring.Util.MapUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.Map;

@Aspect
public class OfbizServiceAspect {
    @Around("@annotation(OfbizSpring.Annotations.OfbizService)")
    public Object ofbizService(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            Object[] jpArgs = joinPoint.getArgs();
            if (jpArgs.length == 0) {
                return joinPoint.proceed();
            }

            Object jpArg0 = jpArgs[0];
            try {
                Map<String, Object> payload = MapUtil.remap(jpArg0);
                payload.put("login.username", "admin");
                payload.put("login.password", "ofbiz");
                return joinPoint.proceed();
            } catch (Exception e) {
//                Class<?> someClass = jpArg0.getClass();
//
//                Field field = someClass.getDeclaredField("custom");
//                field.setAccessible(true);
//                field.set(jpArg0, "custom");
//                field.setAccessible(false);

                return joinPoint.proceed();
            }
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}
