package com.centralconsig.crawler_bancos.application.service;

import com.centralconsig.crawler_bancos.domain.entity.Cliente;
import com.centralconsig.crawler_bancos.domain.entity.Proposta;
import com.centralconsig.crawler_bancos.domain.repository.PropostaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PropostaService {

    private final PropostaRepository propostaRepository;
    private final ClienteService clienteService;

    public PropostaService(PropostaRepository propostaRepository, ClienteService clienteService) {
        this.propostaRepository = propostaRepository;
        this.clienteService = clienteService;
    }

    public void salvarPropostaEAtualizarCliente(Proposta proposta, Cliente cliente) {
        cliente = clienteService.salvarOuAtualizarCliente(cliente);

        Optional<Proposta> propostaExistenteOpt = propostaRepository.findByNumeroProposta(proposta.getNumeroProposta());

        Cliente clienteFinal = cliente;

        Proposta propostaFinal = propostaExistenteOpt.map(persistida -> {
            persistida.setCliente(clienteFinal);
            persistida.setDataCadastro(LocalDate.now());
            persistida.setValorLiberado(proposta.getValorLiberado());
            persistida.setValorParcela(proposta.getValorParcela());
            return persistida;
        }).orElseGet(() -> {
            proposta.setCliente(clienteFinal);
            proposta.setDataCadastro(LocalDate.now());
            return proposta;
        });
        propostaRepository.save(propostaFinal);
    }

    public Optional<Proposta> retornaPropostaPorNumero(String numero) {
        return propostaRepository.findByNumeroProposta(numero);
    }

    public Optional<Proposta> retornaPropostaPorClienteEData(Cliente cliente, LocalDate dataCadastro) {
        return propostaRepository.findByClienteAndDataCadastro(cliente, dataCadastro);
    }

    public Page<Proposta> getAllPropostas(Pageable pageable) {
        return propostaRepository.findAll(pageable);
    }

    public void removerProposta(String numeroProposta) {
        Optional<Proposta> prop = retornaPropostaPorNumero(numeroProposta);
        prop.ifPresent(propostaRepository::delete);
    }

    public Optional<List<Proposta>> getPropostasPorCliente(Cliente cliente) {
        return propostaRepository.findByCliente(cliente);
    }

    public List<Proposta> todasAsPropostas() {
        return propostaRepository.findAll();
    }
}
