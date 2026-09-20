package br.uniube.viagens.cli;

import br.uniube.viagens.algoritmos.Planejador;
import br.uniube.viagens.algoritmos.ResultadoBusca;
import br.uniube.viagens.dados.RedeViaria;
import br.uniube.viagens.modelo.Aresta;
import br.uniube.viagens.modelo.Criterio;
import br.uniube.viagens.modelo.Grafo;
import br.uniube.viagens.modelo.Localidade;
import br.uniube.viagens.modelo.Restricoes;
import br.uniube.viagens.modelo.Rota;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Interface de console (menu interativo) do Planejador Inteligente de Viagens.
 *
 * <p>Execução: {@code java -cp target/classes br.uniube.viagens.cli.ConsoleApp}
 * ou {@code java -jar target/planejador-viagens.jar --cli}.
 */
public final class ConsoleApp {

    private static final Locale BR = Locale.of("pt", "BR");

    private final Grafo grafo;
    private final Planejador planejador;
    private final Restricoes restricoes;
    private final BufferedReader entrada;
    private final PrintStream saida;

    public ConsoleApp(Grafo grafo, InputStream entrada, PrintStream saida) {
        this.grafo = grafo;
        this.planejador = new Planejador(grafo);
        this.restricoes = planejador.novasRestricoes();
        this.entrada = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8));
        this.saida = saida;
    }

    public static void main(String[] args) {
        PrintStream saida = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        new ConsoleApp(RedeViaria.carregarPadrao(), System.in, saida).executar();
    }

    /** Sinaliza fim da entrada (Ctrl+D / arquivo acabou). */
    private static final class EntradaEncerrada extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    // ---------------------------------------------------------------- laço principal

    public void executar() {
        saida.println();
        saida.println("=== Planejamento Inteligente de Viagens ===");
        saida.printf(BR, "Rede carregada: %d localidades e %d estradas.%n",
                grafo.numeroLocalidades(), grafo.numeroEstradas());

        try {
            while (true) {
                menuPrincipal();
                String opcao = ler("Opção: ");
                try {
                    switch (opcao) {
                        case "1" -> listarLocalidades();
                        case "2" -> listarEstradas();
                        case "3" -> melhorRota();
                        case "4" -> menosTrechos();
                        case "5" -> todosOsCaminhos();
                        case "6" -> menuRestricoes();
                        case "0" -> {
                            saida.println("Até logo!");
                            return;
                        }
                        default -> saida.println("Opção inválida.");
                    }
                } catch (IllegalArgumentException e) {
                    saida.println("Erro: " + e.getMessage());
                }
            }
        } catch (EntradaEncerrada e) {
            saida.println();
            saida.println("Entrada encerrada.");
        }
    }

    private void menuPrincipal() {
        saida.println();
        saida.println("1) Listar localidades");
        saida.println("2) Listar estradas");
        saida.println("3) Melhor rota (Dijkstra)");
        saida.println("4) Rota com menos trechos (BFS)");
        saida.println("5) Todos os caminhos possíveis (DFS)");
        saida.println("6) Restrições (bloqueios e limites)");
        saida.println("0) Sair");
    }

    // ------------------------------------------------------------------- consultas

    private void listarLocalidades() {
        saida.println();
        for (Localidade l : grafo.localidades()) {
            saida.printf(BR, "%3d) %s%n", l.id() + 1, l.rotulo());
        }
    }

    private void listarEstradas() {
        saida.println();
        for (Aresta a : grafo.estradas()) {
            saida.printf(BR, "%-16s <-> %-16s %6.1f km  %8s  R$ %7.2f%n",
                    grafo.localidade(a.origem()).nome(), grafo.localidade(a.destino()).nome(),
                    a.distanciaKm(), formatarTempo(a.tempoMin()), a.custoReais());
        }
    }

    private void melhorRota() {
        String origem = lerLocalidade("Origem (nome ou número): ");
        String destino = lerLocalidade("Destino (nome ou número): ");
        Criterio criterio = lerCriterio();
        ResultadoBusca r = planejador.melhorRota(origem, destino, criterio, restricoes);
        saida.println();
        saida.println("Algoritmo: Dijkstra (critério: " + criterio.rotulo() + ")");
        imprimirResultado(r);
    }

    private void menosTrechos() {
        String origem = lerLocalidade("Origem (nome ou número): ");
        String destino = lerLocalidade("Destino (nome ou número): ");
        ResultadoBusca r = planejador.menosTrechos(origem, destino, restricoes);
        saida.println();
        saida.println("Algoritmo: Busca em Largura (BFS) - menor número de trechos");
        imprimirResultado(r);
    }

    private void todosOsCaminhos() {
        String origem = lerLocalidade("Origem (nome ou número): ");
        String destino = lerLocalidade("Destino (nome ou número): ");
        Criterio criterio = lerCriterio();
        int maximo = lerInteiro("Quantos caminhos exibir (Enter = 10): ", 10);

        ResultadoBusca primeiro = planejador.primeiroCaminho(origem, destino, restricoes);
        List<Rota> rotas = planejador.alternativas(origem, destino, criterio, restricoes, maximo);

        saida.println();
        saida.println("Algoritmo: Busca em Profundidade (DFS) com backtracking");
        saida.println("Ordem de visita da DFS clássica: " + nomes(primeiro.ordemVisita()));
        if (rotas.isEmpty()) {
            saida.println("Nenhum caminho encontrado com as restrições atuais.");
            return;
        }
        saida.printf(BR, "%d caminho(s), ordenados por %s:%n", rotas.size(), criterio.rotulo().toLowerCase(BR));
        int i = 1;
        for (Rota rota : rotas) {
            saida.println();
            saida.println("Caminho " + i++ + ":");
            imprimirRota(rota);
        }
    }

    // ----------------------------------------------------------------- restrições

    private void menuRestricoes() {
        while (true) {
            saida.println();
            saida.println("--- Restrições ativas ---");
            List<String> ativas = restricoes.descrever();
            if (ativas.isEmpty()) {
                saida.println("(nenhuma)");
            } else {
                ativas.forEach(linha -> saida.println("* " + linha));
            }
            saida.println();
            saida.println("1) Bloquear localidade");
            saida.println("2) Desbloquear localidade");
            saida.println("3) Bloquear trecho");
            saida.println("4) Desbloquear trecho");
            saida.println("5) Definir limites (km, minutos, R$)");
            saida.println("6) Limpar todas as restrições");
            saida.println("0) Voltar");
            String opcao = ler("Opção: ");
            try {
                switch (opcao) {
                    case "1" -> restricoes.bloquearLocalidade(lerLocalidade("Localidade a bloquear: "));
                    case "2" -> restricoes.desbloquearLocalidade(lerLocalidade("Localidade a desbloquear: "));
                    case "3" -> restricoes.bloquearTrecho(
                            lerLocalidade("Primeira ponta do trecho: "), lerLocalidade("Segunda ponta do trecho: "));
                    case "4" -> restricoes.desbloquearTrecho(
                            lerLocalidade("Primeira ponta do trecho: "), lerLocalidade("Segunda ponta do trecho: "));
                    case "5" -> definirLimites();
                    case "6" -> restricoes.limpar();
                    case "0" -> {
                        return;
                    }
                    default -> saida.println("Opção inválida.");
                }
            } catch (IllegalArgumentException e) {
                saida.println("Erro: " + e.getMessage());
            }
        }
    }

    private void definirLimites() {
        saida.println("Deixe em branco para manter; digite 0 para remover o limite.");
        String km = ler("Distância máxima (km): ");
        if (!km.isEmpty()) {
            double v = lerNumero(km);
            restricoes.distanciaMaxima(v == 0 ? null : v);
        }
        String min = ler("Tempo máximo (minutos): ");
        if (!min.isEmpty()) {
            int v = (int) lerNumero(min);
            restricoes.tempoMaximo(v == 0 ? null : v);
        }
        String reais = ler("Custo máximo (R$): ");
        if (!reais.isEmpty()) {
            double v = lerNumero(reais);
            restricoes.custoMaximo(v == 0 ? null : v);
        }
    }

    // ----------------------------------------------------------------- impressão

    private void imprimirResultado(ResultadoBusca resultado) {
        if (resultado.rota().isPresent()) {
            imprimirRota(resultado.rota().get());
        } else {
            saida.println("Nenhuma rota encontrada com as restrições atuais.");
        }
        if (resultado.observacao() != null) {
            saida.println("Obs.: " + resultado.observacao());
        }
        if (!resultado.ordemVisita().isEmpty()) {
            saida.println("Ordem de visita: " + nomes(resultado.ordemVisita()));
        }
    }

    private void imprimirRota(Rota rota) {
        saida.println("  Rota: " + rota.vertices().stream()
                .map(i -> grafo.localidade(i).nome())
                .collect(Collectors.joining(" -> ")));
        saida.printf(BR, "  Distância: %.1f km | Tempo: %s | Custo: R$ %.2f | Trechos: %d%n",
                rota.distanciaKm(), formatarTempo(rota.tempoMin()), rota.custoReais(), rota.trechos());
    }

    private String nomes(List<Integer> indices) {
        return indices.stream().map(i -> grafo.localidade(i).nome()).collect(Collectors.joining(", "));
    }

    static String formatarTempo(int minutos) {
        return String.format(BR, "%dh%02dmin", minutos / 60, minutos % 60);
    }

    // ------------------------------------------------------------------- entrada

    private String ler(String pergunta) {
        saida.print(pergunta);
        saida.flush();
        try {
            String linha = entrada.readLine();
            if (linha == null) {
                throw new EntradaEncerrada();
            }
            return linha.trim();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Aceita o nome da localidade ou o número mostrado na listagem. */
    private String lerLocalidade(String pergunta) {
        String texto = ler(pergunta);
        if (texto.matches("\\d+")) {
            int numero = Integer.parseInt(texto);
            if (numero < 1 || numero > grafo.numeroLocalidades()) {
                throw new IllegalArgumentException("Número fora da lista (1 a " + grafo.numeroLocalidades() + ").");
            }
            return grafo.localidade(numero - 1).nome();
        }
        // valida já na digitação, para o erro aparecer antes das próximas perguntas
        return grafo.localidade(grafo.indiceDe(texto)).nome();
    }

    private Criterio lerCriterio() {
        String texto = ler("Critério [1=Distância, 2=Tempo, 3=Custo] (Enter = Distância): ");
        return switch (texto) {
            case "", "1" -> Criterio.DISTANCIA;
            case "2" -> Criterio.TEMPO;
            case "3" -> Criterio.CUSTO;
            default -> Criterio.de(texto);
        };
    }

    private int lerInteiro(String pergunta, int padrao) {
        String texto = ler(pergunta);
        if (texto.isEmpty()) {
            return padrao;
        }
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Número inválido: '" + texto + "'.");
        }
    }

    private double lerNumero(String texto) {
        try {
            double valor = Double.parseDouble(texto.replace(',', '.'));
            if (valor < 0) {
                throw new IllegalArgumentException("O valor não pode ser negativo.");
            }
            return valor;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Número inválido: '" + texto + "'.");
        }
    }
}
