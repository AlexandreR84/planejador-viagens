package br.uniube.viagens.algoritmos;

import br.uniube.viagens.GrafosDeTeste;
import br.uniube.viagens.modelo.Criterio;
import br.uniube.viagens.modelo.Grafo;
import br.uniube.viagens.modelo.Restricoes;
import br.uniube.viagens.modelo.Rota;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuscaProfundidadeTest {

    private final Grafo g = GrafosDeTeste.losango();
    private final int a = g.indiceDe("A");
    private final int b = g.indiceDe("B");
    private final int d = g.indiceDe("D");
    private final int f = g.indiceDe("F");

    @Test
    void dfsClassicaAchaUmCaminhoValido() {
        ResultadoBusca r = BuscaProfundidade.buscar(g, a, d, Restricoes.para(g));

        Rota rota = r.rota().orElseThrow();
        assertEquals(a, rota.origem());
        assertEquals(d, rota.destino());
        assertEquals(a, r.ordemVisita().get(0));
        // cada vértice é visitado no máximo uma vez
        assertEquals(new HashSet<>(r.ordemVisita()).size(), r.ordemVisita().size());
    }

    @Test
    void dfsClassicaSemCaminhoRetornaVazio() {
        assertFalse(BuscaProfundidade.buscar(g, a, f, Restricoes.para(g)).encontrada());
    }

    @Test
    void enumeraTodosOsCincoCaminhosSimplesDeAAteD() {
        List<Rota> rotas = BuscaProfundidade.todosOsCaminhos(g, a, d, Restricoes.para(g), 100);

        assertEquals(5, rotas.size());
        Set<List<Integer>> distintos = new HashSet<>();
        for (Rota rota : rotas) {
            distintos.add(rota.vertices());
            assertEquals(new HashSet<>(rota.vertices()).size(), rota.vertices().size(),
                    "caminho simples não repete vértice (ciclos são evitados)");
        }
        assertEquals(5, distintos.size(), "sem caminhos repetidos");
    }

    @Test
    void respeitaOLimiteDeCaminhosEnumerados() {
        assertEquals(2, BuscaProfundidade.todosOsCaminhos(g, a, d, Restricoes.para(g), 2).size());
        assertEquals(0, BuscaProfundidade.todosOsCaminhos(g, a, d, Restricoes.para(g), 0).size());
    }

    @Test
    void bloqueioDeLocalidadeRemoveCaminhos() {
        // Sem B restam A-C-D e A-E-D.
        Restricoes r = Restricoes.para(g).bloquearLocalidade("B");
        List<Rota> rotas = BuscaProfundidade.todosOsCaminhos(g, a, d, r, 100);

        assertEquals(2, rotas.size());
        assertTrue(rotas.stream().noneMatch(rota -> rota.vertices().contains(b)));
    }

    @Test
    void limiteDeCustoFiltraCaminhos() {
        // Custos: 10, 10, 8, 10, 6  ->  com máximo de R$ 8 sobram A-C-D e A-E-D.
        Restricoes r = Restricoes.para(g).custoMaximo(8.0);
        List<Rota> rotas = BuscaProfundidade.todosOsCaminhos(g, a, d, r, 100);

        assertEquals(2, rotas.size());
        assertTrue(rotas.stream().allMatch(rota -> rota.custoReais() <= 8.0));
    }

    @Test
    void limiteDeDistanciaEInclusivo() {
        // Distâncias: 20, 40, 50, 40, 30  ->  com máximo de 30 km sobram A-B-D (20) e A-E-D (30).
        Restricoes r = Restricoes.para(g).distanciaMaxima(30.0);
        assertEquals(2, BuscaProfundidade.todosOsCaminhos(g, a, d, r, 100).size());
    }

    @Test
    void origemIgualDestinoTemUmCaminhoTrivial() {
        List<Rota> rotas = BuscaProfundidade.todosOsCaminhos(g, a, a, Restricoes.para(g), 10);
        assertEquals(1, rotas.size());
        assertEquals(0, rotas.get(0).trechos());
    }

    @Test
    void melhorRotaComLimitesEscolheOMelhorDentroDosLimites() {
        // Menor tempo é A-C-D (40 min, R$ 8). Com custo máximo de R$ 7 só sobra A-E-D (R$ 6).
        Restricoes r = Restricoes.para(g).custoMaximo(7.0);
        Rota rota = BuscaProfundidade.melhorRotaComLimites(g, a, d, Criterio.TEMPO, r).rota().orElseThrow();

        assertEquals(List.of(a, g.indiceDe("E"), d), rota.vertices());
    }

    @Test
    void melhorRotaComLimitesImpossiveisNaoRetornaRota() {
        Restricoes r = Restricoes.para(g).custoMaximo(1.0);
        assertFalse(BuscaProfundidade.melhorRotaComLimites(g, a, d, Criterio.CUSTO, r).encontrada());
    }
}
