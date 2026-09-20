package br.uniube.viagens.modelo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Resultado de uma busca: a sequência de vértices visitados da origem ao destino
 * e os totais das três métricas ao longo do caminho.
 *
 * @param vertices    índices dos vértices, da origem ao destino (inclusive)
 * @param distanciaKm soma das distâncias
 * @param tempoMin    soma dos tempos
 * @param custoReais  soma dos custos
 */
public record Rota(List<Integer> vertices, double distanciaKm, int tempoMin, double custoReais) {

    public Rota {
        vertices = List.copyOf(vertices);
    }

    /** Monta a rota a partir da origem e da sequência de arestas percorridas. */
    public static Rota deArestas(int origem, List<Aresta> arestas) {
        List<Integer> vertices = new ArrayList<>(arestas.size() + 1);
        vertices.add(origem);
        double km = 0;
        int min = 0;
        double custo = 0;
        for (Aresta a : arestas) {
            vertices.add(a.destino());
            km += a.distanciaKm();
            min += a.tempoMin();
            custo += a.custoReais();
        }
        return new Rota(vertices, arredondar(km), min, arredondar(custo));
    }

    /**
     * Reconstrói o caminho seguindo o vetor de "aresta pai" de trás para a frente
     * (destino -> origem). Só deve ser chamado se o destino foi alcançado.
     */
    public static Rota reconstruir(int origem, int destino, Aresta[] arestaPai) {
        LinkedList<Aresta> caminho = new LinkedList<>();
        int atual = destino;
        while (atual != origem) {
            Aresta a = arestaPai[atual];
            caminho.addFirst(a);
            atual = a.origem();
        }
        return deArestas(origem, caminho);
    }

    public int origem() {
        return vertices.get(0);
    }

    public int destino() {
        return vertices.get(vertices.size() - 1);
    }

    /** Número de trechos (arestas) percorridos. */
    public int trechos() {
        return vertices.size() - 1;
    }

    /** Valor total da rota no critério informado. */
    public double valor(Criterio criterio) {
        return switch (criterio) {
            case DISTANCIA -> distanciaKm;
            case TEMPO -> tempoMin;
            case CUSTO -> custoReais;
        };
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
