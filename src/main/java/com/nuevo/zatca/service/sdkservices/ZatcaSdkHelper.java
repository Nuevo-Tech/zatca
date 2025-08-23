package com.nuevo.zatca.service.sdkservices;

import java.io.InputStream;

public class ZatcaSdkHelper {

    public String generateCsrAndPrivateKey(InputStream inputCsrConfigStream, boolean nonProd) throws Exception {
        ZatcaApplicationPropertyDto applicationProperty = new ZatcaApplicationPropertyDto();
        applicationProperty.setGenerateCsr(true);
        applicationProperty.setFileInputStream(inputCsrConfigStream);
        applicationProperty.setNonPrdServer(nonProd);
        ZatcaGeneratorTemplate generateService = new ZatcaCsrGenerationService();
        String csr = generateService.generate2(applicationProperty);
        return csr;
    }

    public String generateInvoiceRequest(InputStream inputFileStream, boolean nonProd) throws Exception {
        ZatcaApplicationPropertyDto applicationProperty = new ZatcaApplicationPropertyDto();
        applicationProperty.setGenerateInvoiceRequest(true);
        applicationProperty.setFileInputStream(inputFileStream);
        applicationProperty.setNonPrdServer(nonProd);
        ZatcaGeneratorTemplate generateService = new ZatcaInvoiceRequestGenerationService();
        String generatedInvoiceRequest = generateService.generate2(applicationProperty);
        return generatedInvoiceRequest;
    }

    public String generateInvoiceHash(InputStream inputFileStream, boolean nonProd) throws Exception {
        ZatcaApplicationPropertyDto applicationProperty = new ZatcaApplicationPropertyDto();
        applicationProperty.setGenerateHash(true);
        applicationProperty.setFileInputStream(inputFileStream);
        applicationProperty.setNonPrdServer(nonProd);
        ZatcaGeneratorTemplate generateService = new ZatcaHashGenerationService();
        String generatedInvoiceHash = generateService.generate2(applicationProperty);
        return generatedInvoiceHash;
    }
}
