package com.centralconsig.crawler_bancos.application.service.crawler;

import com.centralconsig.crawler_bancos.application.service.ClienteService;
import com.centralconsig.crawler_bancos.application.service.PropostaService;
import com.centralconsig.crawler_bancos.application.service.system.SystemConfigurationService;
import com.centralconsig.crawler_bancos.application.utils.CrawlerUtils;
import com.centralconsig.crawler_bancos.domain.entity.Cliente;
import com.centralconsig.crawler_bancos.domain.entity.Proposta;
import com.centralconsig.crawler_bancos.domain.entity.UsuarioLoginQueroMaisCredito;
import com.centralconsig.crawler_bancos.domain.entity.Vinculo;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class QueroMaisCreditoPropostaService {

    private final WebDriverService webDriverService;

    private final ClienteService clienteService;
    private final PropostaService propostaService;

    private final QueroMaisCreditoLoginService queroMaisCreditoLoginService;
    private final UsuarioLoginQueroMaisCreditoService usuarioLoginQueroMaisCreditoService;
    private final SystemConfigurationService systemConfigurationService;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private static final int THREAD_COUNT = 9;

    private Cliente cliente;
    private Proposta proposta;

    private static final Logger log = LoggerFactory.getLogger(QueroMaisCreditoPropostaService.class);

    public QueroMaisCreditoPropostaService(WebDriverService webDriverService, ClienteService clienteService,
           PropostaService propostaService, UsuarioLoginQueroMaisCreditoService usuarioLoginQueroMaisCreditoService,
           QueroMaisCreditoLoginService queroMaisCreditoLoginService, SystemConfigurationService systemConfigurationService) {
        this.webDriverService = webDriverService;
        this.clienteService = clienteService;
        this.propostaService = propostaService;
        this.usuarioLoginQueroMaisCreditoService = usuarioLoginQueroMaisCreditoService;
        this.queroMaisCreditoLoginService = queroMaisCreditoLoginService;
        this.systemConfigurationService = systemConfigurationService;
        proposta = new Proposta();
    }

    @Scheduled(cron = "0 0 13,15,17,19 * * MON-FRI")
    public void executarPropostasAuto() {
        if (!systemConfigurationService.isPropostaAutomaticaAtiva())
            return;
        if (!isRunning.compareAndSet(false, true)) {
            log.info("Criar Proposta já em execução. Ignorando nova tentativa.");
            return;
        }
        criarProposta();
        isRunning.set(false);
    }

    private void criarProposta() {
        List<UsuarioLoginQueroMaisCredito> usuarios = usuarioLoginQueroMaisCreditoService.retornaUsuariosParaCrawler().stream()
                .filter(usuario -> !"46985260861_900411".equals(usuario.getUsername())).toList();

        List<Cliente> clientes = clienteService.clientesFiltradosPorMargem();

        int numThreads = 8;
        int totalClientes = clientes.size();
        int clientesPorThread = totalClientes / numThreads;

        try (ExecutorService executor = Executors.newFixedThreadPool(numThreads)) {
            for (int i = 0; i < numThreads; i++) {
                int startCliente = i * clientesPorThread;
                int endCliente = (i == numThreads - 1) ? totalClientes : (i + 1) * clientesPorThread;
                List<Cliente> clientesSubset = clientes.subList(startCliente, endCliente);

                UsuarioLoginQueroMaisCredito usuario = usuarios.get(i % usuarios.size());

                executor.submit(() -> {
                    try {
                        WebDriver driver = webDriverService.criarDriver();
                        WebDriverWait wait = webDriverService.criarWait(driver);
                        queroMaisCreditoLoginService.seleniumLogin(driver, usuario);

                        for (Cliente cliente : clientesSubset) {
                            processarCliente(driver, wait, cliente);
                        }
                    } catch (Exception e) {
                        log.error("Erro ao processar proposta: " + e.getMessage());
                    }
                });
            }
            boolean terminou = executor.awaitTermination(3, TimeUnit.HOURS);

            if (!terminou) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void processarCliente(WebDriver driver, WebDriverWait wait, Cliente cliente) {
        for (Vinculo vinculo : cliente.getVinculos()) {
            boolean sucesso = tentarExecutar(() -> {
                acessarTelaProposta(driver, wait);
                selecionaEmpregador(wait);
                preencheCpfCliente(driver, wait);
                selecionaClientePorMatricula(driver, wait, vinculo.getMatriculaPensionista());
                preencheMargemCliente(driver, wait, vinculo.getHistoricos().getFirst().getMargemBeneficio());
                calcularSimulacaoCliente(driver, wait);
                selecionarPrimeiraOpcaoCheckBoxTabela(driver, wait);
                isentarSeguroProposta(driver, wait);
//                gravarProposta(driver, wait); //TODO
            }, driver, wait);

            if (sucesso) {
                try {
                    adicionarPdfNaProposta(driver, wait);
                    aprovarProposta(driver, wait);
                    retornarParaTelaPrincipal(driver, wait);
                    acessarTelaEsteira(driver, wait);
                    buscarProposta(driver, wait, proposta.getNumeroProposta());
                    obtemInformacoesValoresProposta(driver, wait);
                    capturaLinkAssinatura(driver, wait);
                } catch (Exception e) {
                    log.error("Erro ao finalizar proposta para cliente " + cliente.getCpf() + ": " + e.getMessage());
                }
            }
        }
    }

    private boolean tentarExecutar(Runnable operacao,WebDriver driver, WebDriverWait wait) {
        for (int tentativa = 1; tentativa <= 3; tentativa++) {
            try {
                operacao.run();
                return true;
            } catch (Exception e) {
                if (tentativa < 3) {
                    driver.navigate().back();
                    acessarTelaProposta(driver, wait);
                }
            }
        }
        return false;
    }

    private void acessarTelaProposta(WebDriver driver, WebDriverWait wait) {
        WebElement menuCadastro = wait.until(ExpectedConditions.presenceOfElementLocated(
        By.xpath("//a[contains(text(),'Cadastro')]")));

        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", menuCadastro);

        WebElement opcaoProposta = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(text(),'Proposta Cartão - SIAPE')]")));

        opcaoProposta.click();
        try {
            Thread.sleep(5000);
        } catch (Exception ignored) {}
    }

    private void selecionaEmpregador(WebDriverWait wait) {
            WebElement selectElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.id("ctl00_Cph_UcPrp_FIJN1_JnDadosIniciais_UcDIni_cboOrigem4_CAMPO")));

            Select select = new Select(selectElement);
            select.selectByValue("202329");
        try {
            Thread.sleep(500);
        } catch (Exception ignored) {}
    }

    private void preencheCpfCliente(WebDriver driver, WebDriverWait wait) {
        CrawlerUtils.preencherCpf(cliente.getCpf(), "ctl00_Cph_UcPrp_FIJN1_JnDadosIniciais_UcDIni_txtCPF_CAMPO", driver, wait);
        try {
          Thread.sleep(1000);
        } catch (Exception ignored) {}
        Actions actions = new Actions(driver);
        actions.sendKeys(Keys.ENTER).perform();
    }

    private void selecionaClientePorMatricula(WebDriver driver, WebDriverWait wait, String matricula) {
        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt("ctl00_Cph_UcPrp_FIJN1_JnDadosIniciais_UcDIni_popCliente_frameAjuda"));

        WebElement linkCliente = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//td[contains(text(), '" + matricula + "')]/preceding-sibling::td/a")
        ));

        linkCliente.click();

        try {
            Thread.sleep(2000);
        } catch (Exception ignored) {}

        driver.switchTo().defaultContent();

        capturaTelefoneCliente(driver);
    }

    private void preencheMargemCliente(WebDriver driver, WebDriverWait wait, String margem) {
       JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("if (document.activeElement) document.activeElement.blur();");

        WebElement inputVlrMargem = driver.findElement(By.xpath("//input[contains(@id, 'txtVlrParcela_CAMPO')]"));

        Actions actions = new Actions(driver);
        actions.moveToElement(inputVlrMargem).click().sendKeys(margem).perform();

        js.executeScript("if (document.activeElement) document.activeElement.blur();");
    }

    private void calcularSimulacaoCliente(WebDriver driver, WebDriverWait wait) {
        WebElement btnCalcular = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("ctl00_Cph_UcPrp_FIJN1_JnSimulacao_UcSimulacaoSnt_FIJanela1_FIJanelaPanel1_UcBtnCalc_btnCalcular_dvBtn")
        ));
        Actions actions = new Actions(driver);
        actions.moveToElement(btnCalcular).click().perform();

        int timer = 0;
        while (true) {
            if (!driver.getPageSource().contains("Não existem dados para exibição"))
                break;
            if (timer >= 30)
                break;

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            timer++;
        }
    }

    private void selecionarPrimeiraOpcaoCheckBoxTabela(WebDriver driver, WebDriverWait wait) {
        WebElement primeiraOpcao = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//table[contains(., 'Tabela') and contains(., 'Descrição Tabela')]//input[@type='checkbox']")));
        primeiraOpcao.click();
        try {
            Thread.sleep(2000);
        } catch (Exception ignored){}
    }

    private void isentarSeguroProposta(WebDriver driver, WebDriverWait wait) {
        WebElement itemSeguro = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[contains(text(), 'SEGURO ACIDENTE PESSOAL')]")));
        itemSeguro.click();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {}

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[contains(text(), 'SEGURO: Escolha o tipo de Proposta para o cliente')]")
        ));

        WebElement isentarRadio = driver.findElement(
                By.xpath("//label[contains(normalize-space(), 'Isentar o cliente')]")
        );
        isentarRadio.click();

        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("cap_Prosseguir_Seguro_Confirma();");

        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {}
    }

    private void capturaTelefoneCliente(WebDriver driver) {
        WebElement dddInput = driver.findElement(By.id("ctl00_Cph_UcPrp_FIJN1_JnDadosCliente_UcDadosPessoaisClienteSnt_FIJN1_JnC_txtDddTelCelular_CAMPO"));

        WebElement celularInput = driver.findElement(By.id("ctl00_Cph_UcPrp_FIJN1_JnDadosCliente_UcDadosPessoaisClienteSnt_FIJN1_JnC_txtTelCelular_CAMPO"));

        String ddd = dddInput.getAttribute("value");
        String celular = celularInput.getAttribute("value");


        if (cliente.getTelefone() == null || cliente.getTelefone().isBlank() || !cliente.getTelefone().equals("(" + ddd + ") " + celular))
            cliente.setTelefone("(" + ddd + ") " + celular);
    }

    private void gravarProposta(WebDriver driver, WebDriverWait wait) {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {
        }

        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("if (document.activeElement) document.activeElement.blur();");

        WebElement botaoGravar = wait.until(ExpectedConditions.elementToBeClickable(By.id("ctl00_Cph_UcPrp_FIJN1_JnBotoes_UcBotoes_btnGravar_dvBtn")));

        Actions actions = new Actions(driver);
        actions.moveToElement(botaoGravar).click().perform();

        int timer = 0;

        while (true) {
            if (driver.getPageSource().contains("Cadastro Finalizado com Sucesso ! Proposta"))
                break;
            if (timer >= 30)
                break;

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            timer++;
        }

        CrawlerUtils.interagirComAlert(driver);

        WebElement msgSpan = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("ctl00_Cph_UcConf_lblMsg")));

        setaDadosNaProposta(msgSpan.getText().substring(msgSpan.getText().indexOf("Proposta")).replace("Proposta", "").trim(), null,null,null);
        salvarDadosPropostaCliente();

        WebElement botaoConfirmar = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[text()[contains(., 'Confirmar')]]")));
        botaoConfirmar.click();
    }

    private void adicionarPdfNaProposta(WebDriver driver, WebDriverWait wait) {
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[text()[contains(., 'Documentação para cadastro')]]")));

        try {
            Thread.sleep(3000);
        } catch (Exception ignored){}

        WebElement iconeAnexar = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("imgChLstP_000023_3")));

        Actions actions = new Actions(driver);
        actions.moveToElement(iconeAnexar).perform();

        WebElement menuAnexar = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("P_000023_3")));

        WebElement linkAnexarDocumento = menuAnexar.findElement(By.xpath(".//a[contains(text(), 'Anexar Documento')]"));
        linkAnexarDocumento.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("ctl00_Cph_TBCP_1_uc_1_1_upModal")));

        File file = new File("src/main/resources/utils/blank_pdf_file.pdf");
        String absolutePath = file.getAbsolutePath();

        try {
            Thread.sleep(1000);
        } catch (Exception ignored){}

        List<WebElement> iframes = driver.findElements(By.tagName("iframe"));
        boolean iframeFound = false;

        for (int i = 0; i < iframes.size(); i++) {
            driver.switchTo().defaultContent();

                driver.switchTo().frame(i);

            if (Objects.requireNonNull(driver.getPageSource()).contains("Upload de Arquivos")) {
                iframeFound = true;
                break;
            }
        }
        if (!iframeFound) {
            throw new RuntimeException("Não foi possível localizar o iframe do modal de upload!");
        }
        WebElement inputUpload = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//input[@type='file']"))
        );
        inputUpload.sendKeys(absolutePath);
        try {
            Thread.sleep(1000);
        } catch (Exception ignored){}


        WebElement botaoAdicionar = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//*[normalize-space(text())='Adicionar']"))
        );
        botaoAdicionar.click();

        try {
            Thread.sleep(1000);
        } catch (Exception ignored){}

        WebElement botaoAnexar = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//*[normalize-space(text())='Anexar']"))
        );
        botaoAnexar.click();

        try {
            Thread.sleep(1000);
        } catch (Exception ignored){}

        CrawlerUtils.interagirComAlert(driver);

        WebElement botaoFechar = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//*[normalize-space(text())='Fechar']"))
        );
        botaoFechar.click();
    }

    private void aprovarProposta(WebDriver driver, WebDriverWait wait) {
        driver.switchTo().parentFrame();

        WebElement botaoAprova = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//a[@id='BBApr_txt' and contains(text(), 'Aprova')]"))
        );

        Actions actions = new Actions(driver);
        actions.moveToElement(botaoAprova).click().perform();

        try {
            Thread.sleep(1000);
        } catch (Exception ignored){}
    }

    private void salvarDadosPropostaCliente() {
        cliente = Optional.ofNullable(cliente)
                .orElseGet(() -> {
                    Cliente c = clienteService.findByCpf(cliente.getCpf());
                    if (c == null) {
                        throw new RuntimeException("Cliente não encontrado no banco...");
                    }
                    return c;
                });

        if (proposta.getCliente() == null) {
            proposta = propostaService
                    .retornaPropostaPorClienteEData(cliente, LocalDate.now())
                    .orElseThrow(() -> new RuntimeException("Proposta não encontrada no banco..."));
        }
        propostaService.salvarPropostaEAtualizarCliente(proposta, cliente);
    }

    private void retornarParaTelaPrincipal(WebDriver driver, WebDriverWait wait) {
        driver.switchTo().parentFrame();
        try {
            Thread.sleep(1000);
        } catch (Exception ignored){}

        WebElement botaoVoltar = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//*[normalize-space(text())='Voltar']"))
        );
        botaoVoltar.click();

        try {
            Thread.sleep(1000);
        } catch (Exception ignored){}

        driver.navigate().back();
        try {
            Thread.sleep(2000);
        } catch (Exception ignored){}
    }

    private void acessarTelaEsteira(WebDriver driver, WebDriverWait wait) {
        WebElement menuCadastro = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//a[contains(text(),'Esteira')]")));

        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", menuCadastro);

        WebElement opcaoProposta = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[contains(text(),'Aprovação / Consulta')]")));

        opcaoProposta.click();
        try {
            Thread.sleep(5000);
        } catch (Exception ignored) {}
    }

    public void buscarProposta(WebDriver driver, WebDriverWait wait, String numeroProposta) {
        WebElement campoBusca = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//span[text()='Nr. Proposta:']/following-sibling::input")));

        Optional<Proposta> proposta = propostaService.retornaPropostaPorClienteEData(cliente, cliente.getPropostas().getFirst().getDataCadastro());
        if(proposta.isEmpty())
            throw new RuntimeException("Proposta de número " + numeroProposta + " não encontrada no banco de dados");

        Actions actions = new Actions(driver);
        actions.moveToElement(campoBusca).click().sendKeys(proposta.get().getNumeroProposta()).perform();


        WebElement btnPesquisar = wait.until(ExpectedConditions.elementToBeClickable(By.id("ctl00_Cph_AprCons_btnPesquisar")));
        btnPesquisar.click();
        try {
            Thread.sleep(2000);
        } catch (Exception ignored) {}
    }

    private void obtemInformacoesValoresProposta(WebDriver driver, WebDriverWait wait) {
        String pageText = driver.findElement(By.tagName("body")).getText();
        String pageAuxStr = pageText.split("Nr Proposta Convênio")[1];
        pageAuxStr = pageAuxStr.split("CARTÃO")[0];
        String[] pageAuxSplt = pageAuxStr.split(" ");
        String valorParcela = pageAuxSplt[pageAuxSplt.length-1];
        String valorLiberado = pageAuxSplt[pageAuxSplt.length-3];

        setaDadosNaProposta(null, CrawlerUtils.parseBrlToBigDecimal(valorLiberado), CrawlerUtils.parseBrlToBigDecimal(valorParcela), null);
    }

    private void capturaLinkAssinatura(WebDriver driver, WebDriverWait wait) {
        WebElement validaCodigoSiapeLink = wait.until(ExpectedConditions.elementToBeClickable(By.linkText("VALIDA CODIGO SIAPE")));
        validaCodigoSiapeLink.click();

        WebElement documentos = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[contains(text(),'DOCUMENTOS')]")));

        Actions actions = new Actions(driver);
        actions.moveToElement(documentos).click().perform();

        String bodySplt = driver.findElement(By.tagName("body")).getText().split("Link de Assinatura:")[1];
        String linkAssinatura = bodySplt.split("Inserir Novas Observações")[0].trim();

        setaDadosNaProposta(null,null,null, linkAssinatura);
        salvarDadosPropostaCliente();
    }

    private void setaDadosNaProposta(String numeroProposta, BigDecimal vlrLib, BigDecimal vlrParc, String linkAss) {
        if (numeroProposta != null)
            proposta.setNumeroProposta(numeroProposta);
        if (vlrLib != null)
            proposta.setValorLiberado(vlrLib);
        if (vlrParc != null)
            proposta.setValorParcela(vlrParc);
        if (linkAss != null)
            proposta.setLinkAssinatura(linkAss);
    }

}
