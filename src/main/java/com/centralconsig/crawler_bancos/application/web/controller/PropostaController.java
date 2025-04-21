package com.centralconsig.crawler_bancos.application.web.controller;

import com.centralconsig.crawler_bancos.application.dto.response.PropostaResponseDTO;
import com.centralconsig.crawler_bancos.application.mapper.PropostaMapper;
import com.centralconsig.crawler_bancos.application.service.crawler.FormularioCancelamentoPropostaService;
import com.centralconsig.crawler_bancos.application.service.PropostaService;
import com.centralconsig.crawler_bancos.application.service.utils.ExportacaoPropostaService;
import com.centralconsig.crawler_bancos.domain.entity.Proposta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/proposta")
public class PropostaController {

    private final PropostaService propostaService;
    private final FormularioCancelamentoPropostaService cancelamentoPropostaService;
    private final ExportacaoPropostaService exportacaoPropostaService;

    public PropostaController(PropostaService propostaService, FormularioCancelamentoPropostaService cancelamentoPropostaService,
                              ExportacaoPropostaService exportacaoPropostaService) {
        this.propostaService = propostaService;
        this.cancelamentoPropostaService = cancelamentoPropostaService;
        this.exportacaoPropostaService = exportacaoPropostaService;
    }

    @GetMapping
    public ResponseEntity<?> getAllPropostas() {
        int page = 0;
        int size = 100;

        Pageable pageable = PageRequest.of(page, size);
        Page<Proposta> propostasPage = propostaService.getAllPropostas(pageable);

        List<PropostaResponseDTO> propostas = propostasPage.stream()
            .map(PropostaMapper::toDto)
            .toList();

        return ResponseEntity.ok(propostas);
    }

    @GetMapping("/numero")
    public ResponseEntity<Proposta> getPropostaByNumero(@RequestParam String numero) {
        return propostaService.retornaPropostaPorNumero(numero)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @DeleteMapping("/cancelamento")
    public ResponseEntity<?> cancelaPropostasByNumero(@RequestBody List<String> numeros) {
        if (cancelamentoPropostaService.cancelaPropostas(numeros))
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    @PostMapping("/excel")
    public ResponseEntity<byte[]> exportarPropostasParaExcel(@RequestBody List<String> numerosPropostas) {
        List<Proposta> propostas = propostaService.todasAsPropostas().stream()
            .filter(proposta -> numerosPropostas.contains(proposta.getNumeroProposta()))
            .toList();
        ExportacaoPropostaService.ExportedFile arquivo = exportacaoPropostaService.gerarExcel(propostas);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + arquivo.getNomeArquivo() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(arquivo.getDados());
    }

}
