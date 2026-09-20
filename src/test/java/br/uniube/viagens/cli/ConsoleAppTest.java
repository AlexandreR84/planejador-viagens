package br.uniube.viagens.cli;

import br.uniube.viagens.dados.RedeViaria;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleAppTest {

    /** Roda o menu com as linhas de entrada informadas e devolve tudo o que foi impresso. */
    private String executar(String... linhas) {
        String entrada = String.join("\n", linhas) + "\n";
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream saida = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        new ConsoleApp(RedeViaria.carregarPadrao(),
                new ByteArrayInputStream(entrada.getBytes(StandardCharsets.UTF_8)), saida).executar();
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    void melhorRotaImprimeCaminhoETotais() {
        String texto = executar("3", "Uberlândia", "Araxá", "1", "0");

        assertTrue(texto.contains("Uberlândia -> Uberaba -> Araxá"), texto);
        assertTrue(texto.contains("228"), texto);
        assertTrue(texto.contains("Dijkstra"), texto);
    }

    @Test
    void aceitaNumeroDaListaEIgnoraAcentos() {
        // 1 = Uberlândia, 9 = Araxá na ordem do CSV; "uberlandia" sem acento também vale.
        String texto = executar("3", "1", "9", "", "0");
        assertTrue(texto.contains("Uberlândia -> Uberaba -> Araxá"), texto);

        String semAcento = executar("3", "uberlandia", "araxa", "", "0");
        assertTrue(semAcento.contains("Uberlândia -> Uberaba -> Araxá"), semAcento);
    }

    @Test
    void restricaoBloqueiaLocalidadeENovaBuscaDesvia() {
        // menu 6 (restrições) -> 1 (bloquear) -> Uberaba -> 0 (voltar) -> 3 (melhor rota)
        String texto = executar("6", "1", "Uberaba", "0", "3", "Uberlândia", "Araxá", "1", "0");

        assertTrue(texto.contains("Localidade bloqueada: Uberaba"), texto);
        assertTrue(texto.contains("Uberlândia -> Monte Carmelo -> Patrocínio -> Araxá"), texto);
    }

    @Test
    void bfsEDfsFuncionamPeloMenu() {
        String bfs = executar("4", "Uberlândia", "São Paulo", "0");
        assertTrue(bfs.contains("Busca em Largura"), bfs);
        assertTrue(bfs.contains("Trechos: 3"), bfs);

        String dfs = executar("5", "Uberlândia", "Goiânia", "1", "5", "0");
        assertTrue(dfs.contains("Busca em Profundidade"), dfs);
        assertTrue(dfs.contains("Caminho 1:"), dfs);
    }

    @Test
    void erroDeDigitacaoNaoDerrubaOPrograma() {
        String texto = executar("3", "Atlantis", "Uberlândia", "9", "0");

        assertTrue(texto.contains("Erro: Localidade não encontrada"), texto);
        assertTrue(texto.contains("Opção inválida."), texto);
        assertTrue(texto.contains("Até logo!"), texto);
    }

    @Test
    void fimDaEntradaEncerraSemExcecao() {
        String texto = executar("1");
        assertTrue(texto.contains("Entrada encerrada."), texto);
    }
}
