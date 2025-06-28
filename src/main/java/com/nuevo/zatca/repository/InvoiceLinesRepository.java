package com.nuevo.zatca.repository;

import com.nuevo.zatca.entity.InvoiceLinesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceLinesRepository extends JpaRepository<InvoiceLinesEntity, String> {
}
