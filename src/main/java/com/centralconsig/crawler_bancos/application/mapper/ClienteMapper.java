package com.centralconsig.crawler_bancos.application.mapper;

import com.centralconsig.crawler_bancos.application.dto.response.ClienteResponseDTO;
import com.centralconsig.crawler_bancos.application.dto.response.HistoricoConsultaResponseDTO;
import com.centralconsig.crawler_bancos.application.dto.response.VinculoResponseDTO;
import com.centralconsig.crawler_bancos.domain.entity.Cliente;

import java.util.List;
import java.util.stream.Collectors;

public class ClienteMapper {

    public static ClienteResponseDTO toDto(Cliente cliente) {
        ClienteResponseDTO dto = new ClienteResponseDTO();
        dto.setCpf(cliente.getCpf());
        dto.setNome(cliente.getNome());
        dto.setBlackList(cliente.isBlackList());

        List<VinculoResponseDTO> vinculoResponseDtos = cliente.getVinculos()
                .stream()
                .map(vinculo -> {
                    VinculoResponseDTO vinculoResponseDto = new VinculoResponseDTO();
                    vinculoResponseDto.setTipoVinculo(vinculo.getTipoVinculo());
                    vinculoResponseDto.setOrgao(vinculo.getOrgao());
                    vinculoResponseDto.setMatriculaPensionista(vinculo.getMatriculaPensionista());
                    vinculoResponseDto.setMatriculaInstituidor(vinculo.getMatriculaInstituidor());

                    List<HistoricoConsultaResponseDTO> historicoDtos = vinculo.getHistoricos()
                            .stream()
                            .map(HistoricoConsultaMapper::toDto)
                            .collect(Collectors.toList());

                    vinculoResponseDto.setHistorico(historicoDtos);

                    return vinculoResponseDto;
                })
                .collect(Collectors.toList());

        dto.setVinculos(vinculoResponseDtos);

        return dto;
    }
}
