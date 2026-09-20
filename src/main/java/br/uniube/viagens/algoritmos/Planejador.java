package br.uniube.viagens.algoritmos;

import br.uniube.viagens.modelo.Criterio;
import br.uniube.viagens.modelo.Grafo;
import br.uniube.viagens.modelo.Restricoes;
import br.uniube.viagens.modelo.Rota;

import java.util.Comparator;
import java.util.List;

/**
 * Fachada do núcleo: recebe nomes de localidades (como o usuário digita), aplica as
 * restrições e escolhe o algoritmo adequado para cada pergunta.
 *
 * <ul>
 *   <li>"Qual a melhor rota?" &rarr; Dijkstra (e busca com poda se houver limites violados);</li>
 *   <li>"Qual a rota com menos paradas?" &rarr; BFS;</li>
 *   <li>"Quais caminhos existem?" &rarr; DFS com backtracking.</li>
 * </ul>
 */
public final class Planejador {

    /** Teto de caminhos enumerados pela DFS, para não explodir em grafos densos. */
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

    /** Rota com o menor número de trechos (BFS). Se ela violar limites, informa no aviso. */
    public ResultadoBusca menosTrechos(String origem, String destino, Restricoes restricoes) {
        int o = grafo.indiceDe(origem);
        int d = grafo.indiceDe(destino);

        ResultadoBusca bfs = BuscaLargura.menorNumeroDeTrechos(grafo, o, d, restricoes);
        if (bfs.rota().isPresent() && !restricoes.atende(bfs.rota().get())) {
            return new ResultadoBusca(java.util.Optional.empty(), bfs.ordemVisita(),
                    "A rota com menos trechos ultrapassa os limites informados.");
        }
        return bfs;
    }

    /** DFS clássica: um caminho qualquer (o primeiro encontrado) e a ordem de visita. */
    public ResultadoBusca primeiroCaminho(String origem, String destino, Restricoes restricoes) {
        return BuscaProfundidade.buscar(grafo, grafo.indiceDe(origem), grafo.indiceDe(destino), restricoes);
    }

    /** Caminhos alternativos (DFS com backtracking), ordenados pelo critério. */
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
