package com.nuevo.zatca.repository;

import com.nuevo.zatca.entity.InvoicesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoicesRepository extends JpaRepository<InvoicesEntity, String> {
}
