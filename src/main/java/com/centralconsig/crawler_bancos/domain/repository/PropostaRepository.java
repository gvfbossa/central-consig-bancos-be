package com.centralconsig.crawler_bancos.domain.repository;

import com.centralconsig.crawler_bancos.domain.entity.Cliente;
import com.centralconsig.crawler_bancos.domain.entity.Proposta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PropostaRepository extends JpaRepository<Proposta, Long> {
    Optional<Proposta> findByNumeroProposta(String numeroProposta);

    Optional<Proposta> findByClienteAndDataCadastro(Cliente cliente, LocalDate dataCadastro);

    Optional<List<Proposta>> findByCliente(Cliente cliente);
}
