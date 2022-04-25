package OfbizSpring.Services;

import SmsGateway.http.EndpointBrilliant;
import SmsGateway.http.SmsProviderHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.springframework.stereotype.Component;


@Component
public class SmsProviderBrilliantHttp extends SmsProviderHttp implements ISmsProviderService {
    public SmsProviderBrilliantHttp() {
        super(new EndpointBrilliant(UtilMisc.toMap(
                "BaseUrl", "http://sms.brilliant.com.bd:6005/api/v2",
                "UrlSuffix", "/SendSMS",
                "ClientId", "2f6d4dd7-62df-4db1-abcf-7a99d9949509",
                "ApiKey", "9fZqmL+O5nGvu9E+cmEPDt98bzseSF/IJ5UMa0MkLec="
        )));
    }

    @Override
    public String getTypeName() {
        return "brilliant";
    }
}
