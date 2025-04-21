package com.centralconsig.crawler_bancos.domain.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Vinculo {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    @JsonBackReference
    private Cliente cliente;

    private String tipoVinculo;
    private String orgao;
    private String matriculaPensionista;
    private String matriculaInstituidor;

    @OneToMany(mappedBy = "vinculo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<HistoricoConsulta> historicos = new ArrayList<>();

    public Vinculo(String tipoVinculo, String orgao, String matriculaPensionista, String matriculaInstituidor, Cliente cliente) {
        this.tipoVinculo = tipoVinculo;
        this.orgao = orgao;
        this.matriculaPensionista = matriculaPensionista;
        this.matriculaInstituidor = matriculaInstituidor;
        this.cliente = cliente;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Vinculo vinculo = (Vinculo) o;
        return Objects.equals(cliente, vinculo.cliente) &&
                Objects.equals(matriculaPensionista, vinculo.matriculaPensionista);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cliente, matriculaPensionista);
    }

}
