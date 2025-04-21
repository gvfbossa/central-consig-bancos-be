package com.centralconsig.crawler_bancos.application.service.utils;

import com.centralconsig.crawler_bancos.domain.entity.Proposta;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExportacaoPropostaService {

    public ExportedFile gerarExcel(List<Proposta> propostas) {
        try (Workbook workbook = new XSSFWorkbook()) {
            String dataFormatada = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

            Sheet sheet = workbook.createSheet("Propostas " + dataFormatada);

            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            Row header = sheet.createRow(0);
            String[] colunas = {
                    "Nome", "CPF", "Telefone", "Número Proposta",
                    "Link Assinatura", "Valor Liberado", "Valor Parcela", "Data Cadastro"
            };

            for (int i = 0; i < colunas.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(colunas[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (Proposta proposta : propostas) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(proposta.getCliente().getNome());
                row.createCell(1).setCellValue(proposta.getCliente().getCpf());
                row.createCell(2).setCellValue(proposta.getCliente().getTelefone());
                row.createCell(3).setCellValue(proposta.getNumeroProposta());
                row.createCell(4).setCellValue(proposta.getLinkAssinatura());
                row.createCell(5).setCellValue(proposta.getValorLiberado().toString());
                row.createCell(6).setCellValue(proposta.getValorParcela().toString());
                row.createCell(7).setCellValue(proposta.getDataCadastro().toString());
            }

            for (int i = 0; i < colunas.length+1; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            String nomeArquivo = "Relatorio_Propostas_" + dataFormatada + ".xlsx";

            return new ExportedFile(out.toByteArray(), nomeArquivo);

        } catch (IOException e) {
            throw new RuntimeException("Erro ao gerar Excel", e);
        }
    }

    public static class ExportedFile {
        private final byte[] dados;
        private final String nomeArquivo;

        public ExportedFile(byte[] dados, String nomeArquivo) {
            this.dados = dados;
            this.nomeArquivo = nomeArquivo;
        }

        public byte[] getDados() {
            return dados;
        }

        public String getNomeArquivo() {
            return nomeArquivo;
        }
    }
}