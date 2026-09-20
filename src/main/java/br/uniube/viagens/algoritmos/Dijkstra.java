package br.uniube.viagens.algoritmos;

import br.uniube.viagens.modelo.Aresta;
import br.uniube.viagens.modelo.Criterio;
import br.uniube.viagens.modelo.Grafo;
import br.uniube.viagens.modelo.Restricoes;
import br.uniube.viagens.modelo.Rota;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * Algoritmo de Dijkstra: caminho de menor peso em grafo com pesos não negativos.
 *
 * <p>A cada passo, retira da <b>fila de prioridade</b> (heap binário) o vértice ainda não
 * fechado com menor distância acumulada e tenta melhorar (relaxar) as distâncias de seus
 * vizinhos. O peso usado é o {@link Criterio} escolhido: distância, tempo ou custo.
 *
 * <p>Aqui a fila é do tipo "lazy": em vez de reduzir a chave de um vértice já enfileirado,
 * insere-se uma nova entrada e as antigas são ignoradas ao sair (vértice já fechado).
 *
 * <p>Complexidade: O((V + E) log V) com heap binário.
 *
 * <p>Diferente da BFS, que minimiza o número de trechos, Dijkstra minimiza a soma dos pesos.
 */
public final class Dijkstra {

    private Dijkstra() {
    }

    private record Estado(int vertice, double acumulado) {
    }

    public static ResultadoBusca menorCaminho(Grafo grafo, int origem, int destino,
                                              Criterio criterio, Restricoes restricoes) {
        grafo.localidade(origem);
        grafo.localidade(destino);

        List<Integer> ordemFechamento = new ArrayList<>();
        if (!restricoes.permiteLocalidade(origem) || !restricoes.permiteLocalidade(destino)) {
            return ResultadoBusca.de(Optional.empty(), ordemFechamento);
        }

        int n = grafo.numeroLocalidades();
        double[] distancia = new double[n];
        Arrays.fill(distancia, Double.POSITIVE_INFINITY);
        Aresta[] arestaPai = new Aresta[n];
        boolean[] fechado = new boolean[n];

        PriorityQueue<Estado> fila = new PriorityQueue<>(Comparator.comparingDouble(Estado::acumulado));
        distancia[origem] = 0;
        fila.add(new Estado(origem, 0));

        while (!fila.isEmpty()) {
            Estado atual = fila.poll();
            int u = atual.vertice();
            if (fechado[u]) {
                continue; // entrada antiga da fila
            }
            fechado[u] = true;
            ordemFechamento.add(u);
            if (u == destino) {
                break;
            }
            for (Aresta aresta : grafo.vizinhos(u)) {
                int v = aresta.destino();
                if (!restricoes.permite(aresta) || fechado[v]) {
                    continue;
                }
                double candidato = distancia[u] + aresta.peso(criterio);
                if (candidato < distancia[v]) { // relaxamento
                    distancia[v] = candidato;
                    arestaPai[v] = aresta;
                    fila.add(new Estado(v, candidato));
                }
            }
        }

        if (!fechado[destino]) {
            return ResultadoBusca.de(Optional.empty(), ordemFechamento);
        }
        return ResultadoBusca.de(Optional.of(Rota.reconstruir(origem, destino, arestaPai)), ordemFechamento);
    }
}
