package com.centralconsig.crawler_bancos.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "proposta")
@Getter
@Setter
public class Proposta {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "numero_proposta")
    private String numeroProposta;

    @Column(name = "link_assinatura")
    private String linkAssinatura;

    @Column(name = "valor_liberado")
    private BigDecimal valorLiberado;

    @Column(name = "valor_parcela")
    private BigDecimal valorParcela;

    @Column(name = "data_cadastro")
    private LocalDate dataCadastro;

}
