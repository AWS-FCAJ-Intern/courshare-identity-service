package com.courshare.identity.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    @Query("SELECT ur.role.name FROM UserRole ur WHERE ur.userId = :userId")
    List<String> findRoleNamesByUserId(@Param("userId") String userId);

    void deleteByUserId(String userId);
}
