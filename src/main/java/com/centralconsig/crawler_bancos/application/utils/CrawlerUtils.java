package com.centralconsig.crawler_bancos.application.utils;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.math.BigDecimal;
import java.time.Duration;

public class CrawlerUtils {

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
            System.out.println("Erro ao preencher o CPF: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean interagirComAlert(WebDriver driver) {
        boolean nrChamadas = false;
        try {
            WebDriverWait waitAlert = new WebDriverWait(driver, Duration.ofSeconds(5));
            waitAlert.until(ExpectedConditions.alertIsPresent());

            Alert alert = driver.switchTo().alert();

            if (alert.getText().contains("Esgotou o numero de chamadas SIAPE MARGEM por minuto")) {
                nrChamadas = true;
                System.out.println("Rate limit atingido. Pausando por 5 minutos...");
                Thread.sleep(60000 * 5);
            }
            alert.accept();
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

}
