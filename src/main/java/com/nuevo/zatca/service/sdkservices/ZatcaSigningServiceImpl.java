package com.nuevo.zatca.service.sdkservices;

import com.gazt.einvoicing.digitalsignature.service.DigitalSignatureService;
import com.gazt.einvoicing.digitalsignature.service.model.DigitalSignature;
import com.gazt.einvoicing.hashing.generation.service.HashingGenerationService;
import com.gazt.einvoicing.hashing.generation.service.impl.HashingGenerationServiceImpl;
import com.gazt.einvoicing.qr.generation.service.QRCodeGeneratorService;
import com.gazt.einvoicing.qr.generation.service.impl.QRCodeGeneratorServiceImpl;
import com.gazt.einvoicing.signing.service.impl.SigningServiceImpl;
import com.gazt.einvoicing.signing.service.model.InvoiceSigningResult;
import net.sf.saxon.TransformerFactoryImpl;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.dom4j.*;
import org.dom4j.io.SAXReader;
import org.springframework.core.io.ClassPathResource;
import org.xml.sax.SAXException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ZatcaSigningServiceImpl extends SigningServiceImpl {

    static final Logger LOGGER = Logger.getLogger(ZatcaSigningServiceImpl.class.getName());
    static final TransformerFactory transformerFactory = new TransformerFactoryImpl();
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private QRCodeGeneratorService qrCodeGeneratorService = new QRCodeGeneratorServiceImpl();

    private HashingGenerationService hashingGenerationServiceImpl = new HashingGenerationServiceImpl();
    private DigitalSignatureService zatcaDigitalSignatureService = new ZatcaDigitalSignatureServiceImpl();

    public InvoiceSigningResult signDocument(String xmlDocument, PrivateKey privateKey, String certificateAsString, String password) throws IOException, TransformerException, DocumentException, SAXException, NoSuchAlgorithmException, CertificateException {
        if (!xmlDocument.contains("xmlns:ext=\"urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2\"")) {
            xmlDocument = xmlDocument.replace("xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\"", "xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\" xmlns:ext=\"urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2\"");
        }

        String invoiceHash = null;

        try {
            invoiceHash = this.hashingGenerationServiceImpl.getInvoiceHash(xmlDocument);
        } catch (Exception e) {
            throw new NullPointerException("unable to generate hash for the provided invoice xml document - " + e.getMessage());
        }

        InvoiceSigningResult invoiceSigningResult = new InvoiceSigningResult();
        invoiceSigningResult.setInvoiceHash(invoiceHash);
        Security.addProvider(new BouncyCastleProvider());
        byte[] certificateBytes = certificateAsString.getBytes(StandardCharsets.UTF_8);

        String certificateCopy;
        X509Certificate certificate;
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(Base64.getDecoder().decode(certificateBytes))) {
            byte[] certificateBytesCopy = Arrays.copyOf(certificateBytes, certificateBytes.length);
            certificateCopy = new String(certificateBytesCopy);
            CertificateFactory certificatefactory = CertificateFactory.getInstance("X.509");
            certificate = (X509Certificate)certificatefactory.generateCertificate(byteArrayInputStream);
        } catch (Exception var21) {
            throw new DocumentException("unable to decode the provided invoice xml document");
        }

        DigitalSignature digitalSignature = null;

        try {
            digitalSignature = this.zatcaDigitalSignatureService.getDigitalSignature(xmlDocument, privateKey, invoiceHash);
        } catch (Exception e) {
            throw new DocumentException("unable to sign the provided invoice xml document - " + e.getMessage());
        }

        xmlDocument = this.transformXML(xmlDocument);
        Document document = this.getXmlDocument(xmlDocument);
        Map<String, String> nameSpacesMap = this.getNameSpacesMap();
        String qrCode = this.getNodeXmlValue(document, nameSpacesMap, "/Invoice/cac:AdditionalDocumentReference[cbc:ID='QR']/cac:Attachment/cbc:EmbeddedDocumentBinaryObject");
        invoiceSigningResult.setIncludesQRCodeAlready(StringUtils.isNotBlank(qrCode));
        String certificateHashing = this.encodeBase64(this.bytesToHex(this.hashStringToBytes(certificateAsString.getBytes(StandardCharsets.UTF_8))).getBytes(StandardCharsets.UTF_8));
        String signedPropertiesHashing = this.populateSignedSignatureProperties(document, nameSpacesMap, certificateHashing, this.getCurrentTimestamp(), certificate.getIssuerDN().getName(), certificate.getSerialNumber().toString());
        this.populateUBLExtensions(document, nameSpacesMap, digitalSignature.getDigitalSignature(), signedPropertiesHashing, this.encodeBase64(digitalSignature.getXmlHashing()), certificateCopy);

        try {
            qrCode = this.populateQRCode(document, nameSpacesMap, certificate, digitalSignature.getDigitalSignature(), invoiceHash);
        } catch (Exception e) {
            throw new DocumentException("unable to generate qr code for the provided invoice xml document - " + e.getMessage());
        }

        invoiceSigningResult.setQrCode(qrCode);
        invoiceSigningResult.setSingedXML(document.asXML());
        return invoiceSigningResult;
    }

    private Map<String, String> getNameSpacesMap() {
        Map<String, String> nameSpaces = new HashMap();
        nameSpaces.put("cac", "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2");
        nameSpaces.put("cbc", "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2");
        nameSpaces.put("ext", "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2");
        nameSpaces.put("sig", "urn:oasis:names:specification:ubl:schema:xsd:CommonSignatureComponents-2");
        nameSpaces.put("sac", "urn:oasis:names:specification:ubl:schema:xsd:SignatureAggregateComponents-2");
        nameSpaces.put("sbc", "urn:oasis:names:specification:ubl:schema:xsd:SignatureBasicComponents-2");
        nameSpaces.put("ds", "http://www.w3.org/2000/09/xmldsig#");
        nameSpaces.put("xades", "http://uri.etsi.org/01903/v1.3.2#");
        return nameSpaces;
    }

    private byte[] hashStringToBytes(byte[] toBeHashed) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(toBeHashed);
    }

    String encodeBase64(byte[] stringTobBeEncoded) {
        return Base64.getEncoder().encodeToString(stringTobBeEncoded);
    }

    private String populateQRCode(Document document, Map<String, String> nameSpacesMap, X509Certificate certificate, String signature, String hashedXml) throws Exception {
        String sellerName = this.getNodeXmlTextValue(document, nameSpacesMap, "/Invoice/cac:AccountingSupplierParty/cac:Party/cac:PartyLegalEntity/cbc:RegistrationName");
        String vatRegistrationNumber = this.getNodeXmlTextValue(document, nameSpacesMap, "/Invoice/cac:AccountingSupplierParty/cac:Party/cac:PartyTaxScheme/cbc:CompanyID");
        this.getNodeXmlTextValue(document, nameSpacesMap, "/Invoice/cac:LegalMonetaryTotal/cbc:TaxInclusiveAmount");
        String payableAmount = this.getNodeXmlTextValue(document, nameSpacesMap, "/Invoice/cac:LegalMonetaryTotal/cbc:PayableAmount");
        String vatTotal = this.getNodeXmlTextValue(document, nameSpacesMap, "/Invoice/cac:TaxTotal/cbc:TaxAmount");
        String issueDate = this.getNodeXmlTextValue(document, nameSpacesMap, "/Invoice/cbc:IssueDate");
        String issueTime = this.getNodeXmlTextValue(document, nameSpacesMap, "/Invoice/cbc:IssueTime");
        String timeStamp;
        if (issueTime.endsWith("Z")) {
            issueTime = issueTime.replace("Z", "");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            Date dateTimeFormat = sdf.parse(issueDate + "T" + issueTime);
            SimpleDateFormat ksaSdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT + 3"));
            timeStamp = ksaSdf.format(dateTimeFormat);
        } else {
            String stringDateTime = issueDate + "T" + issueTime;
            LocalDateTime dateTimeFormat = LocalDateTime.parse(stringDateTime);
            timeStamp = this.dateTimeFormatter.format(dateTimeFormat);
        }

        String invoiceType = this.getNodeXmlTextValue(document, nameSpacesMap, "/Invoice/cbc:InvoiceTypeCode/@name");
        String qrCode = this.qrCodeGeneratorService.generateQrCode(sellerName, vatRegistrationNumber, timeStamp, payableAmount, vatTotal, hashedXml, certificate.getPublicKey().getEncoded(), signature, invoiceType.startsWith("02"), certificate.getSignature());
        this.populateXmlAttributeValue(document, nameSpacesMap, "/Invoice/cac:AdditionalDocumentReference[cbc:ID='QR']/cac:Attachment/cbc:EmbeddedDocumentBinaryObject", qrCode);
        return qrCode;
    }

    private String transformXML(String xmlDocument) throws IOException, TransformerException {
        xmlDocument = this.transformXML(xmlDocument, "removeElements.xsl");
        xmlDocument = this.transformXML(xmlDocument, "addUBLElement.xsl");
        xmlDocument = xmlDocument.replace("UBL-TO-BE-REPLACED", this.getElementFromFile("ubl.xml"));
        xmlDocument = this.transformXML(xmlDocument, "addQRElement.xsl");
        xmlDocument = xmlDocument.replace("QR-TO-BE-REPLACED", this.getElementFromFile("qr.xml"));
        xmlDocument = this.transformXML(xmlDocument, "addSignatureElement.xsl");
        xmlDocument = xmlDocument.replace("SIGN-TO-BE-REPLACED", this.getElementFromFile("signature.xml"));
        return xmlDocument;
    }

    private void populateUBLExtensions(Document document, Map<String, String> nameSpacesMap, String digitalSignature, String signedPropertiesHashing, String xmlHashing, String certificate) {
        this.populateXmlAttributeValue(document, nameSpacesMap, "/Invoice/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/sig:UBLDocumentSignatures/sac:SignatureInformation/ds:Signature/ds:SignatureValue", digitalSignature);
        this.populateXmlAttributeValue(document, nameSpacesMap, "/Invoice/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/sig:UBLDocumentSignatures/sac:SignatureInformation/ds:Signature/ds:KeyInfo/ds:X509Data/ds:X509Certificate", certificate);
        this.populateXmlAttributeValue(document, nameSpacesMap, "/Invoice/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/sig:UBLDocumentSignatures/sac:SignatureInformation/ds:Signature/ds:SignedInfo/ds:Reference[@URI='#xadesSignedProperties']/ds:DigestValue", signedPropertiesHashing);
        this.populateXmlAttributeValue(document, nameSpacesMap, "/Invoice/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/sig:UBLDocumentSignatures/sac:SignatureInformation/ds:Signature/ds:SignedInfo/ds:Reference[@Id='invoiceSignedData']/ds:DigestValue", xmlHashing);
    }

    private String populateSignedSignatureProperties(Document document, Map<String, String> nameSpacesMap, String publicKeyHashing, String signatureTimestamp, String x509IssuerName, String serialNumber) throws NoSuchAlgorithmException {
        this.populateXmlAttributeValue(document, nameSpacesMap, "/Invoice/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/sig:UBLDocumentSignatures/sac:SignatureInformation/ds:Signature/ds:Object/xades:QualifyingProperties/xades:SignedProperties/xades:SignedSignatureProperties/xades:SigningCertificate/xades:Cert/xades:CertDigest/ds:DigestValue", publicKeyHashing);
        this.populateXmlAttributeValue(document, nameSpacesMap, "/Invoice/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/sig:UBLDocumentSignatures/sac:SignatureInformation/ds:Signature/ds:Object/xades:QualifyingProperties/xades:SignedProperties/xades:SignedSignatureProperties/xades:SigningTime", signatureTimestamp);
        this.populateXmlAttributeValue(document, nameSpacesMap, "/Invoice/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/sig:UBLDocumentSignatures/sac:SignatureInformation/ds:Signature/ds:Object/xades:QualifyingProperties/xades:SignedProperties/xades:SignedSignatureProperties/xades:SigningCertificate/xades:Cert/xades:IssuerSerial/ds:X509IssuerName", x509IssuerName);
        this.populateXmlAttributeValue(document, nameSpacesMap, "/Invoice/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/sig:UBLDocumentSignatures/sac:SignatureInformation/ds:Signature/ds:Object/xades:QualifyingProperties/xades:SignedProperties/xades:SignedSignatureProperties/xades:SigningCertificate/xades:Cert/xades:IssuerSerial/ds:X509SerialNumber", serialNumber);
        String signedSignatureElement = this.getNodeXmlValue(document, nameSpacesMap, "/Invoice/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/sig:UBLDocumentSignatures/sac:SignatureInformation/ds:Signature/ds:Object/xades:QualifyingProperties/xades:SignedProperties");
        return signedSignatureElement != null ? this.encodeBase64(this.bytesToHex(this.hashStringToBytes(signedSignatureElement.getBytes(StandardCharsets.UTF_8))).getBytes(StandardCharsets.UTF_8)) : null;
    }

    private Document populateXmlAttributeValue(Document document, Map<String, String> nameSpaces, String attributeXpath, String newValue) {
        XPath xpath = DocumentHelper.createXPath(attributeXpath);
        xpath.setNamespaceURIs(nameSpaces);
        List<Node> nodes = xpath.selectNodes(document);
        IntStream.range(0, nodes.size()).mapToObj((i) -> (Element)nodes.get(i)).forEach((element) -> element.setText(newValue));
        return document;
    }

    private String getNodeXmlValue(Document document, Map<String, String> nameSpaces, String attributeXpath) {
        XPath xpath = DocumentHelper.createXPath(attributeXpath);
        xpath.setNamespaceURIs(nameSpaces);
        Node node = xpath.selectSingleNode(document);
        return node != null ? node.asXML() : null;
    }

    private String getNodeXmlTextValue(Document document, Map<String, String> nameSpaces, String attributeXpath) {
        XPath xpath = DocumentHelper.createXPath(attributeXpath);
        xpath.setNamespaceURIs(nameSpaces);
        return xpath.selectSingleNode(document).getText();
    }

    private String getCurrentTimestamp() {
        LocalDateTime localDateTime = LocalDateTime.now();
        return this.dateTimeFormatter.format(localDateTime);
    }

    Document getXmlDocument(String xmlDocument) throws SAXException, DocumentException, IOException {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(xmlDocument.getBytes(StandardCharsets.UTF_8))) {
            SAXReader xmlReader = new SAXReader();
            xmlReader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document doc = xmlReader.read(byteArrayInputStream);
            xmlReader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            xmlReader.setFeature("http://xml.org/sax/features/external-general-entities", false);
            xmlReader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            xmlReader.setFeature("http://xml.org/sax/features/namespaces", true);
            return doc;
        }
    }

    private String transformXML(String xmlDocument, String fileName) throws IOException, TransformerException {
        Transformer transformer = this.getXsltTransformer(fileName);

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            StreamResult xmlOutput = new StreamResult(bos);
            transformer.transform(new StreamSource(new StringReader(xmlDocument)), xmlOutput);
            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);

        for(int i = 0; i < hash.length; ++i) {
            String hex = Integer.toHexString(255 & hash[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }

            hexString.append(hex);
        }

        return hexString.toString();
    }

    private Transformer getXsltTransformer(String fileName) throws TransformerConfigurationException, IOException {
        try (InputStream inputStream = (new ClassPathResource("xslt/" + fileName)).getInputStream()) {
            Transformer transformer = transformerFactory.newTransformer(new StreamSource(inputStream));
            transformer.setOutputProperty("encoding", "UTF-8");
            transformer.setOutputProperty("indent", "no");
            return transformer;
        }
    }

    private String getElementFromFile(String fileName) throws IOException {
        String var5;
        try (
                InputStream inputStream = (new ClassPathResource("xml/" + fileName)).getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        ) {
            var5 = (String)bufferedReader.lines().collect(Collectors.joining("\n"));
        }

        return var5;
    }
}
