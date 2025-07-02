package com.centralconsig.crawler_bancos.application.mapper;

import com.centralconsig.crawler_bancos.application.dto.response.UsuarioLoginQueroMaisCreditoResponseDTO;
import com.centralconsig.crawler_bancos.domain.entity.UsuarioLoginQueroMaisCredito;

public class UsuarioMapper {
    
    public static UsuarioLoginQueroMaisCreditoResponseDTO toDto(UsuarioLoginQueroMaisCredito usuarioLoginQueroMaisCredito) {
        UsuarioLoginQueroMaisCreditoResponseDTO dto = new UsuarioLoginQueroMaisCreditoResponseDTO();
        dto.setLogin(usuarioLoginQueroMaisCredito.getUsername());
        dto.setSenha(usuarioLoginQueroMaisCredito.getPassword());
        dto.setSomenteConsulta(usuarioLoginQueroMaisCredito.isSomenteConsulta());
        
        return dto;
    }

}
