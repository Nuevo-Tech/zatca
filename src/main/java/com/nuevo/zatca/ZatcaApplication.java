package com.nuevo.zatca;

import com.nuevo.zatca.service.FatooraCliService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ZatcaApplication {

    public static void main(String[] args) throws Exception {

        SpringApplication.run(ZatcaApplication.class, args);
        FatooraCliService fatooraCliService = new FatooraCliService();
//        System.out.println(fatooraCliService.fatooraGenerateCsrForFile("csr-config-template.properties"));

//        System.out.println(fatooraCliService.fatooraGenerateInvoiceHash("src/resources/xmlinvoices/Standard_Invoice.xml"));
//        System.out.println("qrcode :"+fatooraCliService.fatooraGenerateQrForInvoice("Standard_Invoice.xml"));

//        System.out.println(fatooraCliService.fatooraGenerateInvoiceRequest("Standard_Invoice.xml"));
//        System.out.println(fatooraCliService.fatooraValidateInvoice("Standard_Invoice.xml"));
//        System.out.println(fatooraCliService.fatooraSignInvoice("Standard_Invoice.xml"));;

    }
}
