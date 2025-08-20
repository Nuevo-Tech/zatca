package com.nuevo.zatca.service.sdkservices;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zatca.sdk.dto.ApplicationPropertyDto;
import com.zatca.sdk.dto.CsrGeneratorDto;
import com.zatca.sdk.util.ECDSAUtil;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.microsoft.MicrosoftObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.RFC4519Style;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZatcaCsrGenerationService extends ZatcaGeneratorTemplate {
    private static final Logger LOG = Logger.getLogger(ZatcaCsrGenerationService.class);
    private CsrGeneratorDto csrGeneratorDto;
    Locale locale = new Locale("en", "US");

    private ResourceBundle resourceBundle;

    public ZatcaCsrGenerationService() {
        this.resourceBundle = ResourceBundle.getBundle("application", this.locale);
    }


    public boolean loadInput() {
        Properties csrInputDataProperty = this.loadCsrConfigFile(this.property.getInputStreamOfCsrConfig());
        if (csrInputDataProperty.isEmpty()) {
            return false;
        } else {
            this.csrGeneratorDto = this.mappingCsrInputData(csrInputDataProperty);
            return this.csrGeneratorDto != null;
        }
    }

    public boolean process() {
        return this.generate(this.csrGeneratorDto, this.property.isNonPrdServer(), this.property.isSimulation());
    }


    public String generateOutputContent() throws Exception {
        String privateKeyString = this.generatePrivateKey(this.property, this.csrGeneratorDto);
        String csr = this.generateCsr(this.property, this.csrGeneratorDto);

        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> result = new HashMap<>();
        result.put("csr", csr);
        result.put("privateKey", privateKeyString);
        return mapper.writeValueAsString(result);
    }

    private String generatePrivateKey(ApplicationPropertyDto property, CsrGeneratorDto csrGeneratorDto) {
        try {
            LOG.debug("generate private key file ");
            String privateKey = "";
            if (property.isOutputPemFormat()) {
                privateKey = csrGeneratorDto.getPrivateKeyPemFormat();
            } else {
                privateKey = csrGeneratorDto.getPrivateKey();
            }
            return privateKey;
        } catch (Exception var4) {
            LOG.error("failed to generate csr [unable to generate private key file] ");
            return null;
        }
    }

    private String generateCsr(ApplicationPropertyDto property, CsrGeneratorDto csrGeneratorDto) {
        try {
            LOG.debug("generate csr file ");
            String csr = "";
            if (property.isOutputPemFormat()) {
                csr = csrGeneratorDto.getCsrPemFormat();
            } else {
                csr = csrGeneratorDto.getCsr();
            }
            return csr;
        } catch (Exception var4) {
            LOG.error("failed to generate csr [unable to generate csr file] ");
            return null;
        }
    }


    private Boolean generate(CsrGeneratorDto csrGeneratorDto, boolean isNonPrd, boolean isSim) {
        LOG.debug("generate csr");
        KeyPair pair = null;

        try {
            pair = ECDSAUtil.getKeyPair();
        } catch (Exception var24) {
            LOG.error("failed to generate csr [unable to generate key pair] ");
            return false;
        }

        PublicKey publicKey = pair.getPublic();
        PrivateKey privateKey = pair.getPrivate();
        X500NameBuilder subject = new X500NameBuilder();
        subject.addRDN(BCStyle.C, csrGeneratorDto.getCountryName());
        subject.addRDN(BCStyle.OU, csrGeneratorDto.getOrganizationUnitName());
        subject.addRDN(BCStyle.O, csrGeneratorDto.getOrganizationName());
        subject.addRDN(BCStyle.CN, csrGeneratorDto.getCommonName());
        X500Name x500 = subject.build();
        X500NameBuilder x500NameBuilderOtherAttributes = new X500NameBuilder();
        x500NameBuilderOtherAttributes.addRDN(RFC4519Style.sn, csrGeneratorDto.getSerialNumber());
        x500NameBuilderOtherAttributes.addRDN(BCStyle.UID, csrGeneratorDto.getOrganizationIdentifier());
        x500NameBuilderOtherAttributes.addRDN(RFC4519Style.title, csrGeneratorDto.getInvoiceType());
        x500NameBuilderOtherAttributes.addRDN(RFC4519Style.registeredAddress, csrGeneratorDto.getLocation());
        x500NameBuilderOtherAttributes.addRDN(RFC4519Style.businessCategory, csrGeneratorDto.getIndustry());
        X500Name x500OtherAttributes = x500NameBuilderOtherAttributes.build();
        Extension subjectAltName = null;

        try {
            String certificateTemplateName = "";
            if (!isNonPrd && !isSim) {
                certificateTemplateName = "ZATCA-Code-Signing";
            } else if (isSim) {
                certificateTemplateName = "PREZATCA-Code-Signing";
            } else {
                certificateTemplateName = "TSTZATCA-Code-Signing";
            }

            subjectAltName = new Extension(MicrosoftObjectIdentifiers.microsoftCertTemplateV1, false, new DEROctetString(new DisplayText(2, certificateTemplateName)));
        } catch (IOException var25) {
            LOG.error("failed to generate csr [unable to encode of an ASN.1 object] ");
            return false;
        }

        GeneralName[] generalNamesArray = new GeneralName[]{new GeneralName(x500OtherAttributes)};
        GeneralNames generalNames = new GeneralNames(generalNamesArray);
        ContentSigner signGen = null;

        try {
            signGen = (new JcaContentSignerBuilder("SHA256WITHECDSA")).build(privateKey);
        } catch (OperatorCreationException var23) {
            LOG.error("failed to generate csr [unable to build sign generator] ");
            return false;
        }

        PKCS10CertificationRequestBuilder certificateBuilder = new JcaPKCS10CertificationRequestBuilder(x500, publicKey);

        try {
            certificateBuilder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, new Extensions(new Extension[]{subjectAltName, Extension.create(Extension.subjectAlternativeName, false, generalNames)})).build(signGen);
        } catch (IOException var22) {
            LOG.error("failed to generate csr [the value of subject template name cannot be encoded into bytes] ");
            return false;
        }

        PKCS10CertificationRequest certRequest = certificateBuilder.build(signGen);
        String certificateStr = null;

        try {
            certificateStr = this.transform("CERTIFICATE REQUEST", certRequest.getEncoded());
        } catch (IOException var21) {
            LOG.error("failed to generate csr [the value of csr cannot be encoded into bytes] ");
            return false;
        }

        if (certificateStr == null) {
            return false;
        } else {
            csrGeneratorDto.setCsr(new String(Base64.getEncoder().encode(certificateStr.getBytes()), StandardCharsets.UTF_8));
            String privateKeyPEM = null;

            try {
                privateKeyPEM = this.transform("EC PRIVATE KEY", privateKey.getEncoded());
            } catch (Exception var20) {
                LOG.error("failed to generate private key [the value of private key cannot be encoded into bytes] ");
                return false;
            }

            if (privateKeyPEM == null) {
                return false;
            } else {
                String key = privateKeyPEM.replace("-----BEGIN EC PRIVATE KEY-----", "").replaceAll(System.lineSeparator(), "").replace("-----END EC PRIVATE KEY-----", "");
                csrGeneratorDto.setCsrPemFormat(certificateStr);
                csrGeneratorDto.setPrivateKey(key);
                csrGeneratorDto.setPrivateKeyPemFormat(privateKeyPEM);
                return true;
            }
        }
    }

    public boolean validateInput() {
        return this.validateCsrConfigInputFile(this.csrGeneratorDto);
    }

    private boolean validateCsrConfigInputFile(CsrGeneratorDto csrGeneratorDto) {
        LOG.debug("validate csr configuration file ");
        if (csrGeneratorDto.getCommonName() != null && !csrGeneratorDto.getCommonName().trim().isEmpty()) {
            if (csrGeneratorDto.getSerialNumber() != null && !csrGeneratorDto.getSerialNumber().trim().isEmpty()) {
                if (csrGeneratorDto.getSerialNumber().contains("=")) {
                    LOG.error("Invalid serial number, The serial number should only contain alphanumeric characters, and special characters ('=') are not allowed");
                    return false;
                } else if (!csrGeneratorDto.getSerialNumber().matches("1-(.+)\\|2-(.+)\\|3-(.+)")) {
                    LOG.error("invalid serial number, serial number should be in regular expression format (1-...|2-...|3-....)");
                    return false;
                } else if (csrGeneratorDto.getOrganizationIdentifier() != null && !csrGeneratorDto.getOrganizationIdentifier().trim().isEmpty()) {
                    if (csrGeneratorDto.getOrganizationIdentifier().length() != 15) {
                        LOG.error("invalid organization identifier, please provide a valid 15 digit of your vat number");
                        return false;
                    } else if (csrGeneratorDto.getOrganizationIdentifier().charAt(0) != '3') {
                        LOG.error("invalid organization identifier, organization identifier should be started with digit 3");
                        return false;
                    } else if (csrGeneratorDto.getOrganizationIdentifier().charAt(csrGeneratorDto.getOrganizationIdentifier().length() - 1) != '3') {
                        LOG.error("invalid organization identifier, organization identifier should be end with digit 3");
                        return false;
                    } else if (csrGeneratorDto.getOrganizationUnitName() != null && !csrGeneratorDto.getOrganizationUnitName().trim().isEmpty()) {
                        if (csrGeneratorDto.getOrganizationIdentifier().charAt(10) == '1' && csrGeneratorDto.getOrganizationUnitName().length() != 10) {
                            LOG.error("Organization Unit Name must be the 10-digit TIN number of the individual group member whose device is being onboarded");
                            return false;
                        } else if (csrGeneratorDto.getOrganizationName() != null && !csrGeneratorDto.getOrganizationName().trim().isEmpty()) {
                            if (csrGeneratorDto.getCountryName() != null && !csrGeneratorDto.getCountryName().trim().isEmpty()) {
                                if (csrGeneratorDto.getCountryName().length() <= 3 && csrGeneratorDto.getCountryName().length() >= 2) {
                                    if (csrGeneratorDto.getInvoiceType() != null && !csrGeneratorDto.getInvoiceType().trim().isEmpty()) {
                                        if (csrGeneratorDto.getInvoiceType().length() != 4) {
                                            LOG.error("invalid invoice type, please provide a valid invoice type");
                                            return false;
                                        } else if (!Pattern.matches("^[0-1]{4}$", csrGeneratorDto.getInvoiceType())) {
                                            LOG.error("invalid invoice type, please provide a valid invoice type");
                                            return false;
                                        } else if (csrGeneratorDto.getLocation() != null && !csrGeneratorDto.getLocation().trim().isEmpty()) {
                                            if (csrGeneratorDto.getIndustry() != null && !csrGeneratorDto.getIndustry().trim().isEmpty()) {
                                                if (csrGeneratorDto.getCommonName() != null && this.checkSpecialCharacter(csrGeneratorDto.getCommonName(), "CommonName")) {
                                                    return false;
                                                } else if (csrGeneratorDto.getOrganizationUnitName() != null && this.checkSpecialCharacter(csrGeneratorDto.getOrganizationUnitName(), "OrganizationUnitName")) {
                                                    return false;
                                                } else if (csrGeneratorDto.getOrganizationName() != null && this.checkSpecialCharacter(csrGeneratorDto.getOrganizationName(), "OrganizationName")) {
                                                    return false;
                                                } else if (csrGeneratorDto.getLocation() != null && this.checkSpecialCharacter(csrGeneratorDto.getLocation(), "Location")) {
                                                    return false;
                                                } else {
                                                    return csrGeneratorDto.getIndustry() == null || !this.checkSpecialCharacter(csrGeneratorDto.getIndustry(), "Industry");
                                                }
                                            } else {
                                                LOG.error("industry is mandatory filed");
                                                return false;
                                            }
                                        } else {
                                            LOG.error("location is mandatory field");
                                            return false;
                                        }
                                    } else {
                                        LOG.error("invoice type is mandatory field");
                                        return false;
                                    }
                                } else {
                                    LOG.error("invalid country code name, please provide a valid country code name");
                                    return false;
                                }
                            } else {
                                LOG.error("country code name is mandatory field");
                                return false;
                            }
                        } else {
                            LOG.error("organization name is mandatory field");
                            return false;
                        }
                    } else {
                        LOG.error("organization unit name is mandatory field");
                        return false;
                    }
                } else {
                    LOG.error("organization identifier is mandatory field");
                    return false;
                }
            } else {
                LOG.error("serial number is mandatory field");
                return false;
            }
        } else {
            LOG.error("common name is mandatory field");
            return false;
        }
    }

    private boolean checkSpecialCharacter(String inputCsr, String inputMsg) {
        Pattern special = Pattern.compile(this.getMessage("specialCharacterRegex"));
        Matcher hasSpecial = special.matcher(inputCsr);
        if (hasSpecial.find()) {
            String pattern = this.getMessage("SpecialCharacter");
            String message = MessageFormat.format(pattern, inputMsg);
            LOG.error(message);
            return true;
        } else {
            return false;
        }
    }

    private String getMessage(String key) {
        return this.resourceBundle.getString(key);
    }

    private String transform(String type, byte[] certificateRequest) {
        try {
            LOG.debug("transform certificate into string");
            PemObject pemObject = new PemObject(type, certificateRequest);
            StringWriter stringWriter = new StringWriter();
            PEMWriter pemWriter = new PEMWriter(stringWriter);
            pemWriter.writeObject(pemObject);
            pemWriter.close();
            stringWriter.close();
            return stringWriter.toString();
        } catch (Exception e) {
            LOG.error(e.getMessage());
            return null;
        }
    }

    private Properties loadCsrConfigFile(InputStream csrConfigStream) {
        LOG.debug("Loading CSR configuration from InputStream");
        Properties csrInputDataProperty = new Properties();

        try {
            try (InputStreamReader reader = new InputStreamReader(csrConfigStream, StandardCharsets.UTF_8)) {
                csrInputDataProperty.load(reader);
            }
            return csrInputDataProperty;
        } catch (IOException e) {
            LOG.error("Failed to generate CSR [unable to load csr config data]", e);
            return new Properties();
        }
    }


//    private Properties loadCsrConfigFile(String csrConfigFileName) {
//        LOG.debug("load csr configuration file ");
//        Properties csrInputDataProperty = new Properties();
//
//        try {
//            try (InputStream input = new FileInputStream(csrConfigFileName)) {
//                csrInputDataProperty.load(new InputStreamReader(input, "UTF-8"));
//            }
//
//            return csrInputDataProperty;
//        } catch (FileNotFoundException var8) {
//            LOG.error("failed to generate csr [csr config file is not found] ");
//            return new Properties();
//        } catch (IOException var9) {
//            LOG.error("failed to generate csr [unable to load csr config data] ");
//            return new Properties();
//        }
//    }

    private CsrGeneratorDto mappingCsrInputData(Properties csrInputDataProperty) {
        return new CsrGeneratorDto(csrInputDataProperty.get("csr.common.name"), csrInputDataProperty.get("csr.serial.number"), csrInputDataProperty.get("csr.organization.identifier"), csrInputDataProperty.get("csr.organization.unit.name"), csrInputDataProperty.get("csr.organization.name"), csrInputDataProperty.get("csr.country.name"), csrInputDataProperty.get("csr.invoice.type"), csrInputDataProperty.get("csr.location.address"), csrInputDataProperty.get("csr.industry.business.category"));
    }
}
