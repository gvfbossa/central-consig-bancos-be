package com.centralconsig.crawler_bancos.domain.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class DadosBancarios {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    @JsonBackReference
    private Cliente cliente;

    private String uf;
    private String banco;
    private String agencia;
    @Column(name = "digito_agencia")
    private String digitoAgencia;
    private String conta;
    @Column(name = "digito_conta")
    private String digitoConta;

}
