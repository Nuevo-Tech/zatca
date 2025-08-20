package com.nuevo.zatca.service.sdkservices;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.zatca.sdk.dto.ApplicationPropertyDto;
import com.zatca.sdk.service.CsrGenerationService;
import com.zatca.sdk.service.GeneratorTemplate;
import org.apache.log4j.Logger;

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

    // ✅ You can add your own helper methods too
    private void customMethod() {
        System.out.println("Custom method for additional logic");
    }
}