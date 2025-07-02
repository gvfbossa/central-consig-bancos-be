package com.centralconsig.crawler_bancos.application.service.utils;

import lombok.Getter;

@Getter
public class ExportedFile {
    private final byte[] dados;
    private final String nomeArquivo;

    public ExportedFile(byte[] dados, String nomeArquivo) {
        this.dados = dados;
        this.nomeArquivo = nomeArquivo;
    }

}