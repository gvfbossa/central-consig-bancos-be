package com.centralconsig.crawler_bancos.domain.repository;

import com.centralconsig.crawler_bancos.domain.entity.HistoricoConsulta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoConsultaRepository extends JpaRepository<HistoricoConsulta, Long> {
}
