//package com.nuevo.zatca.controller;
//
//import com.nuevo.zatca.service.FatooraCliService;
//import com.nuevo.zatca.utils.FileUtils;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.io.IOException;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/csr")
//public class CSRGeneratorController {
//
////    TODO: Add the properties file as multipartformdata and download the file in respective folder and get csr from that
//    @PostMapping("/generateCsr")
//    public Map<String, String> generateCsr(@RequestBody Map<String, Object> requestBody) throws IOException {
//        FatooraCliService fatooraCliService = new FatooraCliService();
//        String csrContent = fatooraCliService.fatooraGenerateCsrForFile(requestBody.get("filePathWithFileName").toString());
//        return Map.of("csr", csrContent);
//    }
//}
