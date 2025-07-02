package com.centralconsig.crawler_bancos.domain.repository;

import com.centralconsig.crawler_bancos.domain.entity.Cliente;
import com.centralconsig.crawler_bancos.domain.entity.Proposta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PropostaRepository extends JpaRepository<Proposta, Long> {
    Optional<Proposta> findByNumeroProposta(String numeroProposta);

    Optional<Proposta> findByClienteAndDataCadastro(Cliente cliente, LocalDate dataCadastro);

    Optional<List<Proposta>> findByCliente(Cliente cliente);

    @Query(value = "SELECT * FROM proposta p WHERE p.link_assinatura  IS NULL OR p.link_assinatura = '' OR p.valor_liberado IS NULL", nativeQuery = true)
    List<Proposta> getPropostasPorFaltaDeInformacao();

    @Query(value = "SELECT * FROM proposta p WHERE p.processada = false", nativeQuery = true)
    List<Proposta> getTodasAsPropostasNaoProcessadas();
}
