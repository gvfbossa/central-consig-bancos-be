package com.centralconsig.crawler_bancos.application.utils;

import com.centralconsig.crawler_bancos.application.service.crawler.QueroMaisCreditoPropostaService;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.Duration;

public class CrawlerUtils {

    private static final Logger log = LoggerFactory.getLogger(CrawlerUtils.class);

    public static void preencherCpf(String cpf, String campoId, WebDriver driver, WebDriverWait wait) {
        try {
            WebElement inputCpf = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.id(campoId)));

            Actions actions = new Actions(driver);
            actions.moveToElement(inputCpf).click().perform();
            for (char c : cpf.toCharArray()) {
                inputCpf.sendKeys(Character.toString(c));
            }
        } catch (Exception e) {
            System.out.println("Erro ao preencher o CPF: " + e.getMessage().substring(0, e.getMessage().indexOf("\n")));
        }
    }

    public static boolean interagirComAlert(WebDriver driver) {
        boolean nrChamadas = false;
        try {
            WebDriverWait waitAlert = new WebDriverWait(driver, Duration.ofSeconds(5));
            waitAlert.until(ExpectedConditions.alertIsPresent());

            Alert alert = driver.switchTo().alert();

            if (alert.getText().contains("Esgotou o numero de chamadas SIAPE MARGEM por minuto")) {

                log.info("Rate limit atingido. Pausando por 5 minutos...");
                Thread.sleep(60000 * 5);
            }
            alert.accept();
            nrChamadas = true;
        } catch (Exception ignored) {}
        return nrChamadas;
    }

    public static BigDecimal parseBrlToBigDecimal(String valorStr) {
        try {
            String normalized = valorStr.replace(".", "").replace(",", ".");
            return new BigDecimal(normalized);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao converter valor: " + valorStr, e);
        }
    }

    public static void killChromeDrivers() {
        try {
            Process chromedriverProcess = Runtime.getRuntime().exec("pgrep -f chromedriver");
            BufferedReader reader = new BufferedReader(new InputStreamReader(chromedriverProcess.getInputStream()));
            String pid;
            while ((pid = reader.readLine()) != null) {
                Process pgrepChild = Runtime.getRuntime().exec("pgrep -P " + pid);
                BufferedReader childReader = new BufferedReader(new InputStreamReader(pgrepChild.getInputStream()));
                String childPid;
                while ((childPid = childReader.readLine()) != null) {
                    Runtime.getRuntime().exec("kill -9 " + childPid);
                    log.info("Matando processo filho Chrome PID=" + childPid);
                }
                Runtime.getRuntime().exec("kill -9 " + pid);
                log.info("Matando processo ChromeDriver PID=" + pid);
            }
            log.info("Processos de ChromeDriver e filhos finalizados.");
        } catch (Exception e) {
            log.error("Erro ao tentar matar processos ChromeDriver e filhos", e);
        }
    }

    public static void esperar(long segundos) {
        try {
            Thread.sleep(segundos*1000);
        } catch (Exception ignored) {}
    }

}
