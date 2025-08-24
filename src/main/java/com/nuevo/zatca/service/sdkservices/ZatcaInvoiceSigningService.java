package com.nuevo.zatca.service.sdkservices;

import com.gazt.einvoicing.signing.service.SigningService;
import com.gazt.einvoicing.signing.service.impl.SigningServiceImpl;
import com.gazt.einvoicing.signing.service.model.InvoiceSigningResult;
import com.zatca.config.ResourcesPaths;
import com.zatca.sdk.dto.ApplicationPropertyDto;
import com.zatca.sdk.util.ECDSAUtil;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.util.Base64;

import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class ZatcaInvoiceSigningService extends ZatcaGeneratorTemplate {
    private static final Logger LOG = Logger.getLogger(ZatcaInvoiceSigningService.class);
    private PrivateKey privateKey;
    private String certificateStr;
    private InvoiceSigningResult invoiceSigningResult;
    private SigningService signingService = new ZatcaSigningServiceImpl();
    private String signedInvoice;
    private ResourcesPaths paths = ResourcesPaths.getInstance();

    private boolean generateSignedInvoiceFile(ApplicationPropertyDto property) {
        try {
            LOG.debug("generate signed invoice file ");
            this.signedInvoice = this.invoiceSigningResult.getSingedXML();
            return true;
        } catch (Exception var3) {
            LOG.error("failed to sign invoice [unable to write the signed content] ");
            return false;
        }
    }

    protected boolean validateInputFiles() {
        boolean result = this.validatePrivateKey();
        return !result ? false : this.validateCertificate();
    }

    private boolean validateCertificate() {
        this.certificateStr = this.property.getInputCertificate();

        ByteArrayInputStream byteArrayInputStream = null;

        try {
            byte[] certificateBytes = this.certificateStr.getBytes(StandardCharsets.UTF_8);
            byteArrayInputStream = new ByteArrayInputStream(Base64.getDecoder().decode(certificateBytes));
        } catch (Exception var6) {
            LOG.error("failed to sign invoice [please provide certificate decoded base64 ");
            return false;
        }

        try {
            CertificateFactory certificatefactory = CertificateFactory.getInstance("X.509");
            certificatefactory.generateCertificate(byteArrayInputStream);
            return true;
        } catch (Exception var5) {
            LOG.error("failed to sign invoice [please provide a valid certificate] ");
            return false;
        }
    }

    private boolean validatePrivateKey() {
        String key = null;
        key = this.property.getInputPrivateKey();

        if (!key.contains("-----BEGIN EC PRIVATE KEY-----") && !key.contains("-----END EC PRIVATE KEY-----")) {
            try {
                this.privateKey = ECDSAUtil.getPrivateKey(key);
            } catch (Exception var7) {
                try {
                    this.privateKey = ECDSAUtil.loadPrivateKey(key);
                } catch (Exception e) {
                    LOG.error("failed to sign invoice [please provide a valid private key] ");
                    e.printStackTrace();
                    return false;
                }
            }

            return true;
        } else {
            LOG.error("failed to sign invoice [please provide private key without -----BEGIN EC PRIVATE KEY----- and -----END EC PRIVATE KEY-----] ");
            return false;
        }
    }

    public boolean signInvoice() {
        try {
            this.invoiceSigningResult = this.signingService.signDocument(this.invoiceStr, this.privateKey, this.certificateStr, "");
        } catch (Exception e1) {
            LOG.error("failed to sign invoice [" + e1.getMessage() + "] ");
            return false;
        }

        return this.invoiceSigningResult != null;
    }

    public boolean loadInput() {
        return this.loadInvoiceFile();
    }

    public boolean validateInput() {
        return this.validateInputFiles();
    }

    public boolean process() {
        return this.signInvoice();
    }

    public String generateOutputContent() {
//            String invoiceHash = this.invoiceSigningResult.getInvoiceHash();
//            LOG.info(" *** INVOICE HASH = " + this.invoiceSigningResult.getInvoiceHash());
        String signedInvoiceXml = this.invoiceSigningResult.getSingedXML();
        if (!(signedInvoiceXml == null || signedInvoiceXml.isEmpty())) {
            LOG.info("invoice has been signed successfully");
        }
        return signedInvoiceXml;
    }
}
