package br.uniube.viagens.algoritmos;

import br.uniube.viagens.modelo.Aresta;
import br.uniube.viagens.modelo.Criterio;
import br.uniube.viagens.modelo.Grafo;
import br.uniube.viagens.modelo.Restricoes;
import br.uniube.viagens.modelo.Rota;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * Busca em Profundidade (DFS), em três usos:
 *
 * <ol>
 *   <li>{@link #buscar}: DFS clássica, com vetor de visitados global. Acha <i>um</i> caminho
 *       (não necessariamente o melhor). O(V + E).</li>
 *   <li>{@link #todosOsCaminhos}: DFS com <b>backtracking</b>. Ao voltar de um vértice ele é
 *       "desmarcado", o que permite enumerar todos os caminhos simples (sem repetir vértice)
 *       entre origem e destino — as rotas alternativas. No pior caso é exponencial.</li>
 *   <li>{@link #melhorRotaComLimites}: enumeração com poda (<i>branch and bound</i>). Usada
 *       quando o menor caminho de Dijkstra estoura algum limite de distância/tempo/custo,
 *       problema conhecido como "caminho mínimo com restrição de recurso" (NP-difícil em
 *       geral; viável aqui por causa do tamanho da rede).</li>
 * </ol>
 *
 * A pilha de chamadas da recursão faz o papel da pilha (LIFO) da DFS.
 */
public final class BuscaProfundidade {

    private BuscaProfundidade() {
    }

    // ------------------------------------------------------------ 1) DFS clássica

    public static ResultadoBusca buscar(Grafo grafo, int origem, int destino, Restricoes restricoes) {
        grafo.localidade(origem);
        grafo.localidade(destino);

        List<Integer> ordemVisita = new ArrayList<>();
        if (!restricoes.permiteLocalidade(origem) || !restricoes.permiteLocalidade(destino)) {
            return ResultadoBusca.de(Optional.empty(), ordemVisita);
        }

        int n = grafo.numeroLocalidades();
        boolean[] visitado = new boolean[n];
        Aresta[] arestaPai = new Aresta[n];
        boolean achou = visitar(grafo, origem, destino, restricoes, visitado, arestaPai, ordemVisita);

        Optional<Rota> rota = achou
                ? Optional.of(Rota.reconstruir(origem, destino, arestaPai))
                : Optional.empty();
        return ResultadoBusca.de(rota, ordemVisita);
    }

    private static boolean visitar(Grafo grafo, int u, int destino, Restricoes restricoes,
                                   boolean[] visitado, Aresta[] arestaPai, List<Integer> ordemVisita) {
        visitado[u] = true;
        ordemVisita.add(u);
        if (u == destino) {
            return true;
        }
        for (Aresta aresta : grafo.vizinhos(u)) {
            int v = aresta.destino();
            if (!restricoes.permite(aresta) || visitado[v]) {
                continue;
            }
            arestaPai[v] = aresta;
            if (visitar(grafo, v, destino, restricoes, visitado, arestaPai, ordemVisita)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------- 2) todos os caminhos (backtracking)

    /**
     * Enumera caminhos simples entre origem e destino que respeitem as restrições
     * (bloqueios e limites), parando após {@code maximo} rotas.
     */
    public static List<Rota> todosOsCaminhos(Grafo grafo, int origem, int destino,
                                             Restricoes restricoes, int maximo) {
        grafo.localidade(origem);
        grafo.localidade(destino);

        List<Rota> encontradas = new ArrayList<>();
        if (maximo <= 0 || !restricoes.permiteLocalidade(origem) || !restricoes.permiteLocalidade(destino)) {
            return encontradas;
        }
        boolean[] noCaminho = new boolean[grafo.numeroLocalidades()];
        enumerar(grafo, origem, origem, destino, restricoes, noCaminho, new ArrayDeque<>(),
                0, 0, 0, encontradas, maximo);
        return encontradas;
    }

    private static void enumerar(Grafo grafo, int origem, int u, int destino, Restricoes restricoes,
                                 boolean[] noCaminho, Deque<Aresta> caminho,
                                 double km, int minutos, double reais,
                                 List<Rota> saida, int maximo) {
        if (saida.size() >= maximo) {
            return;
        }
        if (u == destino) {
            saida.add(Rota.deArestas(origem, new ArrayList<>(caminho)));
            return;
        }
        noCaminho[u] = true;
        for (Aresta aresta : grafo.vizinhos(u)) {
            int v = aresta.destino();
            if (!restricoes.permite(aresta) || noCaminho[v]) {
                continue;
            }
            double novoKm = km + aresta.distanciaKm();
            int novoMin = minutos + aresta.tempoMin();
            double novoCusto = reais + aresta.custoReais();
            if (restricoes.excede(novoKm, novoMin, novoCusto)) {
                continue; // poda: pesos são não negativos, então não vai voltar a caber
            }
            caminho.addLast(aresta);
            enumerar(grafo, origem, v, destino, restricoes, noCaminho, caminho,
                    novoKm, novoMin, novoCusto, saida, maximo);
            caminho.removeLast(); // backtracking
        }
        noCaminho[u] = false; // backtracking
    }

    // ------------------------------------------- 3) melhor rota com limites (poda)

    /** Melhor rota no critério dado, entre as que respeitam bloqueios e limites. */
    public static ResultadoBusca melhorRotaComLimites(Grafo grafo, int origem, int destino,
                                                     Criterio criterio, Restricoes restricoes) {
        grafo.localidade(origem);
        grafo.localidade(destino);

        if (!restricoes.permiteLocalidade(origem) || !restricoes.permiteLocalidade(destino)) {
            return ResultadoBusca.de(Optional.empty(), List.of());
        }
        Melhor melhor = new Melhor();
        boolean[] noCaminho = new boolean[grafo.numeroLocalidades()];
        ramificar(grafo, origem, origem, destino, criterio, restricoes, noCaminho, new ArrayDeque<>(),
                0, 0, 0, 0, melhor);
        return new ResultadoBusca(Optional.ofNullable(melhor.rota), List.of(),
                "Busca em profundidade com poda (branch and bound).");
    }

    private static final class Melhor {
        double valor = Double.POSITIVE_INFINITY;
        Rota rota;
    }

    private static void ramificar(Grafo grafo, int origem, int u, int destino, Criterio criterio,
                                  Restricoes restricoes, boolean[] noCaminho, Deque<Aresta> caminho,
                                  double km, int minutos, double reais, double valorAtual, Melhor melhor) {
        if (valorAtual >= melhor.valor) {
            return; // poda: já existe rota melhor ou igual
        }
        if (u == destino) {
            melhor.valor = valorAtual;
            melhor.rota = Rota.deArestas(origem, new ArrayList<>(caminho));
            return;
        }
        noCaminho[u] = true;
        for (Aresta aresta : grafo.vizinhos(u)) {
            int v = aresta.destino();
            if (!restricoes.permite(aresta) || noCaminho[v]) {
                continue;
            }
            double novoKm = km + aresta.distanciaKm();
            int novoMin = minutos + aresta.tempoMin();
            double novoCusto = reais + aresta.custoReais();
            if (restricoes.excede(novoKm, novoMin, novoCusto)) {
                continue;
            }
            caminho.addLast(aresta);
            ramificar(grafo, origem, v, destino, criterio, restricoes, noCaminho, caminho,
                    novoKm, novoMin, novoCusto, valorAtual + aresta.peso(criterio), melhor);
            caminho.removeLast();
        }
        noCaminho[u] = false;
    }
}
