package br.uniube.viagens;

import br.uniube.viagens.modelo.Grafo;

/** Grafos pequenos e conhecidos, para testar os algoritmos com resultados calculados à mão. */
public final class GrafosDeTeste {

    private GrafosDeTeste() {
    }

    /**
     * Grafo de 6 vértices (A..F). F é isolado.
     *
     * <pre>
     *           B
     *         / | \
     *       A   |   D
     *      / \  |  / \
     *     E   \ | /   (E-D)
     *          C
     *
     * Estrada  km   min  R$
     * A-B      10   30   5
     * A-C      25   20   4
     * A-E      15   35   3
     * B-C       5   10   1
     * B-D      10   30   5
     * C-D      25   20   4
     * E-D      15   35   3
     * </pre>
     *
     * Caminhos simples de A até D (5 no total):
     * <pre>
     * A-B-D    20 km   60 min  R$10
     * A-B-C-D  40 km   60 min  R$10
     * A-C-D    50 km   40 min  R$ 8   &lt;- menor tempo
     * A-C-B-D  40 km   60 min  R$10
     * A-E-D    30 km   70 min  R$ 6   &lt;- menor custo
     * </pre>
     * O menor caminho em distância é A-B-D (20 km), e há vários caminhos com 2 trechos.
     */
    public static Grafo losango() {
        Grafo g = new Grafo();
        for (String nome : new String[] {"A", "B", "C", "D", "E", "F"}) {
            g.adicionarLocalidade(nome, "XX");
        }
        g.adicionarEstrada("A", "B", 10, 30, 5);
        g.adicionarEstrada("A", "C", 25, 20, 4);
        g.adicionarEstrada("A", "E", 15, 35, 3);
        g.adicionarEstrada("B", "C", 5, 10, 1);
        g.adicionarEstrada("B", "D", 10, 30, 5);
        g.adicionarEstrada("C", "D", 25, 20, 4);
        g.adicionarEstrada("E", "D", 15, 35, 3);
        return g;
    }
}
