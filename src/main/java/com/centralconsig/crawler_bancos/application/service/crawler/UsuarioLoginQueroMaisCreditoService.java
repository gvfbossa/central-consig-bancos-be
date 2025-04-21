package com.centralconsig.crawler_bancos.application.service.crawler;

import com.centralconsig.crawler_bancos.domain.entity.UsuarioLoginQueroMaisCredito;
import com.centralconsig.crawler_bancos.domain.repository.UsuarioLoginQueroMaisCreditoRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioLoginQueroMaisCreditoService {

    private final UsuarioLoginQueroMaisCreditoRepository usuarioLoginQueroMaisCreditoRepository;

    public UsuarioLoginQueroMaisCreditoService(UsuarioLoginQueroMaisCreditoRepository usuarioLoginQueroMaisCreditoRepository) {
        this.usuarioLoginQueroMaisCreditoRepository = usuarioLoginQueroMaisCreditoRepository;
    }

    @PostConstruct
    public void init() {
        List<UsuarioLoginQueroMaisCredito> usuarios = List.of(
                new UsuarioLoginQueroMaisCredito("40438852885_900411", "Sucesso@251"),
                new UsuarioLoginQueroMaisCredito("12693180805_900411", "Sucesso@251"),
                new UsuarioLoginQueroMaisCredito("25186301809_900411", "Sucesso@251"),
                new UsuarioLoginQueroMaisCredito("38190488805_900411", "Sucesso@251"),
                new UsuarioLoginQueroMaisCredito("30730703894_900411", "Sucesso@251"),
                new UsuarioLoginQueroMaisCredito("48369294820_900411", "Sucesso@251"),
                new UsuarioLoginQueroMaisCredito("42265813850_900411", "Sucesso@250"),
                new UsuarioLoginQueroMaisCredito("18761619817_900411", "Sucesso@250"),
                new UsuarioLoginQueroMaisCredito("46985260861_900411", "Sucesso@250"));

        usuarioLoginQueroMaisCreditoRepository.saveAll(usuarios);
    }

    public List<UsuarioLoginQueroMaisCredito> retornaUsuariosParaCrawler() {
        return usuarioLoginQueroMaisCreditoRepository.findAll();
    }

}
