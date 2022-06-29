package OfbizSpring.Services;

import SmsGateway.http.EndpointRobi;
import SmsGateway.http.SmsProviderHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.springframework.stereotype.Component;


@Component
public class SmsProviderRobiHttp extends SmsProviderHttp implements ISmsProviderService {
    public SmsProviderRobiHttp() {
        super(new EndpointRobi(UtilMisc.toMap(
                "BaseUrl", "https://api.mobireach.com.bd",
                "UrlSuffix", "/SendTextMultiMessage",
                "UserId", "excee",
                "Password", "Rglraj321#$"
        )));
    }

    @Override
    public String getTypeName() {
        return "robi";
    }
}
