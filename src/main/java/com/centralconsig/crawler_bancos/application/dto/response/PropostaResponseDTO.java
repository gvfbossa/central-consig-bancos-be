package com.centralconsig.crawler_bancos.application.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class PropostaResponseDTO {

    private ClienteResponseDTO cliente;
    private String numeroProposta;
    private String linkAssinatura;
    private BigDecimal valorLiberado;
    private BigDecimal valorParcela;
    private LocalDate dataCadastro;

}
