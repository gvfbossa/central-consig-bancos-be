package com.centralconsig.crawler_bancos.application.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioLoginQueroMaisCreditoRequestDTO {

    private String login;
    private String senha;
    private boolean somenteConsulta;

}
