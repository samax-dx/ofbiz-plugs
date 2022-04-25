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


///TODO: extend dispatcher to authorize all runSync

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
    public Delegator delegator() {
        return DelegatorFactory.getDelegator("default");
    }

    @Bean
    public LocalDispatcher dispatcher() {
        return ServiceContainer.getLocalDispatcher("spring", delegator());
    }

    @Bean
    public AuthorizationAspect authorizationAspect() {
        return new AuthorizationAspect(delegator(), dispatcher());
    }

    @Bean
    public OfbizServiceAspect ofbizServiceAspect() {
        return new OfbizServiceAspect(delegator(), dispatcher());
    }
}
