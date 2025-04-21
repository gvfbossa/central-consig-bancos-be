package com.centralconsig.crawler_bancos.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(of = {"username", "password"})
@NoArgsConstructor
public class UsuarioLoginQueroMaisCredito {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String username;
    private String password;

    public UsuarioLoginQueroMaisCredito(String username, String password) {
        this.password = password;
        this.username = username;
    }

}
