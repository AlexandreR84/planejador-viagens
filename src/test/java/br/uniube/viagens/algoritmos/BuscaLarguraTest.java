package br.uniube.viagens.algoritmos;

import br.uniube.viagens.GrafosDeTeste;
import br.uniube.viagens.modelo.Grafo;
import br.uniube.viagens.modelo.Restricoes;
import br.uniube.viagens.modelo.Rota;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuscaLarguraTest {

    private final Grafo g = GrafosDeTeste.losango();
    private final int a = g.indiceDe("A");
    private final int b = g.indiceDe("B");
    private final int d = g.indiceDe("D");
    private final int f = g.indiceDe("F");

    @Test
    void encontraRotaComMenorNumeroDeTrechos() {
        ResultadoBusca r = BuscaLargura.menorNumeroDeTrechos(g, a, d, Restricoes.para(g));

        Rota rota = r.rota().orElseThrow();
        assertEquals(2, rota.trechos(), "A-D exige exatamente 2 trechos");
        assertEquals(a, rota.origem());
        assertEquals(d, rota.destino());
    }

    @Test
    void origemIgualDestinoRetornaRotaVazia() {
        Rota rota = BuscaLargura.menorNumeroDeTrechos(g, a, a, Restricoes.para(g)).rota().orElseThrow();

        assertEquals(0, rota.trechos());
        assertEquals(List.of(a), rota.vertices());
        assertEquals(0.0, rota.distanciaKm(), 0.0);
    }

    @Test
    void destinoInalcancavelNaoRetornaRota() {
        ResultadoBusca r = BuscaLargura.menorNumeroDeTrechos(g, a, f, Restricoes.para(g));
        assertFalse(r.encontrada());
    }

    @Test
    void localidadeBloqueadaForcaDesvio() {
        // Bloqueando B, D só é alcançável por C ou por E (ainda 2 trechos).
        Restricoes r = Restricoes.para(g).bloquearLocalidade("B");
        Rota rota = BuscaLargura.menorNumeroDeTrechos(g, a, d, r).rota().orElseThrow();

        assertFalse(rota.vertices().contains(b));
        assertEquals(2, rota.trechos());
    }

    @Test
    void bloquearDestinoImpedeARota() {
        Restricoes r = Restricoes.para(g).bloquearLocalidade("D");
        assertFalse(BuscaLargura.menorNumeroDeTrechos(g, a, d, r).encontrada());
    }

    @Test
    void trechoBloqueadoAumentaONumeroDeTrechosQuandoNaoHaAlternativaCurta() {
        // Sem A-B, A-C e A-E, o vértice A fica isolado.
        Restricoes r = Restricoes.para(g)
                .bloquearTrecho("A", "B").bloquearTrecho("A", "C").bloquearTrecho("A", "E");
        assertFalse(BuscaLargura.menorNumeroDeTrechos(g, a, d, r).encontrada());
    }

    @Test
    void ordemDeVisitaComecaPelaOrigemEVaiEmCamadas() {
        List<Integer> ordem = BuscaLargura.alcancaveis(g, a, Restricoes.para(g));

        assertEquals(a, ordem.get(0));
        // camada 1 (vizinhos de A) vem antes de D (camada 2)
        assertTrue(ordem.indexOf(b) < ordem.indexOf(d));
        assertTrue(ordem.indexOf(g.indiceDe("C")) < ordem.indexOf(d));
        assertTrue(ordem.indexOf(g.indiceDe("E")) < ordem.indexOf(d));
        assertFalse(ordem.contains(f), "F é isolado");
        assertEquals(5, ordem.size());
    }
}
