package com.centralconsig.crawler_bancos.application.service.crawler;

import com.centralconsig.crawler_bancos.application.service.PropostaService;
import com.centralconsig.crawler_bancos.domain.entity.FormularioCancelamentoConfig;
import com.centralconsig.crawler_bancos.domain.entity.Proposta;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class FormularioCancelamentoPropostaService {

    private final WebDriverService webDriverService;
    private WebDriverWait wait;
    private WebDriver driver;

    private final PropostaService propostaService;
    private final FormularioCancelamentoConfigService configService;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Value("${form.cancelamento.url}")
    private String URL_FORM;

    private static final Logger log = LoggerFactory.getLogger(FormularioCancelamentoPropostaService.class);

    public FormularioCancelamentoPropostaService(WebDriverService webDriverService, PropostaService propostaService,
                                                 FormularioCancelamentoConfigService configService) {
        this.webDriverService = webDriverService;
        this.propostaService = propostaService;
        this.configService = configService;
    }

    public boolean cancelaPropostas(List<String> numeros) {
        if (!isRunning.compareAndSet(false, true)) {
            log.info("Cancelamento de Propostas já em execução. Ignorando nova tentativa.");
            return false;
        }

        this.driver = webDriverService.criarDriver();
        this.wait = webDriverService.criarWait(driver);

        try {
            for (String numero : numeros) {
                acessaForm();
                preencheInformacoesForm(numero);
                propostaService.removerProposta(numero);
            }
        } catch(Exception e) {
            return false;
        } finally {
            isRunning.set(false);
            webDriverService.fecharDriver(driver);
        }
        return true;
    }

    private void acessaForm() {
        driver.get(URL_FORM);
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//*[contains(text(),'Solicitação de cancelamento de propostas')]")));
    }

    private void preencheInformacoesForm(String numero) {
        FormularioCancelamentoConfig config = configService.getConfig();

        Optional<Proposta> propostaDb = propostaService.retornaPropostaPorNumero(numero);
        if (propostaDb.isEmpty())
            throw new RuntimeException("Proposta não encontrada com o número: " + numero);

        Proposta proposta = propostaDb.get();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@aria-label='Seu e-mail']"))).sendKeys(config.getEmail());

        WebElement radioCancelamento = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@role='radio' and @aria-label='Cancelamento de proposta']")));
        Actions actions = new Actions(driver);
        actions.moveToElement(radioCancelamento).click().perform();


        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@aria-labelledby='i19 i22']"))).sendKeys(proposta.getNumeroProposta());

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@aria-labelledby='i24 i27']")))
                .sendKeys(proposta.getCliente().getNome());

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@aria-labelledby='i29 i32']")))
                .sendKeys(proposta.getCliente().getCpf());

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@aria-labelledby='i34 i37']")))
                .sendKeys(config.getMotivoCancelamento());

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@aria-labelledby='i39 i42']")))
                .sendKeys(config.getPromotora());

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@aria-labelledby='i44 i47']")))
                .sendKeys(config.getEmail());

        WebElement radioEstouCiente = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@role='radio' and @aria-label='Estou ciente.']")));
        actions.moveToElement(radioEstouCiente).click().perform();

        WebElement botaoEnviar = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@role='button']//span[contains(text(),'Enviar')]")));
        actions.moveToElement(botaoEnviar).click().perform();
    }

}