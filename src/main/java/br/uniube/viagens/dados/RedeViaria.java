package br.uniube.viagens.dados;

import br.uniube.viagens.modelo.Grafo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Carrega o grafo a partir de dois arquivos CSV (separador ';', codificação UTF-8):
 *
 * <pre>
 * localidades.csv:  nome;uf;latitude;longitude
 * estradas.csv:     origem;destino;distancia_km;tempo_min;pedagio_rs
 * </pre>
 *
 * Linhas em branco e linhas iniciadas por '#' são ignoradas. O custo de cada estrada
 * é calculado como {@code distancia_km * CUSTO_COMBUSTIVEL_POR_KM + pedagio_rs}.
 *
 * Trocar os CSVs é a forma de aplicar o planejador a outra região sem alterar código.
 */
public final class RedeViaria {

    /** Custo médio de combustível por km rodado (carro de passeio), em reais. */
    public static final double CUSTO_COMBUSTIVEL_POR_KM = 0.55;

    private static final String RECURSO_LOCALIDADES = "/dados/localidades.csv";
    private static final String RECURSO_ESTRADAS = "/dados/estradas.csv";

    private RedeViaria() {
    }

    /** Carrega a rede padrão que acompanha o projeto (src/main/resources/dados). */
    public static Grafo carregarPadrao() {
        try (InputStream localidades = RedeViaria.class.getResourceAsStream(RECURSO_LOCALIDADES);
             InputStream estradas = RedeViaria.class.getResourceAsStream(RECURSO_ESTRADAS)) {
            if (localidades == null || estradas == null) {
                throw new IllegalStateException("Arquivos de dados não encontrados no classpath (/dados).");
            }
            return carregar(localidades, estradas);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Carrega uma rede a partir de arquivos no disco. */
    public static Grafo carregar(Path localidades, Path estradas) throws IOException {
        try (InputStream l = Files.newInputStream(localidades); InputStream e = Files.newInputStream(estradas)) {
            return carregar(l, e);
        }
    }

    public static Grafo carregar(InputStream localidades, InputStream estradas) throws IOException {
        Grafo grafo = new Grafo();

        int linhaNumero = 0;
        for (String linha : linhasUteis(localidades)) {
            linhaNumero++;
            String[] c = campos(linha, 4, "localidades.csv", linhaNumero);
            try {
                grafo.adicionarLocalidade(c[0], c[1], Double.parseDouble(c[2]), Double.parseDouble(c[3]));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("localidades.csv, registro " + linhaNumero + ": " + e.getMessage(), e);
            }
        }

        linhaNumero = 0;
        for (String linha : linhasUteis(estradas)) {
            linhaNumero++;
            String[] c = campos(linha, 5, "estradas.csv", linhaNumero);
            try {
                double km = Double.parseDouble(c[2]);
                int min = Integer.parseInt(c[3]);
                double pedagio = Double.parseDouble(c[4]);
                double custo = Math.round((km * CUSTO_COMBUSTIVEL_POR_KM + pedagio) * 100.0) / 100.0;
                grafo.adicionarEstrada(c[0], c[1], km, min, custo);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("estradas.csv, registro " + linhaNumero + ": " + e.getMessage(), e);
            }
        }
        return grafo;
    }

    // -------------------------------------------------------------------- interno

    private static java.util.List<String> linhasUteis(InputStream entrada) throws IOException {
        java.util.List<String> linhas = new java.util.ArrayList<>();
        BufferedReader leitor = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8));
        String linha;
        while ((linha = leitor.readLine()) != null) {
            String limpa = linha.replace("﻿", "").trim(); // remove BOM, se houver
            if (!limpa.isEmpty() && !limpa.startsWith("#")) {
                linhas.add(limpa);
            }
        }
        return linhas;
    }

    private static String[] campos(String linha, int esperado, String arquivo, int numero) {
        String[] partes = linha.split(";", -1);
        if (partes.length != esperado) {
            throw new IllegalArgumentException(arquivo + ", registro " + numero + ": esperados "
                    + esperado + " campos separados por ';', encontrados " + partes.length + ".");
        }
        for (int i = 0; i < partes.length; i++) {
            partes[i] = partes[i].trim();
        }
        return partes;
    }
}
