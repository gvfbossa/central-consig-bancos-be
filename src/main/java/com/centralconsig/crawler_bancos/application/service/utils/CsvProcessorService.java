package com.centralconsig.crawler_bancos.application.service.utils;

import com.centralconsig.crawler_bancos.application.service.ClienteService;
import com.centralconsig.crawler_bancos.application.service.crawler.QueroMaisCreditoPropostaService;
import com.centralconsig.crawler_bancos.domain.entity.Cliente;
import com.centralconsig.crawler_bancos.domain.entity.Vinculo;

import com.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileReader;
import java.util.*;

@Service
public class CsvProcessorService {

    private final ClienteService clienteService;

    @Value("${sheet.download.dir}")
    private String CSV_DIR;

    private static final Logger log = LoggerFactory.getLogger(CsvProcessorService.class);

    public CsvProcessorService(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    public void processCpfs() {
        File dir = new File(CSV_DIR);
        File[] csvFiles = dir.listFiles((d, name) -> name.endsWith(".csv"));

        if (csvFiles == null || csvFiles.length == 0) {
            log.error("Nenhum arquivo CSV encontrado.");
            return;
        }

        Arrays.sort(csvFiles, (f1, f2) -> {
            boolean f1IsCasa = f1.getName().toUpperCase().contains("CASA");
            boolean f2IsCasa = f2.getName().toUpperCase().contains("CASA");

            if (f1IsCasa && !f2IsCasa) return -1;
            if (!f1IsCasa && f2IsCasa) return 1;
            return 0;
        });

        Map<String, Cliente> clientesMap = new HashMap<>();

        for (File csvFile : csvFiles) {
            try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
                String[] header = reader.readNext();
                if (header == null) continue;

                int cpfIndex = -1;
                int matriculaIndex = -1;

                for (int i = 0; i < header.length; i++) {
                    if (header[i].equalsIgnoreCase("CPF")) cpfIndex = i;
                    if (header[i].equalsIgnoreCase("Matricula")) matriculaIndex = i;
                }

                if (cpfIndex == -1 || matriculaIndex == -1) {
                    continue;
                }

                String[] row;

                while ((row = reader.readNext()) != null) {
                    if (cpfIndex < row.length && matriculaIndex < row.length) {
                        String cpf = row[cpfIndex];
                        String matricula = row[matriculaIndex];

                        if (cpf != null && matricula != null &&
                                !cpf.trim().isEmpty() && !matricula.trim().isEmpty()) {

                            cpf = cpf.trim();
                            matricula = clienteService.removeZerosAEsquerdaMatricula(matricula);

                            if (cpf.length() == 10)
                                cpf = "0" + cpf;

                            String finalCpf = cpf;
                            Cliente cliente = clientesMap.computeIfAbsent(cpf, k -> {
                                Cliente c = new Cliente();
                                c.setCpf(finalCpf);
                                c.setCasa(csvFile.getName().toUpperCase().contains("CASA"));
                                return c;
                            });

                            Vinculo vinculo = new Vinculo();
                            vinculo.setMatriculaPensionista(matricula);
                            vinculo.setCliente(cliente);
                            cliente.getVinculos().add(vinculo);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Erro ao processar arquivo " + csvFile.getName() + ": " + e.getMessage());
            }
        }
        clienteService.salvarOuAtualizarEmLote(new ArrayList<>(clientesMap.values()));
    }

}
