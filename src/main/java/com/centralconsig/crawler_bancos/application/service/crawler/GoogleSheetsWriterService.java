package com.centralconsig.crawler_bancos.application.service.crawler;

import com.centralconsig.crawler_bancos.application.service.ClienteService;
import com.centralconsig.crawler_bancos.domain.entity.Cliente;
import com.centralconsig.crawler_bancos.domain.entity.GoogleSheet;
import com.centralconsig.crawler_bancos.domain.entity.HistoricoConsulta;
import com.centralconsig.crawler_bancos.domain.entity.Vinculo;
import com.centralconsig.crawler_bancos.domain.repository.GoogleSheetRepository;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class GoogleSheetsWriterService {

    private static final String APPLICATION_NAME = "CrawlerConsig";
    private static final String SPREADSHEET_ID = "1s9y_R5RJS3hOhoMpIQYwNXKcsdnKYCg5BeCUtyvvdfk";

    private final Sheets sheetsService;
    private final ClienteService clienteService;
    private final GoogleSheetRepository googleSheetRepository;

    private static final Logger log = LoggerFactory.getLogger(GoogleSheetsWriterService.class);

    public GoogleSheetsWriterService(ClienteService clienteService, GoogleSheetRepository googleSheetRepository) throws Exception {
        this.clienteService = clienteService;
        this.googleSheetRepository = googleSheetRepository;

        InputStream credentialsStream = getClass().getClassLoader()
                .getResourceAsStream("google/centralconsig-crawler-sheets-a3cdc937f506.json");

        assert credentialsStream != null;
        var credentials = ServiceAccountCredentials.fromStream(credentialsStream)
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/spreadsheets"));

        this.sheetsService = new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials)
        ).setApplicationName(APPLICATION_NAME).build();
    }

    @Scheduled(cron = "0 0 12 * * MON-FRI")
    public void appendToSheetBatch() throws Exception {
        List<Cliente> clientes = clienteService.getAllClientes().stream()
                .filter(cliente -> cliente.getVinculos().stream()
                        .anyMatch(vinculo -> vinculo.getHistoricos().stream()
                                .anyMatch(historico -> historico.getDataConsulta().equals(LocalDate.now()))
                        )
                )
            .toList();

        List<GoogleSheet> sheets = googleSheetRepository.findAll();

        for (GoogleSheet sheet : sheets) {
            String abaNome = sheet.getFileName();
            String rangeCompleto = abaNome + "!A1:Z";

            ValueRange response = sheetsService.spreadsheets().values()
                    .get(SPREADSHEET_ID, rangeCompleto)
                    .execute();

            List<List<Object>> linhas = response.getValues();
            if (linhas == null || linhas.isEmpty()) continue;

            List<Object> cabecalho = linhas.getFirst();
            int colunaCpf = encontrarIndiceColuna("cpf",cabecalho);
            int colunaMatricula = encontrarIndiceColuna("Matrícula", cabecalho);
            int colunaOrgao = encontrarIndiceColuna("Órgão", cabecalho);

            if (colunaCpf == -1 || colunaMatricula == -1 || colunaOrgao == -1) continue;

            List<ValueRange> updates = new ArrayList<>();

            for (Cliente cliente : clientes) {
                for (Vinculo vinculo : cliente.getVinculos()) {
                    Optional<HistoricoConsulta> historicoOpt = vinculo.getHistoricos().stream()
                            .filter(h -> h.getDataConsulta().equals(LocalDate.now()))
                            .max(Comparator.comparing(HistoricoConsulta::getDataConsulta));

                    if (historicoOpt.isEmpty()) continue;

                    HistoricoConsulta historico = historicoOpt.get();

                    Optional<List<ValueRange>> updatesVinculo = montarAtualizacoesBatch(
                            abaNome, cliente, vinculo, historico, linhas, colunaCpf, colunaMatricula, colunaOrgao
                    );

                    updatesVinculo.ifPresent(updates::addAll);
                }
            }

            if (!updates.isEmpty()) {
                BatchUpdateValuesRequest body = new BatchUpdateValuesRequest()
                        .setValueInputOption("USER_ENTERED")
                        .setData(updates);

                sheetsService.spreadsheets().values()
                        .batchUpdate(SPREADSHEET_ID, body)
                        .execute();

                log.info("Batch enviado com sucesso para aba " + abaNome + " com " + updates.size() + " updates.");
            }
        }
    }

    private Optional<List<ValueRange>> montarAtualizacoesBatch(String aba, Cliente cliente, Vinculo vinculo,
                                           HistoricoConsulta historico, List<List<Object>> linhas,
                                           int colunaCpf, int colunaMatricula, int colunaOrgao) throws Exception {

        int linhaIndex = encontrarLinhaComMatchCompleto(linhas, cliente.getCpf(),
                vinculo.getMatriculaPensionista(), vinculo.getOrgao(),
                colunaCpf, colunaMatricula, colunaOrgao);

        if (linhaIndex == -1) {
            log.error("Dados não encontrados na aba: CPF=" + cliente.getCpf()
                    + ", Matricula=" + vinculo.getMatriculaPensionista()
                    + ", Orgao=" + vinculo.getOrgao());
            return Optional.empty();
        }

        String dataConsultaFormatada = formatarDataParaCabecalho(historico.getDataConsulta());
        int indiceNovaColuna = inserirColunasComTitulos(aba, dataConsultaFormatada);

        int linhaPlanilha = linhaIndex + 1;

        List<ValueRange> updates = new ArrayList<>();
        updates.add(new ValueRange()
                .setRange(aba + "!" + getColunaLetra(indiceNovaColuna - 2) + linhaPlanilha)
                .setValues(List.of(List.of(historico.getSituacaoCredito()))));

        updates.add(new ValueRange()
                .setRange(aba + "!" + getColunaLetra(indiceNovaColuna - 1) + linhaPlanilha)
                .setValues(List.of(List.of(historico.getSituacaoBeneficio()))));

        updates.add(new ValueRange()
                .setRange(aba + "!" + getColunaLetra(indiceNovaColuna) + linhaPlanilha)
                .setValues(List.of(List.of(historico.getMargemCredito()))));

        updates.add(new ValueRange()
                .setRange(aba + "!" + getColunaLetra(indiceNovaColuna + 1) + linhaPlanilha)
                .setValues(List.of(List.of(historico.getMargemBeneficio()))));

        List<Request> requests = new ArrayList<>();

        if (Double.parseDouble(historico.getMargemCredito()) > 50) {
            requests.add(criarHighlightRequest(aba, linhaIndex, indiceNovaColuna));
        }

        if (Double.parseDouble(historico.getMargemBeneficio()) > 50) {
            requests.add(criarHighlightRequest(aba, linhaIndex, indiceNovaColuna + 1));
        }

        if (!requests.isEmpty()) {
            var batchUpdateRequest = new BatchUpdateSpreadsheetRequest().setRequests(requests);
            sheetsService.spreadsheets().batchUpdate(SPREADSHEET_ID, batchUpdateRequest).execute();
        }

        return Optional.of(updates);
    }

    private Request criarHighlightRequest(String aba, int linhaIndex, int colunaIndex) throws Exception {
        return new Request()
                .setRepeatCell(new RepeatCellRequest()
                        .setRange(new GridRange()
                                .setSheetId(obterSheetId(aba))
                                .setStartRowIndex(linhaIndex)
                                .setEndRowIndex(linhaIndex + 1)
                                .setStartColumnIndex(colunaIndex)
                                .setEndColumnIndex(colunaIndex + 1)
                        )
                        .setCell(new CellData()
                                .setUserEnteredFormat(new CellFormat()
                                        .setBackgroundColor(new Color()
                                                .setRed(1f)
                                                .setGreen(1f)
                                                .setBlue(0f)
                                        )
                                )
                        )
                        .setFields("userEnteredFormat.backgroundColor")
                );
    }

    private int encontrarLinhaComMatchCompleto(List<List<Object>> linhas, String cpf, String matricula, String orgao,
                                               int idxCpf, int idxMatricula, int idxOrgao) {
        for (int i = 1; i < linhas.size(); i++) {
            List<Object> linha = linhas.get(i);

            String cpfLinha = getValorCelula(linha, idxCpf);
            String matriculaLinha = getValorCelula(linha, idxMatricula);
            String orgaoLinha = getValorCelula(linha, idxOrgao);

            if (cpf.equals(cpfLinha) && matricula.equals(matriculaLinha) && orgao.equalsIgnoreCase(orgaoLinha)) {
                return i;
            }
        }
        return -1;
    }

    private String getValorCelula(List<Object> linha, int idx) {
        if (idx >= linha.size()) return "";
        return String.valueOf(linha.get(idx)).trim();
    }

    private String formatarDataParaCabecalho(LocalDate data) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM");
        return data.format(formatter);
    }

    private int encontrarIndiceColuna(String nomeColuna, List<Object> cabecalho) {
        String nomeBusca = nomeColuna.trim().toLowerCase();
        for (int i = 0; i < cabecalho.size(); i++) {
            String valor = cabecalho.get(i).toString().trim().toLowerCase();
            if (valor.equals(nomeBusca)) {
                return i;
            }
        }
        return -1;
    }

    private Integer obterSheetId(String nomeAba) throws Exception {
        var spreadsheet = sheetsService.spreadsheets().get(SPREADSHEET_ID).execute();
        return spreadsheet.getSheets().stream()
                .filter(sheet -> sheet.getProperties().getTitle().equalsIgnoreCase(nomeAba))
                .findFirst()
                .map(sheet -> sheet.getProperties().getSheetId())
                .orElseThrow(() -> new IllegalArgumentException("Aba não encontrada: " + nomeAba));
    }

    private int inserirColunasComTitulos(String aba, String dataConsulta) throws Exception {
        int sheetId = obterSheetId(aba);

        String tituloConsig = "C Consig " + dataConsulta;
        String tituloBenef = "C Benef " + dataConsulta;
        String baseTitulo = "Cartao Benef";

        // Busca os headers
        String rangeCabecalho = aba + "!1:1";
        List<Object> cabecalho = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, rangeCabecalho)
                .execute()
                .getValues()
                .getFirst();

        if (cabecalho.contains(tituloConsig))
            return cabecalho.indexOf(tituloConsig);

        int indexBase = -1;
        for (int i = 0; i < cabecalho.size(); i++) {
            if (cabecalho.get(i).toString().toLowerCase().contains(baseTitulo.toLowerCase())) {
                indexBase = i;
                break;
            }
        }

        if (indexBase == -1) {
            throw new RuntimeException("Coluna com título contendo '" + baseTitulo + "' não encontrada.");
        }

        var insertRequest = new Request().setInsertDimension(new InsertDimensionRequest()
                .setRange(new DimensionRange()
                        .setSheetId(sheetId)
                        .setDimension("COLUMNS")
                        .setStartIndex(indexBase + 1)
                        .setEndIndex(indexBase + 3))
                .setInheritFromBefore(true));

        sheetsService.spreadsheets().batchUpdate(SPREADSHEET_ID,
                        new BatchUpdateSpreadsheetRequest().setRequests(List.of(insertRequest)))
                .execute();

        List<List<Object>> novosTitulos = List.of(List.of(tituloConsig, tituloBenef));
        String novaLinhaHeader = aba + "!" + getColunaLetra(indexBase + 1) + "1";

        sheetsService.spreadsheets().values()
                .update(SPREADSHEET_ID, novaLinhaHeader, new ValueRange().setValues(novosTitulos))
                .setValueInputOption("USER_ENTERED")
                .execute();

        return indexBase + 1;
    }

    private String getColunaLetra(int index) {
        StringBuilder sb = new StringBuilder();
        index++;
        while (index > 0) {
            int rem = (index - 1) % 26;
            sb.insert(0, (char) (rem + 'A'));
            index = (index - 1) / 26;
        }
        return sb.toString();
    }

}
