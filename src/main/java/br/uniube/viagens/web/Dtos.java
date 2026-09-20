package br.uniube.viagens.web;

import java.util.List;

/** Objetos trocados com o front-end em JSON. */
public final class Dtos {

    private Dtos() {
    }

    /**
     * Corpo do POST /api/rotas.
     *
     * @param algoritmo DIJKSTRA (padrão), BFS ou DFS
     * @param criterio  DISTANCIA (padrão), TEMPO ou CUSTO
     * @param maximo    quantidade máxima de caminhos devolvidos pela DFS (padrão 10)
     */
    public record RotaRequisicao(String origem, String destino, String algoritmo, String criterio,
                                 RestricoesDto restricoes, Integer maximo) {
    }

    /** Restrições enviadas a cada requisição (a API não guarda estado entre chamadas). */
    public record RestricoesDto(List<String> localidadesBloqueadas, List<List<String>> trechosBloqueados,
                                Double distanciaMaximaKm, Integer tempoMaximoMin, Double custoMaximo) {
    }

    public record RotaDto(List<String> localidades, double distanciaKm, int tempoMin,
                          double custoReais, int trechos) {
    }

    public record RotaResposta(String algoritmo, String criterio, boolean encontrada,
                               List<RotaDto> rotas, List<String> ordemVisita, String mensagem) {
    }

    public record LocalidadeDto(String nome, String uf, double latitude, double longitude) {
    }

    public record EstradaDto(String origem, String destino, double distanciaKm, int tempoMin, double custoReais) {
    }

    public record ErroDto(String erro) {
    }
}
