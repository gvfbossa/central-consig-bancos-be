package com.centralconsig.crawler_bancos.application.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class HistoricoConsultaResponseDTO {

    private String margemCredito;
    private String autorizacaoCredito;
    private String situacaoCredito;

    private String margemBeneficio;
    private String autorizacaoBeneficio;
    private String situacaoBeneficio;

    private LocalDate dataConsulta;

}
