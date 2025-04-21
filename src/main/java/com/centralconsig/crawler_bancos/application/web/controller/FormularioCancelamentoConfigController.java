package com.centralconsig.crawler_bancos.application.web.controller;

import com.centralconsig.crawler_bancos.application.service.crawler.FormularioCancelamentoConfigService;
import com.centralconsig.crawler_bancos.domain.entity.FormularioCancelamentoConfig;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/form-cancelamento/config")
public class FormularioCancelamentoConfigController {

    private final FormularioCancelamentoConfigService service;

    public FormularioCancelamentoConfigController(FormularioCancelamentoConfigService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<?> getConfig() {
        FormularioCancelamentoConfig config = service.getConfig();
        return ResponseEntity.ok(config);
    }

    @PutMapping
    public ResponseEntity<?> atualizarConfig(@RequestBody FormularioCancelamentoConfig config) {
        service.atualizarConfig(config);
        return ResponseEntity.ok(HttpStatus.NO_CONTENT);
    }

}
