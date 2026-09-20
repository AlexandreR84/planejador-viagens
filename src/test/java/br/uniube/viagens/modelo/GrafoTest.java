package br.uniube.viagens.modelo;

import br.uniube.viagens.GrafosDeTeste;
import br.uniube.viagens.algoritmos.BuscaLargura;
import br.uniube.viagens.dados.RedeViaria;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrafoTest {

    @Test
    void estradaEDeMaoDuplaEGravadaNasDuasListasDeAdjacencia() {
        Grafo g = GrafosDeTeste.losango();
        int a = g.indiceDe("A");
        int b = g.indiceDe("B");

        assertTrue(g.vizinhos(a).stream().anyMatch(e -> e.destino() == b));
        assertTrue(g.vizinhos(b).stream().anyMatch(e -> e.destino() == a));
        assertEquals(7, g.numeroEstradas());
        assertEquals(7, g.estradas().size(), "estradas() lista cada estrada uma única vez");
    }

    @Test
    void buscaPorNomeIgnoraAcentoECaixa() {
        Grafo g = RedeViaria.carregarPadrao();
        assertEquals(g.indiceDe("Uberlândia"), g.indiceDe("uberlandia"));
        assertEquals(g.indiceDe("Uberlândia"), g.indiceDe("  UBERLÂNDIA "));
        assertEquals(g.indiceDe("Patos de Minas"), g.indiceDe("patos   de minas"));
    }

    @Test
    void localidadeInexistenteLancaExcecao() {
        Grafo g = GrafosDeTeste.losango();
        assertThrows(LocalidadeInexistenteException.class, () -> g.indiceDe("Z"));
    }

    @Test
    void naoPermiteLocalidadeOuEstradaDuplicada() {
        Grafo g = GrafosDeTeste.losango();
        assertThrows(IllegalArgumentException.class, () -> g.adicionarLocalidade("a", "XX"));
        assertThrows(IllegalArgumentException.class, () -> g.adicionarEstrada("B", "A", 1, 1, 1));
    }

    @Test
    void naoPermiteLacoNemPesoNegativo() {
        Grafo g = GrafosDeTeste.losango();
        assertThrows(IllegalArgumentException.class, () -> g.adicionarEstrada("A", "A", 1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> g.adicionarEstrada("A", "F", -1, 1, 1));
    }

    @Test
    void redePadraoCarregaTodasAsLocalidadesEEstradas() {
        Grafo g = RedeViaria.carregarPadrao();
        assertEquals(17, g.numeroLocalidades());
        assertEquals(25, g.numeroEstradas());
    }

    @Test
    void redePadraoEConexa() {
        Grafo g = RedeViaria.carregarPadrao();
        Restricoes livre = Restricoes.para(g);
        int alcancaveis = BuscaLargura.alcancaveis(g, g.indiceDe("Uberlândia"), livre).size();
        assertEquals(g.numeroLocalidades(), alcancaveis);
    }

    @Test
    void custoDaEstradaSomaCombustivelEPedagio() {
        Grafo g = RedeViaria.carregarPadrao();
        Aresta a = g.estrada(g.indiceDe("Uberlândia"), g.indiceDe("Uberaba")).orElseThrow();
        // 108 km * 0,55 + 14,90 de pedágio
        assertEquals(74.30, a.custoReais(), 0.001);
    }
}
