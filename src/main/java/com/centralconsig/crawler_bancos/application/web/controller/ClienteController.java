package com.centralconsig.crawler_bancos.application.web.controller;

import com.centralconsig.crawler_bancos.application.dto.response.ClienteResponseDTO;
import com.centralconsig.crawler_bancos.application.mapper.ClienteMapper;
import com.centralconsig.crawler_bancos.application.service.ClienteService;
import com.centralconsig.crawler_bancos.domain.entity.Cliente;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cliente")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping("/todos")
    public ResponseEntity<?> getAllClientes() {
        List<Cliente> clientes = clienteService.getAllClientes();

        if (!clientes.isEmpty())
            return ResponseEntity.ok(clienteService.getAllClientes());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @GetMapping("/busca")
    public ResponseEntity<?> getClienteByMatriculaOrCpf(
            @RequestParam(required = false) String cpf,
            @RequestParam(required = false) String matricula) {

        try {
            ClienteResponseDTO cliente = clienteService.buscaClientePorCpfOuMatricula(cpf, matricula);

            if (cliente == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            return ResponseEntity.ok(cliente);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erro ao realizar a busca");
        }
    }

    @PostMapping("/black-list")
    public ResponseEntity<?> atualizaBlackListCliente(@RequestParam String cpf) {
        try {
            Cliente cliente = clienteService.findByCpf(cpf);

            if (cliente == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            cliente.setBlackList(!cliente.isBlackList());
            clienteService.salvarOuAtualizarCliente(cliente);

            return ResponseEntity.ok(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erro ao atualizar BlackList Cliente");
        }
    }

}
