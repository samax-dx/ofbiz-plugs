package OfbizSpring.Configurations;

import OfbizSpring.Aspects.AuthorizationAspect;
import OfbizSpring.Aspects.OfbizServiceAspect;
import OfbizSpring.Interceptors.AuthorizationInterceptor;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Map;
import java.util.HashMap;

/**
 * Use delegator and dispatcher from OFBiz as Spring-managed beans.
 */
@Configuration
@EnableWebMvc
@EnableAspectJAutoProxy
public class OfbizConfiguration implements WebMvcConfigurer {
    @Override
    public void configurePathMatch(PathMatchConfigurer config) {
        AntPathMatcher matcher = new AntPathMatcher();
        matcher.setCaseSensitive(false);
        config.setPathMatcher(matcher);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthorizationInterceptor());
    }

    @Bean
    public AuthorizationAspect authorizationAspect() {
        return new AuthorizationAspect();
    }

    @Bean
    public OfbizServiceAspect ofbizServiceAspect() {
        return new OfbizServiceAspect();
    }

    @Bean
    public Delegator delegator() {
        return DelegatorFactory.getDelegator("default");
    }

    @Bean
    public LocalDispatcher dispatcher() {
        return ServiceContainer.getLocalDispatcher("spring", delegator());
    }

    @Bean("SmsSenderServiceName")
    public String smsSenderServiceName() {
        return "spSendSmsBrilliant";
    }

    @Bean("SmsGatewayConfig")
    public Map<String, Object> smsGatewayConfig() {
        Map<String, Object> args = new HashMap<>();
        args.put("BaseUrl", "http://sms.brilliant.com.bd:6005/api/v2");
        args.put("UrlSuffix", "/SendSMS");
        args.put("ClientId", "2f6d4dd7-62df-4db1-abcf-7a99d9949509");
        args.put("ApiKey", "9fZqmL+O5nGvu9E+cmEPDt98bzseSF/IJ5UMa0MkLec=");
        return args;
    }
}
