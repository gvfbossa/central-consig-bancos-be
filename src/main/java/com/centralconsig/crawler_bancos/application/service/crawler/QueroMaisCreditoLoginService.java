package com.centralconsig.crawler_bancos.application.service.crawler;

import com.centralconsig.crawler_bancos.domain.entity.UsuarioLoginQueroMaisCredito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

@Service
public class QueroMaisCreditoLoginService {

    private final WebDriverService webDriverService;

    @Value("${base.url.quero.mais.credito}")
    private String BASE_URL;

    private static final Logger log = LoggerFactory.getLogger(QueroMaisCreditoLoginService.class);

    public QueroMaisCreditoLoginService(WebDriverService webDriverService) {
        this.webDriverService = webDriverService;
    }

    public boolean seleniumLogin(WebDriver driver, UsuarioLoginQueroMaisCredito usuario) {
        WebDriverWait wait = webDriverService.criarWait(driver);
        try {
            driver.get(BASE_URL);

            WebElement userField = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("EUsuario_CAMPO")));
            WebElement passwordField = driver.findElement(By.id("ESenha_CAMPO"));
            WebElement loginButton = driver.findElement(By.id("lnkEntrar"));

            userField.sendKeys(usuario.getUsername());
            passwordField.sendKeys(usuario.getPassword());
            loginButton.click();

            WebElement negociacaoDiv = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//div[contains(text(),'NEGOCIAÇÃO')]")));
            negociacaoDiv.click();

            WebElement autorizadorLink = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//div[@class='submenu-item']//a[contains(text(),'Autorizador')]")));
            autorizadorLink.click();

            wait.until(ExpectedConditions.urlContains("WebAutorizador"));

            log.info("Login efetuado com sucesso!");

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
