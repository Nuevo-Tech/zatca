package com.nuevo.zatca.repository;

import com.nuevo.zatca.entity.TenantUsersEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantUsersRepository extends JpaRepository<TenantUsersEntity, String> {
}
