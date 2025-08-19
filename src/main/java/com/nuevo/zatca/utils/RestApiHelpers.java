package com.nuevo.zatca.utils;

import com.nuevo.zatca.entity.ZatcaOnboardingCredentialsEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class RestApiHelpers {

    public HttpHeaders setCommonHeadersWithZatcaAcceptVersion(String zatcaAcceptVersion) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(MediaType.parseMediaTypes("application/json"));
        headers.set("Accept-version", zatcaAcceptVersion);
        return headers;
    }

    public HttpHeaders setZatcaAuthorizationHeader(ZatcaOnboardingCredentialsEntity zatcaOnboardingCredentialsEntity, HttpHeaders headers) {
        String username = zatcaOnboardingCredentialsEntity.getBinarySecurityToken();
        String password = zatcaOnboardingCredentialsEntity.getSecret();
        String plainCreds = username + ":" + password;
        String base64Creds = Base64.getEncoder().encodeToString(plainCreds.getBytes());

        headers.set("Authorization", "Basic " + base64Creds);
        return headers;
    }

    public HttpHeaders setZatcaProdAuthorizationHeader(ZatcaOnboardingCredentialsEntity zatcaOnboardingCredentialsEntity, HttpHeaders headers) {
        String username = zatcaOnboardingCredentialsEntity.getProdBinarySecurityToken();
        String password = zatcaOnboardingCredentialsEntity.getProdSecret();
        String plainCreds = username + ":" + password;
        String base64Creds = Base64.getEncoder().encodeToString(plainCreds.getBytes());

        headers.set("Authorization", "Basic " + base64Creds);
        return headers;
    }

}
