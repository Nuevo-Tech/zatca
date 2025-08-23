package com.nuevo.zatca.service.sdkservices;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gazt.einvoicing.signing.service.SigningService;
import com.gazt.einvoicing.signing.service.impl.SigningServiceImpl;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ZatcaHashGenerationService extends ZatcaGeneratorTemplate {
    private static final Logger LOG = Logger.getLogger(ZatcaHashGenerationService.class);
    private SigningService signingService = new SigningServiceImpl();
    private String invoiceHash;

    public boolean loadInput() {
        return this.loadInvoiceFile();
    }

    public boolean validateInput() {
        return true;
    }

    public boolean process() {
        try {
            LOG.debug("generate invoice hash");
            this.invoiceHash = this.signingService.generateInvoiceHash(this.invoiceStr);
            return true;
        } catch (Exception e) {
            LOG.error("failed to generate hash - " + e.getMessage());
            return false;
        }
    }

    public String generateOutputContent() {
        LOG.info("invoice hash has been generated successfully");
        return this.invoiceHash;
    }
}
