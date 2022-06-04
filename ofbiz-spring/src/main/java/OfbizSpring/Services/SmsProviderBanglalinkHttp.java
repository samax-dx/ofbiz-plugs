package OfbizSpring.Services;

import SmsGateway.http.EndpointBanglalink;
import SmsGateway.http.SmsProviderHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.springframework.stereotype.Component;


@Component
public class SmsProviderBanglalinkHttp extends SmsProviderHttp implements ISmsProviderService {
    public SmsProviderBanglalinkHttp() {
        super(new EndpointBanglalink(UtilMisc.toMap(
                "BaseUrl", "https://vas.banglalink.net/sendSMS",
                "UrlSuffix", "/sendSMS",
                "UserId", "Exceeli123",
                "Password", "Exceeli123@2022"
        )));
    }

    @Override
    public String getTypeName() {
        return "banglalink";
    }
}
