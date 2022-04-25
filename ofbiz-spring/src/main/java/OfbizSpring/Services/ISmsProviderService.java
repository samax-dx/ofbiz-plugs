package OfbizSpring.Services;

import SmsGateway.ISmsProvider;


public interface ISmsProviderService extends ISmsProvider {
    String getTypeName();
}
