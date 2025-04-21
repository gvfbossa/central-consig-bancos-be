package com.centralconsig.crawler_bancos.application.web.controller;

import com.centralconsig.crawler_bancos.application.service.crawler.GoogleSheetsExtractorService;
import com.centralconsig.crawler_bancos.domain.entity.GoogleSheet;
import com.centralconsig.crawler_bancos.domain.repository.GoogleSheetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sheets")
public class GoogleSheetsController {

    private final GoogleSheetsExtractorService sheetExtractorService;
    private final GoogleSheetRepository sheetRepository;

    public GoogleSheetsController(GoogleSheetsExtractorService sheetExtractorService, GoogleSheetRepository sheetRepository) {
        this.sheetExtractorService = sheetExtractorService;
        this.sheetRepository = sheetRepository;
    }

    @PostMapping("/download")
    public ResponseEntity<?> downloadSheet() {
        try {
            sheetExtractorService.startDownload();
            return ResponseEntity.ok("Download efetuado com sucesso!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<?> getSheets() {
        return ResponseEntity.ok(sheetRepository.findAll());
    }

    @DeleteMapping("/nome")
    public void removeSheets(@RequestParam String nome) {
        sheetRepository.delete(sheetRepository.findByFileName(nome));
    }

    @PostMapping("/nome")
    public ResponseEntity<?> saveSheets(@RequestBody GoogleSheet sheet) {
        return ResponseEntity.ok(sheetRepository.save(sheet));
    }

}
