package com.centralconsig.crawler_bancos.application.service;

import com.centralconsig.crawler_bancos.application.dto.response.ClienteResponseDTO;
import com.centralconsig.crawler_bancos.application.mapper.ClienteMapper;
import com.centralconsig.crawler_bancos.domain.entity.Cliente;
import com.centralconsig.crawler_bancos.domain.entity.HistoricoConsulta;
import com.centralconsig.crawler_bancos.domain.entity.Vinculo;
import com.centralconsig.crawler_bancos.domain.repository.ClienteRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final VinculoService vinculoService;

    public ClienteService(ClienteRepository clienteRepository, VinculoService vinculoService) {
        this.clienteRepository = clienteRepository;
        this.vinculoService = vinculoService;
    }

    public Cliente salvarOuAtualizarCliente(Cliente novoCliente) {
        Optional<Cliente> clienteExistenteOpt = clienteRepository.findByCpf(novoCliente.getCpf());

        if (clienteExistenteOpt.isPresent()) {
            Cliente existente = clienteExistenteOpt.get();

            if (!Objects.equals(existente.isCasa(), novoCliente.isCasa())) {
                existente.setCasa(novoCliente.isCasa());
            }

            for (Vinculo vinculoNovo : novoCliente.getVinculos()) {
                Optional<Vinculo> vinculoExistenteOpt = existente.getVinculos().stream()
                        .filter(v -> {
                            return v.getMatriculaPensionista().equals(vinculoNovo.getMatriculaPensionista())
                                    && Objects.equals(v.getOrgao(), vinculoNovo.getOrgao());
                        })
                        .findFirst();

                if (vinculoExistenteOpt.isPresent()) {
                    Vinculo vinculoExistente = vinculoExistenteOpt.get();

                    for (HistoricoConsulta historico : vinculoNovo.getHistoricos()) {
                        historico.setVinculo(vinculoExistente);
                        if (!vinculoExistente.getHistoricos().contains(historico)) {
                            vinculoExistente.getHistoricos().add(historico);

                            if (vinculoExistente.getHistoricos().size() > 2) {
                                vinculoExistente.getHistoricos().sort(Comparator.comparing(HistoricoConsulta::getDataConsulta));
                                vinculoExistente.getHistoricos().removeFirst();
                            }
                        }
                    }
                } else {
                    vinculoNovo.setCliente(existente);
                    existente.getVinculos().add(vinculoNovo);
                }
            }
            existente.setNome(novoCliente.getNome());
            return clienteRepository.saveAndFlush(existente);
        } else {
            return clienteRepository.saveAndFlush(novoCliente);
        }
    }

    public void salvarOuAtualizarEmLote(List<Cliente> clientesDoCsv) {
        Map<String, Cliente> mapaClientesExistentes = new HashMap<>();

        Set<String> cpfs = clientesDoCsv.stream()
                .map(Cliente::getCpf)
                .collect(Collectors.toSet());

        List<Cliente> clientesExistentes = clienteRepository.findByCpfIn(cpfs);

        for (Cliente existente : clientesExistentes) {
            mapaClientesExistentes.put(existente.getCpf(), existente);
        }

        List<Cliente> novos = new ArrayList<>();
        List<Cliente> atualizaveis = new ArrayList<>();

        for (Cliente clienteCsv : clientesDoCsv) {
            Cliente existente = mapaClientesExistentes.get(clienteCsv.getCpf());

            if (existente == null) {
                novos.add(clienteCsv);
            } else {
                boolean atualizado = false;

                if (!Objects.equals(existente.isCasa(), clienteCsv.isCasa())) {
                    existente.setCasa(clienteCsv.isCasa());
                    atualizado = true;
                }

                for (Vinculo vinculoCsv : clienteCsv.getVinculos()) {
                    boolean existeVinculo = existente.getVinculos().stream()
                            .anyMatch(v -> v.getMatriculaPensionista().equals(vinculoCsv.getMatriculaPensionista())
                                    && Objects.equals(v.getOrgao(), vinculoCsv.getOrgao()));

                    if (!existeVinculo) {
                        vinculoCsv.setCliente(existente);
                        existente.getVinculos().add(vinculoCsv);
                        atualizado = true;
                    }
                }

                if (atualizado) {
                    atualizaveis.add(existente);
                }
            }
        }

        clienteRepository.saveAll(novos);
        clienteRepository.saveAll(atualizaveis);
    }

    public List<Cliente> getAllClientes() {
        return clienteRepository.findAll();
    }

    public Cliente criarObjetoCliente(String cpf, String nome, List<Vinculo> vinculos) {
        Cliente cliente = new Cliente();
        cliente.setCpf(cpf);
        cliente.setNome(nome);
        cliente.setVinculos(vinculos);

        return cliente;
    }

    public Cliente findByCpf(String cpf) {
        return clienteRepository.findByCpf(cpf).orElseThrow();
    }

    public String removeZerosAEsquerdaMatricula(String matricula) {
        return matricula.trim().replaceFirst("^0+(?!$)", "");
    }

    public ClienteResponseDTO buscaClientePorCpfOuMatricula(String cpf, String matricula) {
        Cliente cliente = null;

        if (cpf != null) {
            cliente = clienteRepository.findByCpf(cpf).orElse(null);
        } else if (matricula != null) {
            Vinculo vinculo = vinculoService.findByMatriculaPensionista(matricula)
                    .orElseThrow(() -> new IllegalArgumentException("Matrícula não encontrada."));
            cliente = vinculo.getCliente();
        } else {
            throw new IllegalArgumentException("Informe CPF ou Matrícula para a busca");
        }
        if (cliente != null) {
            return ClienteMapper.toDto(cliente);
        } else {
            return null;
        }
    }

    public List<Cliente> clientesFiltradosPorMargem() {
        return clienteRepository.findAll().stream()
                .filter(cliente -> !cliente.isBlackList())
                .filter(cliente -> cliente.getVinculos().stream()
                        .map(Vinculo::getHistoricos)
                        .filter(historicos -> !historicos.isEmpty())
                        .map(historicos -> historicos.stream()
                                .max(Comparator.comparing(HistoricoConsulta::getDataConsulta))
                                .orElse(null))
                        .anyMatch(historico -> {
                            if (historico == null) return false;
                            String margemStr = historico.getMargemBeneficio().replace(",", ".");
                            try {
                                double margem = Double.parseDouble(margemStr);
                                return margem > 0 && !"Não Autorizado".equalsIgnoreCase(historico.getSituacaoBeneficio());
                            } catch (NumberFormatException e) {
                                return false;
                            }
                        })
                )
                .sorted(Comparator.comparingDouble(cliente -> cliente.getVinculos().stream()
                        .map(Vinculo::getHistoricos)
                        .filter(historicos -> !historicos.isEmpty())
                        .map(historicos -> historicos.stream()
                                .max(Comparator.comparing(HistoricoConsulta::getDataConsulta))
                                .orElse(null))
                        .filter(historico -> !"Não Autorizado".equalsIgnoreCase(historico.getSituacaoBeneficio()))
                        .mapToDouble(historico -> {
                            try {
                                return Double.parseDouble(historico.getMargemBeneficio().replace(",", "."));
                            } catch (NumberFormatException e) {
                                return Double.MAX_VALUE;
                            }
                        })
                        .min()
                        .orElse(Double.MAX_VALUE)))
                .collect(Collectors.toList());
    }

    public List<Cliente> getClientesCasaComVinculosEHistorico() {
        return clienteRepository.buscarClientesCasaComVinculosEHistoricos();
    }

    public List<Cliente> getClientesNaoCasaComVinculosEHistorico() {
        return clienteRepository.buscarClientesNaoCasaComVinculosEHistorico();
    }

}
