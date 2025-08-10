package br.com.alura.desafio2_literalura.service;

public interface IConverteDados {

    <T> T obterDados(String json, Class<T> classe);

}

