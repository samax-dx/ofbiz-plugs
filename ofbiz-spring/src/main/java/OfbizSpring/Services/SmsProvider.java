package OfbizSpring.Services;

import SmsGateway.ISmsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class SmsProvider {
    @Autowired
    private List<ISmsProviderService> services;

    private static final Map<String, ISmsProviderService> myServiceCache = new HashMap<>();

    @PostConstruct
    public void initSmsProviderCache() {
        for(ISmsProviderService service : services) {
            myServiceCache.put(service.getTypeName(), service);
        }
    }

    public static ISmsProvider getService(String type) {
        ISmsProvider service = myServiceCache.get(type);
        if(service == null) {
            throw new RuntimeException("Unknown service type: " + type);
        }
        return service;
    }
}
