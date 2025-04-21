package com.centralconsig.crawler_bancos.application.service.crawler;

import com.centralconsig.crawler_bancos.application.service.*;
import com.centralconsig.crawler_bancos.application.utils.CrawlerUtils;
import com.centralconsig.crawler_bancos.domain.entity.Cliente;
import com.centralconsig.crawler_bancos.domain.entity.HistoricoConsulta;
import com.centralconsig.crawler_bancos.domain.entity.UsuarioLoginQueroMaisCredito;
import com.centralconsig.crawler_bancos.domain.entity.Vinculo;
import com.google.common.util.concurrent.RateLimiter;
import org.openqa.selenium.interactions.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class QueroMaisCreditoCapturaDadosService {

    private final WebDriverService webDriverService;
    private final QueroMaisCreditoLoginService queroMaisCreditoLoginService;
    private final ClienteService clienteService;
    private final HistoricoConsultaService historicoConsultaService;
    private final UsuarioLoginQueroMaisCreditoService usuarioLoginQueroMaisCreditoService;
    private final AtomicBoolean isRunningCasa = new AtomicBoolean(false);
    private final AtomicBoolean isRunningNaoCasa = new AtomicBoolean(false);

    @Value("${base.url.quero.mais.credito}")
    private String BASE_URL;

    private static final int LIMITE_CONSULTA_DIARIA = 10_000;
    private static final int THREAD_COUNT = 9;
    private int TENTATIVAS_SEGUIDAS = 0;
    private int LIMITE_TENTATIVAS = 5;

    private static final Logger log = LoggerFactory.getLogger(QueroMaisCreditoCapturaDadosService.class);

    public QueroMaisCreditoCapturaDadosService(WebDriverService webDriverService, ClienteService clienteService, UsuarioLoginQueroMaisCreditoService usuarioLoginQueroMaisCreditoService,
                       HistoricoConsultaService historicoConsultaService, QueroMaisCreditoLoginService queroMaisCreditoLoginService) {
        this.webDriverService = webDriverService;
        this.clienteService = clienteService;
        this.historicoConsultaService = historicoConsultaService;
        this.queroMaisCreditoLoginService = queroMaisCreditoLoginService;
        this.usuarioLoginQueroMaisCreditoService = usuarioLoginQueroMaisCreditoService;
    }

//    @Scheduled(cron = "0 0 0,3,6,9,21 * * MON-FRI")
    @Scheduled(initialDelay = 1000, fixedDelay = Long.MAX_VALUE)
    public void buscaMargensCasa() {
        if (!isRunningCasa.compareAndSet(false, true)) {
            log.info("Buscar Margens Casa já em execução. Ignorando nova tentativa.");
            return;
        }
        try {
            List<Cliente> clientes = clienteService.getClientesCasaComVinculosEHistorico().stream()
                    .sorted(Comparator.comparing(this::getDataUltimaConsulta, Comparator.nullsFirst(Comparator.naturalOrder())))
                    .toList();
            capturaDadosClienteEmParalelo(clientes, 120, "casa");
        } finally {
            isRunningCasa.set(false);
            LIMITE_TENTATIVAS = 0;
        }
    }

//    @Scheduled(cron = "0 0 2,6,10,14,18,22 * * SAT,SUN")
//    @Scheduled(initialDelay = 1000, fixedDelay = Long.MAX_VALUE)
    public void buscaMargensNaoCasa() {
        if (!isRunningNaoCasa.compareAndSet(false, true)) {
            log.info("Buscar Margens Não Casa já em execução. Ignorando nova tentativa.");
            return;
        }
        try {
            List<Cliente> clientes = clienteService.getClientesNaoCasaComVinculosEHistorico().stream()
                    .sorted(Comparator.comparing(this::getDataUltimaConsulta, Comparator.nullsFirst(Comparator.naturalOrder())))
                    .limit(LIMITE_CONSULTA_DIARIA)
                    .toList();
            capturaDadosClienteEmParalelo(clientes, 1000, "nao");
        } finally {
            isRunningNaoCasa.set(false);
            LIMITE_TENTATIVAS = 0;
        }
    }

    private LocalDate getDataUltimaConsulta(Cliente cliente) {
        return cliente.getVinculos().stream()
                .flatMap(vinculo -> vinculo.getHistoricos().stream())
                .map(HistoricoConsulta::getDataConsulta)
                .max(LocalDate::compareTo)
                .orElse(null);
    }

    private void capturaDadosClienteEmParalelo(List<Cliente> clientes, int clientesPorThread, String casa) {
        List<UsuarioLoginQueroMaisCredito> usuarios = usuarioLoginQueroMaisCreditoService.retornaUsuariosParaCrawler();

        clientes = clientes.stream()
            .filter(cliente -> cliente.getVinculos().stream()
                .flatMap(vinculo -> vinculo.getHistoricos().stream())
                .noneMatch(historico -> historico.getDataConsulta().equals(LocalDate.now())))
            .toList();

        RateLimiter rateLimiter = RateLimiter.create(4.0);

        try (ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT)) {

            long inicio = System.currentTimeMillis();

            List<List<Cliente>> subListas = new ArrayList<>();
            for (int i = 0; i < clientes.size(); i += clientesPorThread) {
                subListas.add(clientes.subList(i, Math.min(i + clientesPorThread, clientes.size())));
            }

            for (int i = 0; i < subListas.size(); i++) {
                final List<Cliente> subLista = subListas.get(i);
                final UsuarioLoginQueroMaisCredito usuario = usuarios.get(i % usuarios.size());

                executor.submit(() -> processarClientes(subLista, usuario, rateLimiter, casa));
            }

            boolean terminou = executor.awaitTermination(3, TimeUnit.HOURS);

            if (!terminou) {
                executor.shutdownNow();
            }

            long fim = System.currentTimeMillis();
            long duracao = fim - inicio;
            long minutos = TimeUnit.MILLISECONDS.toMinutes(duracao);
            long segundos = TimeUnit.MILLISECONDS.toSeconds(duracao) % 60;
            log.info("Tempo total de execução para: " + casa + " - " + minutos + " min " + segundos + " s");

        } catch (Exception e) {
            log.error("Erro na execução da captura em paralelo: " + e.getMessage());
        }
    }

    private void processarClientes(List<Cliente> clientes, UsuarioLoginQueroMaisCredito usuario, RateLimiter rateLimiter, String casa) {
        WebDriver driver = null;
        try {
            driver = webDriverService.criarDriver();
            WebDriverWait wait = webDriverService.criarWait(driver);

            if (!queroMaisCreditoLoginService.seleniumLogin(driver, usuario)) return;

            acessarTelaConsultaMargemSiape(driver, wait);

            for (Cliente cliente : clientes) {
                if (TENTATIVAS_SEGUIDAS == 100) {
                    LIMITE_TENTATIVAS++;
                    if (LIMITE_TENTATIVAS <= 5)
                        novaTentativaEm5Minutos(casa);
                    break;
                }

                rateLimiter.acquire();
                processarClienteComTentativas(cliente, driver, wait);
            }
        } finally {
            if (driver != null) webDriverService.fecharDriver(driver);
        }
    }

    private void novaTentativaEm5Minutos(String casa) {
        Runnable tarefa = () -> {
            try {
                Thread.sleep(5 * 60 * 1000);
            } catch (Exception ignored) {}
            if (casa.equals("casa"))
                buscaMargensCasa();
            else
                buscaMargensNaoCasa();
        };

        new Thread(tarefa).start();
    }

    private void processarClienteComTentativas(Cliente cliente, WebDriver driver, WebDriverWait wait) {
        int tentativas = 0;
        boolean sucesso = false;

        while (tentativas < 5 && !sucesso) {
            try {
                CrawlerUtils.preencherCpf(cliente.getCpf(), "SIAPE_ctl00_cph_JN_JpCPF_txtCPF_CAMPO", driver, wait);
                Thread.sleep(500);

                consultarMargens(driver, wait);
                Thread.sleep(500);

                sucesso = extrairInfoMargem(driver);
                if (sucesso) {
                    fechariFrameMargem(driver);
                    TENTATIVAS_SEGUIDAS = 0;
                    break;
                } else {
                    log.error("Tentativa " + (tentativas + 1) + " falhou para CPF: " + cliente.getCpf());
                    driver.navigate().back();
                    driver.navigate().refresh();
                    acessarTelaConsultaMargemSiape(driver, wait);
                    TENTATIVAS_SEGUIDAS++;
                }
            } catch (Exception e) {
                log.warn("Erro ao processar cliente na tentativa " + (tentativas + 1));
            }
            tentativas++;
        }
        if (!sucesso) {
            log.error("Falha após 5 tentativas para CPF: " + cliente.getCpf());
        }
    }

    private void acessarTelaConsultaMargemSiape(WebDriver driver, WebDriverWait wait) {
        WebElement menuCadastro = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//a[contains(text(),'Cadastro')]")));

        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", menuCadastro);

        WebElement opcaoConsultaSiape = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[contains(text(),'Consulta Margem SIAPE')]")));

        opcaoConsultaSiape.click();
    }

    private void consultarMargens(WebDriver driver, WebDriverWait wait) {
        try {
            Thread.sleep(2000);
            WebElement consultar = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(text(),'Consultar Margens e Autorizações')]")));
            Actions actions = new Actions(driver);
            actions.moveToElement(consultar).click().perform();
            try {
                wait.until(ExpectedConditions.textToBePresentInElementLocated(
                        By.tagName("body"), "Aguarde, efetuando Consulta de Margens e Autorizações no SIAPE ..."));
                wait.until(ExpectedConditions.invisibilityOfElementLocated(
                        By.xpath("//*[contains(text(), 'Aguarde, efetuando Consulta de Margens')]")));
            } catch (Exception ignored) {}
            if (CrawlerUtils.interagirComAlert(driver)) {
                fechariFrameMargem(driver);
            }
            else {
                driver.switchTo().frame("SIAPE_Portal_Consulta");
            }
        } catch (Exception e) {
            log.error("Erro ao consultar a Margem");
        }
    }

    private boolean extrairInfoMargem(WebDriver driver) {
        try {
            WebElement blocoResultado = driver.findElement(By.id("formulario:idMostrarResultado"));
            String cpf = blocoResultado.findElement(By.xpath(".//td[1]")).getText();
            String nome = blocoResultado.findElement(By.xpath(".//td[2]")).getText();

            List<Vinculo> vinculos = new ArrayList<>();

            for (int i = 2; i <= 3; i++) {
                try {
                    Vinculo vinculo = extrairInformacoesTabela(blocoResultado, i, driver);
                    if (vinculo.getOrgao() == null || vinculo.getOrgao().isEmpty())
                        return false;
                    if (vinculos.stream().noneMatch(v -> v.getMatriculaPensionista().equals(vinculo.getMatriculaPensionista()))) {
                        vinculos.add(vinculo);
                    }
                } catch (Exception ignored) {
                }
            }
            Cliente cliente = clienteService.criarObjetoCliente(cpf, nome, vinculos);
            clienteService.salvarOuAtualizarCliente(cliente);
            log.info("Dados de margem do CPF " + cliente.getCpf() + " atualizados com sucesso");
            return true;
        } catch (Exception e) {
            log.error("Erro inesperado ao extrair dados da margem");
            return false;
        }
    }

    private Vinculo extrairInformacoesTabela(WebElement blocoResultado, int indiceTabela, WebDriver driver) {
        List<WebElement> dadosVinculo = blocoResultado.findElements(By.xpath(".//table[" + indiceTabela + "]/tbody/tr[2]/td"));
        String tipoVinculo = dadosVinculo.get(0).getText();
        String orgao = dadosVinculo.get(1).getText();
        String matriculaPensionista = clienteService.removeZerosAEsquerdaMatricula(dadosVinculo.get(2).getText());
        String matriculaInstituidor = "";
        if (dadosVinculo.size() == 3) {
            matriculaInstituidor = dadosVinculo.get(2).getText();
        }

        String margemCredito = "", autorizacaoCredito = "", situacaoCredito = "";
        String margemBeneficio = "", autorizacaoBeneficio = "", situacaoBeneficio = "";

        List<WebElement> cartaoCreditoTh = driver.findElements(By.xpath("//th[contains(text(), 'CARTÃO DE CRÉDITO')]"));
        if (!cartaoCreditoTh.isEmpty()) {
            WebElement cartaoCreditoTitulo = cartaoCreditoTh.getFirst();
            margemCredito = cartaoCreditoTitulo.findElement(By.xpath("./following-sibling::th"))
                    .getText().replace("Margem Consignável:", "").trim();

            WebElement tabelaCredito = cartaoCreditoTitulo
                    .findElement(By.xpath("ancestor::table/following::table[1]//tr[2]"));
            autorizacaoCredito = tabelaCredito.findElement(By.xpath("./td[1]")).getText().trim();
            situacaoCredito = tabelaCredito.findElement(By.xpath("./td[2]")).getText().trim();
        }

        List<WebElement> cartaoBeneficioTh = driver.findElements(By.xpath("//th[contains(text(), 'CARTÃO BENEFÍCIO')]"));
        if (!cartaoBeneficioTh.isEmpty()) {
            WebElement cartaoBeneficioTitulo = cartaoBeneficioTh.getFirst();
            margemBeneficio = cartaoBeneficioTitulo.findElement(By.xpath("./following-sibling::th"))
                    .getText().replace("Margem Consignável:", "").trim();

            WebElement tabelaBeneficio = cartaoBeneficioTitulo
                    .findElement(By.xpath("ancestor::table/following::table[1]//tr[2]"));
            autorizacaoBeneficio = tabelaBeneficio.findElement(By.xpath("./td[1]")).getText().trim();
            situacaoBeneficio = tabelaBeneficio.findElement(By.xpath("./td[2]")).getText().trim();
        }

        Vinculo vinculo = new Vinculo();
        vinculo.setTipoVinculo(tipoVinculo);
        vinculo.setOrgao(orgao);
        vinculo.setMatriculaInstituidor(matriculaInstituidor);
        vinculo.setMatriculaPensionista(matriculaPensionista);

        HistoricoConsulta historicoConsulta = historicoConsultaService.criarObjetoHistorico(vinculo, margemCredito, autorizacaoCredito, situacaoCredito, margemBeneficio, autorizacaoBeneficio, situacaoBeneficio);

        vinculo.getHistoricos().add(historicoConsulta);

        return vinculo;
    }

    private void fechariFrameMargem(WebDriver driver) {
        try {
            driver.switchTo().defaultContent();
            WebElement botaoFechar = driver.findElement(By.id("SIAPE_Portal_Consulta_Fechar"));
            botaoFechar.click();

            driver.switchTo().defaultContent();
        } catch (Exception e) {
            log.error("Erro ao tentar clicar no botão 'Fechar': " + e.getMessage().split("\\(Session info")[0]);
        }
    }

}
