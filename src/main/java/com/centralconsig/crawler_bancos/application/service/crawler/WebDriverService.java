package com.centralconsig.crawler_bancos.application.service.crawler;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class WebDriverService {

    @Value("${sheet.download.dir}")
    private String DOWNLOAD_DIR;

    public WebDriver criarDriver() {
        String downloadPath = Paths.get(DOWNLOAD_DIR).toAbsolutePath().toString();
        ChromeOptions options = new ChromeOptions();
//        options.addArguments("--headless=new"); //TODO
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadPath);
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.directory_upgrade", true);
        prefs.put("safebrowsing.enabled", true);
        prefs.put("profile.default_content_settings.popups", 0);
        prefs.put("profile.content_settings.exceptions.automatic_downloads.*.setting", 1);

        options.setExperimentalOption("prefs", prefs);

        return new ChromeDriver(options);
    }

    public WebDriverWait criarWait(WebDriver driver) {
        return new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    public void fecharDriver(WebDriver driver) {
        try {
            driver.close();
        } catch (Exception ignored){}
    }

}
