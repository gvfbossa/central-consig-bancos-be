package com.centralconsig.crawler_bancos.domain.repository;

import com.centralconsig.crawler_bancos.domain.entity.GoogleSheet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoogleSheetRepository extends JpaRepository<GoogleSheet, Long> {
    GoogleSheet findByFileName(String nome);
}