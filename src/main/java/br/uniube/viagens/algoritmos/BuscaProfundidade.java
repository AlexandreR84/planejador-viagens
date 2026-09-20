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
import java.util.function.ToDoubleFunction;

/**
 * Busca em Profundidade (DFS), em três usos:
 *
 * <ol>
 *   <li>{@link #buscar}: DFS clássica, com vetor de visitados global. Acha <i>um</i> caminho
 *       (não necessariamente o melhor) em O(V + E).</li>
 *   <li>{@link #todosOsCaminhos}: DFS com <b>backtracking</b>. Ao voltar de um vértice ele é
 *       "desmarcado", o que permite enumerar todos os caminhos simples (sem repetir vértice)
 *       entre origem e destino — as rotas alternativas. No pior caso é exponencial.</li>
 *   <li>{@link #melhorRotaComLimites} e {@link #menosTrechosComLimites}: enumeração com poda
 *       (<i>branch and bound</i>), usadas quando existem limites de distância, tempo ou custo.
 *       É o problema do "caminho mínimo com restrição de recurso" (NP-difícil em geral; viável
 *       aqui por causa do tamanho da rede).</li>
 * </ol>
 *
 * A pilha de chamadas da recursão faz o papel da pilha (LIFO) da DFS.
 */
public final class BuscaProfundidade {

    private BuscaProfundidade() {
    }

    // ------------------------------------------------------------ 1) DFS clássica

    /**
     * DFS clássica: devolve o primeiro caminho encontrado e a ordem de visita.
     *
     * <p><b>Atenção:</b> respeita bloqueios de localidade e de trecho, mas <b>não</b> os limites
     * de distância, tempo e custo. O vetor de visitados é global — uma vez que um vértice é
     * marcado, ele não volta a ser explorado —, então não há como desfazer uma escolha que
     * estourou um limite. Respeitar limites exige backtracking: veja {@link #todosOsCaminhos}
     * e {@link #menosTrechosComLimites}. O {@code Planejador} cuida de escolher o método certo.
     */
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
     *
     * <p>A ordem em que os caminhos saem é a da DFS, não a de qualidade: quem precisa dos
     * melhores primeiro deve ordenar depois, ciente de que a ordenação só é global enquanto
     * o teto {@code maximo} não for atingido.
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

    // ------------------------------------------- 3) otimização com limites (poda)

    /** Melhor rota no critério dado, entre as que respeitam bloqueios e limites. */
    public static ResultadoBusca melhorRotaComLimites(Grafo grafo, int origem, int destino,
                                                     Criterio criterio, Restricoes restricoes) {
        return otimizar(grafo, origem, destino, criterio::peso, restricoes,
                "Busca em profundidade com poda (branch and bound).");
    }

    /**
     * Rota com o <b>menor número de trechos</b> entre as que respeitam bloqueios e limites.
     *
     * <p>É a versão da pergunta da BFS para quando há limites. A BFS devolve uma rota de
     * número mínimo de trechos, mas não escolhe <i>qual</i> delas: se aquela violar um limite,
     * pode existir outra, do mesmo tamanho, que caiba. Este método busca entre todas.
     */
    public static ResultadoBusca menosTrechosComLimites(Grafo grafo, int origem, int destino,
                                                       Restricoes restricoes) {
        return otimizar(grafo, origem, destino, aresta -> 1.0, restricoes,
                "Busca em profundidade com poda, minimizando o número de trechos.");
    }

    private static ResultadoBusca otimizar(Grafo grafo, int origem, int destino,
                                           ToDoubleFunction<Aresta> peso, Restricoes restricoes,
                                           String observacao) {
        grafo.localidade(origem);
        grafo.localidade(destino);

        if (!restricoes.permiteLocalidade(origem) || !restricoes.permiteLocalidade(destino)) {
            return new ResultadoBusca(Optional.empty(), List.of(), observacao);
        }
        Melhor melhor = new Melhor();
        boolean[] noCaminho = new boolean[grafo.numeroLocalidades()];
        ramificar(grafo, origem, origem, destino, peso, restricoes, noCaminho, new ArrayDeque<>(),
                0, 0, 0, 0, melhor);
        return new ResultadoBusca(Optional.ofNullable(melhor.rota), List.of(), observacao);
    }

    private static final class Melhor {
        double valor = Double.POSITIVE_INFINITY;
        Rota rota;
    }

    private static void ramificar(Grafo grafo, int origem, int u, int destino,
                                  ToDoubleFunction<Aresta> peso, Restricoes restricoes,
                                  boolean[] noCaminho, Deque<Aresta> caminho,
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
            ramificar(grafo, origem, v, destino, peso, restricoes, noCaminho, caminho,
                    novoKm, novoMin, novoCusto, valorAtual + peso.applyAsDouble(aresta), melhor);
            caminho.removeLast();
        }
        noCaminho[u] = false;
    }
}
