package com.nuevo.zatca.service.sdkservices;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class ZatcaSdkHelper {

    @Value("${zatca.sdk.env.nonprod}")
    private Boolean nonProd;

    @Value("${zatca.sdk.env.sim}")
    private Boolean sim;

    public String generateCsrAndPrivateKey(InputStream inputCsrConfigStream) throws Exception {
        ZatcaApplicationPropertyDto applicationProperty = new ZatcaApplicationPropertyDto();
        applicationProperty.setGenerateCsr(true);
        applicationProperty.setFileInputStream(inputCsrConfigStream);
        applicationProperty.setNonPrdServer(nonProd);
        applicationProperty.setSimulation(sim);
        ZatcaGeneratorTemplate generateService = new ZatcaCsrGenerationService();
        String csr = generateService.generate2(applicationProperty);
        return csr;
    }

    public String generateInvoiceRequest(InputStream inputFileStream) throws Exception {
        ZatcaApplicationPropertyDto applicationProperty = new ZatcaApplicationPropertyDto();
        applicationProperty.setGenerateInvoiceRequest(true);
        applicationProperty.setFileInputStream(inputFileStream);
        applicationProperty.setNonPrdServer(nonProd);
        applicationProperty.setSimulation(sim);
        ZatcaGeneratorTemplate generateService = new ZatcaInvoiceRequestGenerationService();
        String generatedInvoiceRequest = generateService.generate2(applicationProperty);
        return generatedInvoiceRequest;
    }

    public String generateInvoiceHash(InputStream inputFileStream) throws Exception {
        ZatcaApplicationPropertyDto applicationProperty = new ZatcaApplicationPropertyDto();
        applicationProperty.setGenerateHash(true);
        applicationProperty.setFileInputStream(inputFileStream);
        applicationProperty.setNonPrdServer(nonProd);
        applicationProperty.setSimulation(sim);
        ZatcaGeneratorTemplate generateService = new ZatcaHashGenerationService();
        String generatedInvoiceHash = generateService.generate2(applicationProperty);
        return generatedInvoiceHash;
    }

    public String generateSignedInvoiceXml(InputStream inputFileStream, String inputPrivateKey, String inputCertificate) throws Exception {
        ZatcaApplicationPropertyDto applicationProperty = new ZatcaApplicationPropertyDto();
        applicationProperty.setGenerateHash(true);
        applicationProperty.setFileInputStream(inputFileStream);
        applicationProperty.setInputCertificate(inputCertificate);
        applicationProperty.setInputPrivateKey(inputPrivateKey);
        applicationProperty.setNonPrdServer(nonProd);
        applicationProperty.setSimulation(sim);
        ZatcaGeneratorTemplate generateService = new ZatcaInvoiceSigningService();
        String signedInvoiceXml = generateService.generate2(applicationProperty);
        return signedInvoiceXml;
    }
}
