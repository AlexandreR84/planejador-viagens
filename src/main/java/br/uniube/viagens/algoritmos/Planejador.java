package br.uniube.viagens.algoritmos;

import br.uniube.viagens.modelo.Criterio;
import br.uniube.viagens.modelo.Grafo;
import br.uniube.viagens.modelo.Restricoes;
import br.uniube.viagens.modelo.Rota;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Fachada do núcleo: recebe nomes de localidades (como o usuário digita), aplica as
 * restrições e escolhe o algoritmo adequado para cada pergunta.
 *
 * <ul>
 *   <li>"Qual a melhor rota?" &rarr; Dijkstra;</li>
 *   <li>"Qual a rota com menos paradas?" &rarr; BFS;</li>
 *   <li>"Quais caminhos existem?" &rarr; DFS com backtracking.</li>
 * </ul>
 *
 * <p><b>Papel dos limites.</b> BFS e Dijkstra são ótimos para as suas perguntas, mas nenhum
 * dos dois sabe lidar com um teto de distância, tempo ou custo: ambos fecham um vértice pelo
 * melhor valor encontrado e nunca reconsideram. Quando a rota que eles devolvem viola um
 * limite, esta classe recorre à busca em profundidade com poda, que examina as alternativas.
 * É a mesma troca nos dois casos — sai a garantia de tempo polinomial, entra a de corretude.
 */
public final class Planejador {

    /**
     * Teto de caminhos enumerados pela DFS em {@link #alternativas}, para não explodir em
     * grafos densos. Enquanto ele não é atingido, a ordenação por critério é global; se for,
     * o resultado passa a ser "os melhores entre os primeiros encontrados", porque o corte
     * acontece durante a enumeração, antes da ordenação. Na rede que acompanha o projeto o
     * par com mais caminhos simples tem 108, bem longe do teto.
     */
    public static final int LIMITE_ENUMERACAO = 10_000;

    private final Grafo grafo;

    public Planejador(Grafo grafo) {
        this.grafo = grafo;
    }

    public Grafo grafo() {
        return grafo;
    }

    /** Novo conjunto de restrições vazio, ligado a este grafo. */
    public Restricoes novasRestricoes() {
        return Restricoes.para(grafo);
    }

    /** Melhor rota segundo o critério, respeitando bloqueios e limites. */
    public ResultadoBusca melhorRota(String origem, String destino, Criterio criterio, Restricoes restricoes) {
        int o = grafo.indiceDe(origem);
        int d = grafo.indiceDe(destino);

        ResultadoBusca dijkstra = Dijkstra.menorCaminho(grafo, o, d, criterio, restricoes);
        if (dijkstra.rota().isEmpty() || restricoes.atende(dijkstra.rota().get())) {
            return dijkstra;
        }

        // O menor caminho no critério escolhido estoura um limite de outra métrica.
        // Dijkstra sozinho não resolve isso; recorre-se à busca exaustiva com poda.
        ResultadoBusca exato = BuscaProfundidade.melhorRotaComLimites(grafo, o, d, criterio, restricoes);
        String aviso = exato.encontrada()
                ? "A rota mais curta pelo critério violava os limites; foi usada busca exaustiva com poda."
                : "Nenhuma rota atende aos limites informados.";
        return new ResultadoBusca(exato.rota(), dijkstra.ordemVisita(), aviso);
    }

    /**
     * Rota com o menor número de trechos (BFS).
     *
     * <p>A BFS devolve <i>uma</i> das rotas de número mínimo de trechos — qual delas depende da
     * ordem da lista de adjacência. Se essa rota violar um limite, pode existir outra, do mesmo
     * tamanho, que caiba; a BFS não as procura, então a busca com poda assume e minimiza o
     * número de trechos entre as rotas válidas.
     */
    public ResultadoBusca menosTrechos(String origem, String destino, Restricoes restricoes) {
        int o = grafo.indiceDe(origem);
        int d = grafo.indiceDe(destino);

        ResultadoBusca bfs = BuscaLargura.menorNumeroDeTrechos(grafo, o, d, restricoes);
        if (bfs.rota().isEmpty() || restricoes.atende(bfs.rota().get())) {
            return bfs; // sem rota mesmo ignorando limites, ou a rota da BFS já os respeita
        }

        ResultadoBusca exato = BuscaProfundidade.menosTrechosComLimites(grafo, o, d, restricoes);
        String aviso = exato.encontrada()
                ? "A rota que a BFS encontrou violava os limites; foi escolhida outra, de mesmo número "
                        + "de trechos ou o menor possível, que os respeita."
                : "Nenhuma rota atende aos limites informados.";
        return new ResultadoBusca(exato.rota(), bfs.ordemVisita(), aviso);
    }

    /**
     * DFS clássica: um caminho qualquer (o primeiro encontrado) e a ordem de visita.
     *
     * <p>A DFS clássica não considera os limites (veja {@link BuscaProfundidade#buscar}), então,
     * quando há algum definido, a rota devolvida vem da DFS com backtracking. A ordem de visita
     * continua sendo a da DFS clássica, que é o que interessa mostrar sobre o algoritmo.
     */
    public ResultadoBusca primeiroCaminho(String origem, String destino, Restricoes restricoes) {
        int o = grafo.indiceDe(origem);
        int d = grafo.indiceDe(destino);

        ResultadoBusca dfs = BuscaProfundidade.buscar(grafo, o, d, restricoes);
        if (!restricoes.temLimites()) {
            return dfs;
        }

        List<Rota> primeira = BuscaProfundidade.todosOsCaminhos(grafo, o, d, restricoes, 1);
        String aviso = primeira.isEmpty()
                ? "Nenhum caminho atende aos limites informados."
                : "Há limites definidos: a rota vem da DFS com backtracking, pois a DFS clássica "
                        + "não sabe desfazer uma escolha que estourou um limite.";
        return new ResultadoBusca(
                primeira.isEmpty() ? Optional.empty() : Optional.of(primeira.get(0)),
                dfs.ordemVisita(), aviso);
    }

    /**
     * Caminhos alternativos (DFS com backtracking), ordenados pelo critério.
     * Veja {@link #LIMITE_ENUMERACAO} sobre o alcance dessa ordenação.
     */
    public List<Rota> alternativas(String origem, String destino, Criterio ordenarPor,
                                   Restricoes restricoes, int maximo) {
        int o = grafo.indiceDe(origem);
        int d = grafo.indiceDe(destino);
        return BuscaProfundidade.todosOsCaminhos(grafo, o, d, restricoes, LIMITE_ENUMERACAO).stream()
                .sorted(Comparator.comparingDouble((Rota r) -> r.valor(ordenarPor)))
                .limit(Math.max(0, maximo))
                .toList();
    }

    /** Localidades alcançáveis a partir da origem com as restrições atuais. */
    public List<Integer> alcancaveis(String origem, Restricoes restricoes) {
        return BuscaLargura.alcancaveis(grafo, grafo.indiceDe(origem), restricoes);
    }
}
