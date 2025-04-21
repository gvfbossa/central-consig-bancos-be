package com.centralconsig.crawler_bancos.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "form_cancelamento_config")
public class FormularioCancelamentoConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String motivoCancelamento;
    private String promotora;

}