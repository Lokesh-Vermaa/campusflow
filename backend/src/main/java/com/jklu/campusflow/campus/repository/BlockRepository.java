package com.jklu.campusflow.campus.repository;

import com.jklu.campusflow.campus.model.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlockRepository extends JpaRepository<Block, UUID> {
    Optional<Block> findByCode(String code);
    Optional<Block> findByName(String name);
}