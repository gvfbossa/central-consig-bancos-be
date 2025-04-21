package com.centralconsig.crawler_bancos.domain.repository;

import com.centralconsig.crawler_bancos.domain.entity.Cliente;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByCpf(String cpf);

    List<Cliente> findByCpfIn(Set<String> cpfs);

    @Query("SELECT c FROM Cliente c " +
            "LEFT JOIN c.vinculos v " +
            "LEFT JOIN v.historicos h " +
            "WHERE c.casa = true")
    List<Cliente> buscarClientesCasaComVinculosEHistoricos();


    @Query("SELECT c FROM Cliente c " +
            "LEFT JOIN c.vinculos v " +
            "LEFT JOIN v.historicos h " +
            "WHERE c.casa = false")
    List<Cliente> buscarClientesNaoCasaComVinculosEHistorico();

}

