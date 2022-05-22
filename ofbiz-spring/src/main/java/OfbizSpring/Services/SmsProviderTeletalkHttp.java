package OfbizSpring.Services;

import SmsGateway.http.EndpointTeletalk;
import SmsGateway.http.SmsProviderHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.springframework.stereotype.Component;


@Component
public class SmsProviderTeletalkHttp extends SmsProviderHttp implements ISmsProviderService {
    public SmsProviderTeletalkHttp() {
        super(new EndpointTeletalk(UtilMisc.toMap(
                "BaseUrl", "http://bulkmsg.teletalk.com.bd/api",
                "UrlSuffix", "/sendSMS",
                "UserId", "exceeli",
                "AccountCode", "1005340",
                "Password", "exceeliH#6T9P"
        )));
    }

    @Override
    public String getTypeName() {
        return "teletalk";
    }
}
