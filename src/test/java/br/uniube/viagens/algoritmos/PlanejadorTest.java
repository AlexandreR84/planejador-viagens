package br.uniube.viagens.algoritmos;

import br.uniube.viagens.GrafosDeTeste;
import br.uniube.viagens.dados.RedeViaria;
import br.uniube.viagens.modelo.Criterio;
import br.uniube.viagens.modelo.Grafo;
import br.uniube.viagens.modelo.LocalidadeInexistenteException;
import br.uniube.viagens.modelo.Restricoes;
import br.uniube.viagens.modelo.Rota;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanejadorTest {

    // ------------------------------------------------------ grafo de teste (losango)

    @Test
    void limiteViolaMenorCaminhoAcionaBuscaExaustivaComPoda() {
        Planejador p = new Planejador(GrafosDeTeste.losango());
        Restricoes r = p.novasRestricoes().custoMaximo(7.0);

        ResultadoBusca res = p.melhorRota("A", "D", Criterio.TEMPO, r);

        assertEquals(List.of(0, 4, 3), res.rota().orElseThrow().vertices()); // A-E-D
        assertNotNull(res.observacao(), "deve avisar que os limites forçaram uma rota diferente");
    }

    @Test
    void semRotaDentroDosLimitesInformaNaObservacao() {
        Planejador p = new Planejador(GrafosDeTeste.losango());
        ResultadoBusca res = p.melhorRota("A", "D", Criterio.DISTANCIA, p.novasRestricoes().custoMaximo(1.0));

        assertFalse(res.encontrada());
        assertTrue(res.observacao().contains("Nenhuma rota"));
    }

    @Test
    void semLimitesNaoHaObservacao() {
        Planejador p = new Planejador(GrafosDeTeste.losango());
        ResultadoBusca res = p.melhorRota("A", "D", Criterio.DISTANCIA, p.novasRestricoes());
        assertEquals(null, res.observacao());
    }

    @Test
    void bfsIndicaQuandoARotaComMenosTrechosUltrapassaLimites() {
        Planejador p = new Planejador(GrafosDeTeste.losango());
        ResultadoBusca res = p.menosTrechos("A", "D", p.novasRestricoes().distanciaMaxima(10.0));

        assertFalse(res.encontrada());
        assertNotNull(res.observacao());
    }

    @Test
    void alternativasVemOrdenadasPeloCriterio() {
        Planejador p = new Planejador(GrafosDeTeste.losango());
        List<Rota> rotas = p.alternativas("A", "D", Criterio.CUSTO, p.novasRestricoes(), 10);

        assertEquals(5, rotas.size());
        for (int i = 1; i < rotas.size(); i++) {
            assertTrue(rotas.get(i - 1).custoReais() <= rotas.get(i).custoReais());
        }
        assertEquals(6.0, rotas.get(0).custoReais(), 1e-9);
    }

    @Test
    void alternativasRespeitamOMaximoSolicitado() {
        Planejador p = new Planejador(GrafosDeTeste.losango());
        assertEquals(3, p.alternativas("A", "D", Criterio.DISTANCIA, p.novasRestricoes(), 3).size());
    }

    @Test
    void nomeInexistenteLancaExcecao() {
        Planejador p = new Planejador(GrafosDeTeste.losango());
        assertThrows(LocalidadeInexistenteException.class,
                () -> p.melhorRota("A", "Z", Criterio.DISTANCIA, p.novasRestricoes()));
        assertThrows(LocalidadeInexistenteException.class,
                () -> p.menosTrechos("Z", "A", p.novasRestricoes()));
    }

    @Test
    void bloquearTrechoInexistenteLancaExcecao() {
        Planejador p = new Planejador(GrafosDeTeste.losango());
        assertThrows(IllegalArgumentException.class, () -> p.novasRestricoes().bloquearTrecho("A", "D"));
    }

    // ------------------------------------------------------------ rede real (CSV)

    private final Grafo rede = RedeViaria.carregarPadrao();
    private final Planejador planejador = new Planejador(rede);

    private List<String> nomes(Rota rota) {
        return rota.vertices().stream().map(i -> rede.localidade(i).nome()).toList();
    }

    @Test
    void uberlandiaGoianiaPorDistanciaPassaPorItumbiara() {
        Rota rota = planejador.melhorRota("Uberlândia", "Goiânia", Criterio.DISTANCIA,
                planejador.novasRestricoes()).rota().orElseThrow();

        assertEquals(List.of("Uberlândia", "Tupaciguara", "Itumbiara", "Goiânia"), nomes(rota));
        assertEquals(321.0, rota.distanciaKm(), 1e-9);
    }

    @Test
    void bloquearItumbiaraDesviaPorAraguariECatalao() {
        Restricoes r = planejador.novasRestricoes().bloquearLocalidade("Itumbiara");
        Rota rota = planejador.melhorRota("uberlandia", "goiania", Criterio.DISTANCIA, r).rota().orElseThrow();

        assertEquals(List.of("Uberlândia", "Araguari", "Catalão", "Goiânia"), nomes(rota));
        assertEquals(403.0, rota.distanciaKm(), 1e-9);
    }

    @Test
    void criteriosDiferentesEscolhemRotasDiferentesEntreUberlandiaEAraxa() {
        Restricoes livre = planejador.novasRestricoes();

        Rota porKm = planejador.melhorRota("Uberlândia", "Araxá", Criterio.DISTANCIA, livre).rota().orElseThrow();
        Rota porTempo = planejador.melhorRota("Uberlândia", "Araxá", Criterio.TEMPO, livre).rota().orElseThrow();
        Rota porCusto = planejador.melhorRota("Uberlândia", "Araxá", Criterio.CUSTO, livre).rota().orElseThrow();

        assertEquals(List.of("Uberlândia", "Uberaba", "Araxá"), nomes(porKm));
        assertEquals(List.of("Uberlândia", "Uberaba", "Araxá"), nomes(porTempo));
        // O caminho por Monte Carmelo é mais longo, mas evita os pedágios de Uberaba.
        assertEquals(List.of("Uberlândia", "Monte Carmelo", "Patrocínio", "Araxá"), nomes(porCusto));
        assertEquals(145.75, porCusto.custoReais(), 0.001);
    }

    @Test
    void limiteDeCustoForcaARotaMaisBarataMesmoPedindoMenorDistancia() {
        Restricoes r = planejador.novasRestricoes().custoMaximo(150.0);
        ResultadoBusca res = planejador.melhorRota("Uberlândia", "Araxá", Criterio.DISTANCIA, r);

        assertEquals(List.of("Uberlândia", "Monte Carmelo", "Patrocínio", "Araxá"), nomes(res.rota().orElseThrow()));
        assertNotNull(res.observacao());
    }

    @Test
    void trechoBloqueadoNaRedeRealMudaARota() {
        Restricoes r = planejador.novasRestricoes().bloquearTrecho("Uberlândia", "Uberaba");
        Rota rota = planejador.melhorRota("Uberlândia", "Uberaba", Criterio.DISTANCIA, r).rota().orElseThrow();

        // Sem a estrada direta: Uberlândia -> Monte Carmelo -> Patrocínio -> Araxá -> Uberaba (385 km)
        assertEquals(List.of("Uberlândia", "Monte Carmelo", "Patrocínio", "Araxá", "Uberaba"), nomes(rota));
    }

    @Test
    void bfsNaRedeRealDevolveMenorNumeroDeTrechos() {
        ResultadoBusca res = planejador.menosTrechos("Uberlândia", "São Paulo", planejador.novasRestricoes());
        Rota rota = res.rota().orElseThrow();

        assertEquals(3, rota.trechos()); // Uberlândia - Uberaba - Ribeirão Preto/Franca - São Paulo
        assertEquals("São Paulo", nomes(rota).get(nomes(rota).size() - 1));
    }

    @Test
    void bloquearTodasAsSaidasDeUberlandiaIsolaAOrigem() {
        Restricoes r = planejador.novasRestricoes()
                .bloquearTrecho("Uberlândia", "Araguari")
                .bloquearTrecho("Uberlândia", "Uberaba")
                .bloquearTrecho("Uberlândia", "Tupaciguara")
                .bloquearTrecho("Uberlândia", "Ituiutaba")
                .bloquearTrecho("Uberlândia", "Monte Carmelo");

        assertFalse(planejador.melhorRota("Uberlândia", "Goiânia", Criterio.DISTANCIA, r).encontrada());
        assertFalse(planejador.menosTrechos("Uberlândia", "Goiânia", r).encontrada());
        assertTrue(planejador.alternativas("Uberlândia", "Goiânia", Criterio.DISTANCIA, r, 10).isEmpty());
    }

    @Test
    void dfsNaRedeRealListaAlternativasEIncluiAMelhorRotaDeDijkstra() {
        Restricoes livre = planejador.novasRestricoes();
        List<Rota> rotas = planejador.alternativas("Uberlândia", "Goiânia", Criterio.DISTANCIA, livre, 50);
        Rota dijkstra = planejador.melhorRota("Uberlândia", "Goiânia", Criterio.DISTANCIA, livre).rota().orElseThrow();

        assertTrue(rotas.size() > 1, "existe mais de um caminho possível");
        assertEquals(dijkstra.vertices(), rotas.get(0).vertices(),
                "a melhor alternativa da DFS coincide com a rota de Dijkstra");
    }
}
