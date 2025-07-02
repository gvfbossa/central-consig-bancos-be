package com.centralconsig.crawler_bancos.domain.repository;

import com.centralconsig.crawler_bancos.domain.entity.DadosBancarios;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DadosBancariosRepository extends JpaRepository<DadosBancarios, Long> {

    DadosBancarios findByClienteId(Long id);
}
