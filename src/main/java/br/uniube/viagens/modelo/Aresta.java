package br.uniube.viagens.modelo;

/**
 * Aresta do grafo: um trecho de estrada entre duas localidades.
 * Cada aresta carrega as três métricas do problema (distância, tempo e custo);
 * o {@link Criterio} escolhido pelo usuário decide qual delas vira o peso.
 *
 * <p>As estradas são de mão dupla: o {@link Grafo} guarda uma aresta em cada sentido.
 *
 * @param origem      índice do vértice de partida
 * @param destino     índice do vértice de chegada
 * @param distanciaKm distância do trecho em quilômetros
 * @param tempoMin    tempo de deslocamento em minutos
 * @param custoReais  custo estimado (combustível + pedágio) em reais
 */
public record Aresta(int origem, int destino, double distanciaKm, int tempoMin, double custoReais) {

    /** Peso da aresta segundo o critério de otimização escolhido. */
    public double peso(Criterio criterio) {
        return criterio.peso(this);
    }
}
