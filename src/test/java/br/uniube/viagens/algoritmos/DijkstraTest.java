package br.uniube.viagens.algoritmos;

import br.uniube.viagens.GrafosDeTeste;
import br.uniube.viagens.modelo.Criterio;
import br.uniube.viagens.modelo.Grafo;
import br.uniube.viagens.modelo.Restricoes;
import br.uniube.viagens.modelo.Rota;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DijkstraTest {

    private final Grafo g = GrafosDeTeste.losango();
    private final int a = g.indiceDe("A");
    private final int b = g.indiceDe("B");
    private final int c = g.indiceDe("C");
    private final int d = g.indiceDe("D");
    private final int e = g.indiceDe("E");
    private final int f = g.indiceDe("F");

    private Rota melhor(Criterio criterio, Restricoes restricoes) {
        return Dijkstra.menorCaminho(g, a, d, criterio, restricoes).rota().orElseThrow();
    }

    @Test
    void criterioDistanciaEscolheAbd() {
        Rota rota = melhor(Criterio.DISTANCIA, Restricoes.para(g));
        assertEquals(List.of(a, b, d), rota.vertices());
        assertEquals(20.0, rota.distanciaKm(), 1e-9);
    }

    @Test
    void criterioTempoEscolheAcd() {
        Rota rota = melhor(Criterio.TEMPO, Restricoes.para(g));
        assertEquals(List.of(a, c, d), rota.vertices());
        assertEquals(40, rota.tempoMin());
    }

    @Test
    void criterioCustoEscolheAed() {
        Rota rota = melhor(Criterio.CUSTO, Restricoes.para(g));
        assertEquals(List.of(a, e, d), rota.vertices());
        assertEquals(6.0, rota.custoReais(), 1e-9);
    }

    @Test
    void totaisDasTresMetricasSaoSomadosAoLongoDoCaminho() {
        Rota rota = melhor(Criterio.DISTANCIA, Restricoes.para(g)); // A-B-D
        assertEquals(20.0, rota.distanciaKm(), 1e-9);
        assertEquals(60, rota.tempoMin());
        assertEquals(10.0, rota.custoReais(), 1e-9);
    }

    @Test
    void trechoBloqueadoMudaARota() {
        // Sem B-D, o melhor em distância passa a ser A-E-D (30 km).
        Restricoes r = Restricoes.para(g).bloquearTrecho("B", "D");
        Rota rota = melhor(Criterio.DISTANCIA, r);

        assertEquals(List.of(a, e, d), rota.vertices());
        assertEquals(30.0, rota.distanciaKm(), 1e-9);
    }

    @Test
    void trechoBloqueadoValeNosDoisSentidos() {
        Restricoes r = Restricoes.para(g).bloquearTrecho("D", "B"); // ordem invertida de propósito
        assertEquals(List.of(a, e, d), melhor(Criterio.DISTANCIA, r).vertices());
    }

    @Test
    void localidadeBloqueadaEHonrada() {
        Restricoes r = Restricoes.para(g).bloquearLocalidade("B").bloquearLocalidade("E");
        assertEquals(List.of(a, c, d), melhor(Criterio.DISTANCIA, r).vertices());
    }

    @Test
    void desbloquearRestauraARotaOriginal() {
        Restricoes r = Restricoes.para(g).bloquearLocalidade("B");
        assertEquals(List.of(a, e, d), melhor(Criterio.DISTANCIA, r).vertices());
        r.desbloquearLocalidade("B");
        assertEquals(List.of(a, b, d), melhor(Criterio.DISTANCIA, r).vertices());
    }

    @Test
    void origemOuDestinoBloqueadosNaoTemRota() {
        assertFalse(Dijkstra.menorCaminho(g, a, d, Criterio.DISTANCIA,
                Restricoes.para(g).bloquearLocalidade("A")).encontrada());
        assertFalse(Dijkstra.menorCaminho(g, a, d, Criterio.DISTANCIA,
                Restricoes.para(g).bloquearLocalidade("D")).encontrada());
    }

    @Test
    void destinoInalcancavelNaoTemRota() {
        assertFalse(Dijkstra.menorCaminho(g, a, f, Criterio.DISTANCIA, Restricoes.para(g)).encontrada());
    }

    @Test
    void origemIgualDestinoTemCustoZero() {
        Rota rota = Dijkstra.menorCaminho(g, a, a, Criterio.CUSTO, Restricoes.para(g)).rota().orElseThrow();
        assertEquals(0, rota.trechos());
        assertEquals(0.0, rota.custoReais(), 0.0);
    }

    @Test
    void rotaDeDijkstraNuncaEPiorQueNenhumaOutraEnumeradaPelaDfs() {
        for (Criterio criterio : Criterio.values()) {
            double melhorDijkstra = melhor(criterio, Restricoes.para(g)).valor(criterio);
            double melhorDfs = BuscaProfundidade.todosOsCaminhos(g, a, d, Restricoes.para(g), 100).stream()
                    .mapToDouble(r -> r.valor(criterio)).min().orElseThrow();
            assertEquals(melhorDfs, melhorDijkstra, 1e-9, "critério " + criterio);
        }
    }

    @Test
    void ordemDeFechamentoComecaNaOrigemETerminaNoDestino() {
        ResultadoBusca r = Dijkstra.menorCaminho(g, a, d, Criterio.DISTANCIA, Restricoes.para(g));
        assertEquals(a, r.ordemVisita().get(0));
        assertEquals(d, r.ordemVisita().get(r.ordemVisita().size() - 1));
    }
}
