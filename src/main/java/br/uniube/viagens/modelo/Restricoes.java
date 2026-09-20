package br.uniube.viagens.modelo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Restrições operacionais aplicadas à busca:
 * <ul>
 *   <li>localidades bloqueadas (ex.: cidade que o viajante quer evitar);</li>
 *   <li>trechos bloqueados (ex.: estrada interditada);</li>
 *   <li>limites de distância, tempo e custo para a viagem inteira.</li>
 * </ul>
 * Os algoritmos consultam este objeto a cada aresta explorada, então as
 * restrições podem ser alteradas entre uma busca e outra sem recarregar o grafo.
 */
public final class Restricoes {

    private static final double EPSILON = 1e-9;

    private final Grafo grafo;
    private final Set<Integer> localidadesBloqueadas = new HashSet<>();
    private final Set<Long> trechosBloqueados = new HashSet<>();
    private Double distanciaMaximaKm;
    private Integer tempoMaximoMin;
    private Double custoMaximo;

    private Restricoes(Grafo grafo) {
        this.grafo = grafo;
    }

    /** Conjunto de restrições vazio (nada bloqueado, sem limites) para este grafo. */
    public static Restricoes para(Grafo grafo) {
        return new Restricoes(grafo);
    }

    // ------------------------------------------------------------ alteração

    public Restricoes bloquearLocalidade(String nome) {
        localidadesBloqueadas.add(grafo.indiceDe(nome));
        return this;
    }

    public Restricoes desbloquearLocalidade(String nome) {
        localidadesBloqueadas.remove(grafo.indiceDe(nome));
        return this;
    }

    public Restricoes bloquearTrecho(String a, String b) {
        trechosBloqueados.add(chaveTrecho(indiceDaEstrada(a, b)));
        return this;
    }

    public Restricoes desbloquearTrecho(String a, String b) {
        trechosBloqueados.remove(chaveTrecho(indiceDaEstrada(a, b)));
        return this;
    }

    /** Define o limite de distância (km) para a viagem toda; {@code null} remove o limite. */
    public Restricoes distanciaMaxima(Double km) {
        this.distanciaMaximaKm = km;
        return this;
    }

    /** Define o limite de tempo (min) para a viagem toda; {@code null} remove o limite. */
    public Restricoes tempoMaximo(Integer minutos) {
        this.tempoMaximoMin = minutos;
        return this;
    }

    /** Define o limite de custo (R$) para a viagem toda; {@code null} remove o limite. */
    public Restricoes custoMaximo(Double reais) {
        this.custoMaximo = reais;
        return this;
    }

    public Restricoes limpar() {
        localidadesBloqueadas.clear();
        trechosBloqueados.clear();
        distanciaMaximaKm = null;
        tempoMaximoMin = null;
        custoMaximo = null;
        return this;
    }

    // ------------------------------------------------------------- consulta

    public boolean permiteLocalidade(int id) {
        return !localidadesBloqueadas.contains(id);
    }

    /** A aresta só pode ser usada se seus dois extremos e o próprio trecho estiverem liberados. */
    public boolean permite(Aresta aresta) {
        return permiteLocalidade(aresta.origem())
                && permiteLocalidade(aresta.destino())
                && !trechosBloqueados.contains(chaveTrecho(aresta.origem(), aresta.destino()));
    }

    public boolean temLimites() {
        return distanciaMaximaKm != null || tempoMaximoMin != null || custoMaximo != null;
    }

    /**
     * Indica se os totais informados já ultrapassam algum limite. Como todos os pesos são
     * não negativos, um caminho parcial que estoura o limite nunca volta a caber — o que
     * permite podar a busca em profundidade.
     */
    public boolean excede(double km, int minutos, double reais) {
        return (distanciaMaximaKm != null && km > distanciaMaximaKm + EPSILON)
                || (tempoMaximoMin != null && minutos > tempoMaximoMin)
                || (custoMaximo != null && reais > custoMaximo + EPSILON);
    }

    /** A rota respeita todos os limites? */
    public boolean atende(Rota rota) {
        return !excede(rota.distanciaKm(), rota.tempoMin(), rota.custoReais());
    }

    public Set<Integer> localidadesBloqueadas() {
        return Set.copyOf(localidadesBloqueadas);
    }

    public Double distanciaMaximaKm() {
        return distanciaMaximaKm;
    }

    public Integer tempoMaximoMin() {
        return tempoMaximoMin;
    }

    public Double custoMaximo() {
        return custoMaximo;
    }

    /** Trechos bloqueados como pares de índices {menor, maior}. */
    public List<int[]> trechosBloqueados() {
        List<int[]> pares = new ArrayList<>();
        for (long chave : trechosBloqueados) {
            pares.add(new int[] {(int) (chave / MULTIPLICADOR), (int) (chave % MULTIPLICADOR)});
        }
        return pares;
    }

    /** Resumo legível, uma linha por restrição ativa. */
    public List<String> descrever() {
        List<String> linhas = new ArrayList<>();
        for (int id : new java.util.TreeSet<>(localidadesBloqueadas)) {
            linhas.add("Localidade bloqueada: " + grafo.localidade(id).nome());
        }
        for (int[] par : trechosBloqueados()) {
            linhas.add("Trecho bloqueado: " + grafo.localidade(par[0]).nome()
                    + " - " + grafo.localidade(par[1]).nome());
        }
        if (distanciaMaximaKm != null) {
            linhas.add(String.format(Locale.ROOT, "Distância máxima: %.1f km", distanciaMaximaKm));
        }
        if (tempoMaximoMin != null) {
            linhas.add("Tempo máximo: " + tempoMaximoMin + " min");
        }
        if (custoMaximo != null) {
            linhas.add(String.format(Locale.ROOT, "Custo máximo: R$ %.2f", custoMaximo));
        }
        return linhas;
    }

    // -------------------------------------------------------------- interno

    private static final long MULTIPLICADOR = 1_000_000L;

    /** Chave simétrica: o trecho A-B e o trecho B-A são o mesmo. */
    private static long chaveTrecho(int a, int b) {
        return Math.min(a, b) * MULTIPLICADOR + Math.max(a, b);
    }

    private static long chaveTrecho(int[] par) {
        return chaveTrecho(par[0], par[1]);
    }

    private int[] indiceDaEstrada(String a, String b) {
        int ia = grafo.indiceDe(a);
        int ib = grafo.indiceDe(b);
        if (!grafo.existeEstrada(ia, ib)) {
            throw new IllegalArgumentException("Não existe estrada direta entre "
                    + grafo.localidade(ia).nome() + " e " + grafo.localidade(ib).nome() + ".");
        }
        return new int[] {ia, ib};
    }
}
