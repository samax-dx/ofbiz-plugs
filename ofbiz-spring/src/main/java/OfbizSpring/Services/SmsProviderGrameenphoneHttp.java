package OfbizSpring.Services;

import SmsGateway.http.EndpointGrameenphone;
import SmsGateway.http.SmsProviderHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.springframework.stereotype.Component;


@Component
public class SmsProviderGrameenphoneHttp extends SmsProviderHttp implements ISmsProviderService {
    public SmsProviderGrameenphoneHttp() {
        super(new EndpointGrameenphone(UtilMisc.toMap(
                "BaseUrl", "https://gpcmp.grameenphone.com/ecmapigw/webresources/ecmapigw.v2",
                "UrlSuffix", "",
                "UserId", "ELAdmin_3338",
                "Password", "Rgl12345^",
                "cli", "EXCEELI"
        )));
    }

    @Override
    public String getTypeName() {
        return "grameenphone";
    }
}
