package com.nuevo.zatca.service.sdkservices;

import java.io.InputStream;

public class ZatcaSdkHelper {

    public String generateCsrAndPrivateKey(InputStream inputCsrConfigStream, boolean nonProd) throws Exception {
        ZatcaApplicationPropertyDto applicationProperty = new ZatcaApplicationPropertyDto();
        applicationProperty.setGenerateCsr(true);
        applicationProperty.setInputStreamOfCsrConfig(inputCsrConfigStream);
        applicationProperty.setNonPrdServer(nonProd);
        ZatcaGeneratorTemplate generateService = new ZatcaCsrGenerationService();
        String csr = generateService.generate2(applicationProperty);
        return csr;
    }
}
