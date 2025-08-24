package com.nuevo.zatca.service.sdkservices;

import com.gazt.einvoicing.digitalsignature.service.DigitalSignatureService;
import com.gazt.einvoicing.digitalsignature.service.impl.DigitalSignatureServiceImpl;
import com.gazt.einvoicing.digitalsignature.service.model.DigitalSignature;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ZatcaDigitalSignatureServiceImpl extends DigitalSignatureServiceImpl {
    private static final Logger LOG = Logger.getLogger(DigitalSignatureServiceImpl.class.getName());

    public DigitalSignature getDigitalSignature(String xmlDocument, PrivateKey privateKey, String xmlHashing) {
        byte[] xmlHashingBytes = Base64.getDecoder().decode(xmlHashing.getBytes(StandardCharsets.UTF_8));
        byte[] digitalSignatureBytes = this.signECDSA(privateKey, xmlHashingBytes);
        DigitalSignature digitalSignature = new DigitalSignature();
        digitalSignature.setDigitalSignature(Base64.getEncoder().encodeToString(digitalSignatureBytes));
        digitalSignature.setXmlHashing(xmlHashingBytes);
        return digitalSignature;
    }

    byte[] signECDSA(PrivateKey privateKey, byte[] messageHash) {
        try {
            Signature signature = Signature.getInstance("SHA256withECDSA", new BouncyCastleProvider());
            signature.initSign(privateKey);
            signature.update(messageHash);
            return signature.sign();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e.getMessage());
            return new byte[0];
        }
    }
}
