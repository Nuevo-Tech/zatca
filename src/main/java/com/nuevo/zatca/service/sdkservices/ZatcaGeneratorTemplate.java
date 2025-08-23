package com.nuevo.zatca.service.sdkservices;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.zatca.sdk.dto.ApplicationPropertyDto;
import com.zatca.sdk.service.CsrGenerationService;
import com.zatca.sdk.service.GeneratorTemplate;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public abstract class ZatcaGeneratorTemplate extends CsrGenerationService {
    private static final Logger LOG = Logger.getLogger(GeneratorTemplate.class);
    protected ZatcaApplicationPropertyDto property;
    protected String invoiceStr;

    protected abstract String generateOutputContent() throws Exception;

    public final String generate2(ZatcaApplicationPropertyDto property) throws Exception {
        this.property = property;
        boolean result = this.loadInput();
        if (!result) {
            return null;
        } else {
            result = this.validateInput();
            if (!result) {
                return null;
            } else {
                result = this.process();
                return !result ? null : generateOutputContent();
            }
        }
    }

    protected boolean loadInvoiceFile() {
        try {
            LOG.debug("load invoice file");
            InputStream inputStream = this.property.getFileInputStream();
            this.invoiceStr = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return true;
        } catch (IOException var2) {
            LOG.error("unable to read the invoice file content");
            return false;
        }
    }

    // ✅ You can add your own helper methods too
    private void customMethod() {
        System.out.println("Custom method for additional logic");
    }
}