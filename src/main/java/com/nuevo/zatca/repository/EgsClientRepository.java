package com.nuevo.zatca.repository;

import com.nuevo.zatca.entity.EgsClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EgsClientRepository extends JpaRepository<EgsClientEntity, String> {

    EgsClientEntity findByEgsClientName(String egsClientName);
}
