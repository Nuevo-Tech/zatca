package com.nuevo.zatca.service;

import com.nuevo.zatca.repository.EgsClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EgsClientService {

    @Autowired
    private EgsClientRepository egsClientRepository;

    public String getEgsClientUniqueSerielNumber(String egsClientName) {
        long count = egsClientRepository.count(); // Count all onboarded companies
        long nextNumber = count + 1;
        return egsClientName.toLowerCase().replaceAll("\\s+", " ") + "_" + nextNumber;
    }
}
