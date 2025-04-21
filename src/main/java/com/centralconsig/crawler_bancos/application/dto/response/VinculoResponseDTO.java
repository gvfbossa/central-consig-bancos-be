package com.centralconsig.crawler_bancos.application.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class VinculoResponseDTO {

    private String tipoVinculo;
    private String orgao;
    private String matriculaPensionista;
    private String matriculaInstituidor;
    private List<HistoricoConsultaResponseDTO> historico;

}
