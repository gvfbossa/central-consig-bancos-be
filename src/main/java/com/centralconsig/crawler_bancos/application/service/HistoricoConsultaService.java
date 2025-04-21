package com.centralconsig.crawler_bancos.application.service;

import com.centralconsig.crawler_bancos.domain.entity.HistoricoConsulta;
import com.centralconsig.crawler_bancos.domain.entity.Vinculo;
import com.centralconsig.crawler_bancos.domain.repository.HistoricoConsultaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class HistoricoConsultaService {

    HistoricoConsultaRepository historicoConsultaRepository;

    public HistoricoConsultaService(HistoricoConsultaRepository historicoConsultaRepository) {
        this.historicoConsultaRepository = historicoConsultaRepository;
    }

    public HistoricoConsulta criarObjetoHistorico(Vinculo vinculo, String margemCredito, String autorizacaoCredito, String situacaoCredito, String margemBeneficio, String autorizacaoBeneficio, String situacaoBeneficio) {

        HistoricoConsulta historico = new HistoricoConsulta();
            historico.setVinculo(vinculo);
            historico.setDataConsulta(LocalDate.now());
            historico.setMargemCredito(margemCredito.replace("R$", "").trim());
            historico.setAutorizacaoCredito(autorizacaoCredito);
            historico.setSituacaoCredito(situacaoCredito);
            historico.setMargemBeneficio(margemBeneficio.replace("R$", "").trim());
            historico.setAutorizacaoBeneficio(autorizacaoBeneficio);
            historico.setSituacaoBeneficio(situacaoBeneficio);
            historico.setDataConsulta(LocalDate.now());

            return historico;
    }

}
