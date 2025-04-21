package com.centralconsig.crawler_bancos.application.mapper;

import com.centralconsig.crawler_bancos.application.dto.response.PropostaResponseDTO;
import com.centralconsig.crawler_bancos.domain.entity.Proposta;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropostaMapper {

    public static PropostaResponseDTO toDto(Proposta proposta) {
        PropostaResponseDTO dto = new PropostaResponseDTO();
        dto.setCliente(ClienteMapper.toDto(proposta.getCliente()));
        dto.setNumeroProposta(proposta.getNumeroProposta());
        dto.setDataCadastro(proposta.getDataCadastro());
        dto.setValorParcela(proposta.getValorParcela());
        dto.setValorLiberado(proposta.getValorLiberado());
        dto.setLinkAssinatura(proposta.getLinkAssinatura());

        return dto;
    }

}
