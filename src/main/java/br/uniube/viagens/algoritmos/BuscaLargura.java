package br.uniube.viagens.algoritmos;

import br.uniube.viagens.modelo.Aresta;
import br.uniube.viagens.modelo.Grafo;
import br.uniube.viagens.modelo.Restricoes;
import br.uniube.viagens.modelo.Rota;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * Busca em Largura (BFS).
 *
 * <p>Explora o grafo em "camadas": primeiro todos os vizinhos da origem, depois os
 * vizinhos dos vizinhos, e assim por diante, usando uma <b>fila</b> (FIFO). Por isso
 * o primeiro caminho que chega ao destino é o de <b>menor número de trechos</b>
 * (paradas), independentemente de distância, tempo ou custo.
 *
 * <p>Complexidade: O(V + E) tempo e O(V) espaço.
 */
public final class BuscaLargura {

    private BuscaLargura() {
    }

    /** Rota com o menor número de trechos entre origem e destino, respeitando as restrições. */
    public static ResultadoBusca menorNumeroDeTrechos(Grafo grafo, int origem, int destino, Restricoes restricoes) {
        // valida os índices (lança IllegalArgumentException se inválidos)
        grafo.localidade(origem);
        grafo.localidade(destino);

        List<Integer> ordemVisita = new ArrayList<>();
        if (!restricoes.permiteLocalidade(origem) || !restricoes.permiteLocalidade(destino)) {
            return ResultadoBusca.de(Optional.empty(), ordemVisita);
        }

        int n = grafo.numeroLocalidades();
        boolean[] visitado = new boolean[n];
        Aresta[] arestaPai = new Aresta[n];
        Deque<Integer> fila = new ArrayDeque<>();

        visitado[origem] = true;
        fila.add(origem);

        while (!fila.isEmpty()) {
            int u = fila.poll();
            ordemVisita.add(u);
            if (u == destino) {
                break;
            }
            for (Aresta aresta : grafo.vizinhos(u)) {
                int v = aresta.destino();
                if (!restricoes.permite(aresta) || visitado[v]) {
                    continue;
                }
                visitado[v] = true;
                arestaPai[v] = aresta;
                fila.add(v);
            }
        }

        if (!visitado[destino]) {
            return ResultadoBusca.de(Optional.empty(), ordemVisita);
        }
        return ResultadoBusca.de(Optional.of(Rota.reconstruir(origem, destino, arestaPai)), ordemVisita);
    }

    /** Todas as localidades alcançáveis a partir da origem, na ordem da BFS. */
    public static List<Integer> alcancaveis(Grafo grafo, int origem, Restricoes restricoes) {
        grafo.localidade(origem);
        List<Integer> ordem = new ArrayList<>();
        if (!restricoes.permiteLocalidade(origem)) {
            return ordem;
        }
        boolean[] visitado = new boolean[grafo.numeroLocalidades()];
        Deque<Integer> fila = new ArrayDeque<>();
        visitado[origem] = true;
        fila.add(origem);
        while (!fila.isEmpty()) {
            int u = fila.poll();
            ordem.add(u);
            for (Aresta aresta : grafo.vizinhos(u)) {
                if (restricoes.permite(aresta) && !visitado[aresta.destino()]) {
                    visitado[aresta.destino()] = true;
                    fila.add(aresta.destino());
                }
            }
        }
        return ordem;
    }
}
