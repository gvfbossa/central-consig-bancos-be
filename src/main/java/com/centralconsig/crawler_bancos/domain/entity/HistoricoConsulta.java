package com.centralconsig.crawler_bancos.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "historico_consulta")
@Getter
@Setter
public class HistoricoConsulta {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String margemCredito;
    private String autorizacaoCredito;
    private String situacaoCredito;

    private String margemBeneficio;
    private String autorizacaoBeneficio;
    private String situacaoBeneficio;

    private LocalDate dataConsulta;

    @ManyToOne
    @JoinColumn(name = "vinculo_id")
    private Vinculo vinculo;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        HistoricoConsulta that = (HistoricoConsulta) o;
        return Objects.equals(margemCredito, that.margemCredito) && Objects.equals(autorizacaoCredito, that.autorizacaoCredito) && Objects.equals(situacaoCredito, that.situacaoCredito) && Objects.equals(margemBeneficio, that.margemBeneficio) && Objects.equals(autorizacaoBeneficio, that.autorizacaoBeneficio) && Objects.equals(situacaoBeneficio, that.situacaoBeneficio) && Objects.equals(dataConsulta, that.dataConsulta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(margemCredito, autorizacaoCredito, situacaoCredito, margemBeneficio, autorizacaoBeneficio, situacaoBeneficio, dataConsulta);
    }
}
