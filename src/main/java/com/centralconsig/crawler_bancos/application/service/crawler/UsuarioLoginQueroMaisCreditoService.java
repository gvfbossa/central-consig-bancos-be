package com.centralconsig.crawler_bancos.application.service.crawler;

import com.centralconsig.crawler_bancos.application.dto.request.UsuarioLoginQueroMaisCreditoRequestDTO;
import com.centralconsig.crawler_bancos.application.dto.response.UsuarioLoginQueroMaisCreditoResponseDTO;
import com.centralconsig.crawler_bancos.domain.entity.UsuarioLoginQueroMaisCredito;
import com.centralconsig.crawler_bancos.domain.repository.UsuarioLoginQueroMaisCreditoRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioLoginQueroMaisCreditoService {

    private final UsuarioLoginQueroMaisCreditoRepository usuarioLoginQueroMaisCreditoRepository;

    public UsuarioLoginQueroMaisCreditoService(UsuarioLoginQueroMaisCreditoRepository usuarioLoginQueroMaisCreditoRepository) {
        this.usuarioLoginQueroMaisCreditoRepository = usuarioLoginQueroMaisCreditoRepository;
    }

    public List<UsuarioLoginQueroMaisCredito> retornaUsuariosParaCrawler() {
        return usuarioLoginQueroMaisCreditoRepository.findAll();
    }

    public boolean atualizaUsuario(UsuarioLoginQueroMaisCreditoRequestDTO usuarioDTO) {
        Optional<UsuarioLoginQueroMaisCredito> usuarioOpt = usuarioLoginQueroMaisCreditoRepository.findByUsername(usuarioDTO.getLogin());

        if (usuarioOpt.isPresent()) {
            UsuarioLoginQueroMaisCredito usuario = usuarioOpt.get();
            usuario.setPassword(usuarioDTO.getSenha());
            usuario.setSomenteConsulta(usuarioDTO.isSomenteConsulta());
            usuarioLoginQueroMaisCreditoRepository.save(usuario);
            return true;
        }
        return false;
    }

    public boolean insereUsuario(UsuarioLoginQueroMaisCreditoRequestDTO usuarioDTO) {
        Optional<UsuarioLoginQueroMaisCredito> usuarioOpt = usuarioLoginQueroMaisCreditoRepository.findByUsername(usuarioDTO.getLogin());

        if (usuarioOpt.isEmpty()) {
            UsuarioLoginQueroMaisCredito usuario = new UsuarioLoginQueroMaisCredito();
            usuario.setPassword(usuarioDTO.getSenha());
            usuario.setUsername(usuarioDTO.getLogin());
            usuario.setSomenteConsulta(usuarioDTO.isSomenteConsulta());
            usuarioLoginQueroMaisCreditoRepository.save(usuario);
            return true;
        }
        return false;
    }

    public boolean removeUsuario(String username) {
        Optional<UsuarioLoginQueroMaisCredito> usuarioOpt = usuarioLoginQueroMaisCreditoRepository.findByUsername(username);

        if (usuarioOpt.isPresent()) {
            usuarioLoginQueroMaisCreditoRepository.delete(usuarioOpt.get());
            return true;
        }
        return false;
    }
}
