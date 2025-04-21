package com.centralconsig.crawler_bancos.application.service.system;

import com.centralconsig.crawler_bancos.domain.entity.SystemConfiguration;
import com.centralconsig.crawler_bancos.domain.repository.SystemConfigurationRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

@Service
public class SystemConfigurationService {

    SystemConfigurationRepository systemConfigurationRepository;

    public SystemConfigurationService(SystemConfigurationRepository systemConfigurationRepository) {
        this.systemConfigurationRepository = systemConfigurationRepository;
    }

    @PostConstruct
    public void init() {
        if (systemConfigurationRepository.count() == 0) {
            SystemConfiguration config = new SystemConfiguration();
            config.setPropostaAutomatica(false);
            systemConfigurationRepository.save(config);
        } else if (systemConfigurationRepository.count() > 1) {
            systemConfigurationRepository.deleteAll(systemConfigurationRepository.findAll().subList(1, systemConfigurationRepository.findAll().size()));
        }

    }

    public void atualizaValorPropostaAutomatica() {
        SystemConfiguration systemConfiguration = systemConfigurationRepository.findAll().getFirst();
        systemConfiguration.setPropostaAutomatica(!systemConfiguration.isPropostaAutomatica());
        systemConfigurationRepository.save(systemConfiguration);
    }

    public boolean isPropostaAutomaticaAtiva() {
        return systemConfigurationRepository.findAll().getFirst().isPropostaAutomatica();
    }

}
