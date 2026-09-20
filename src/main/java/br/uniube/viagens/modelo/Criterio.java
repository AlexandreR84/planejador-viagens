package br.uniube.viagens.modelo;

import java.util.function.ToDoubleFunction;

/** Critério de preferência do usuário: qual métrica das arestas será minimizada. */
public enum Criterio {

    DISTANCIA("Distância", "km", Aresta::distanciaKm),
    TEMPO("Tempo", "min", a -> a.tempoMin()),
    CUSTO("Custo", "R$", Aresta::custoReais);

    private final String rotulo;
    private final String unidade;
    private final ToDoubleFunction<Aresta> extrator;

    Criterio(String rotulo, String unidade, ToDoubleFunction<Aresta> extrator) {
        this.rotulo = rotulo;
        this.unidade = unidade;
        this.extrator = extrator;
    }

    public String rotulo() {
        return rotulo;
    }

    public String unidade() {
        return unidade;
    }

    /** Peso (não negativo) da aresta neste critério. */
    public double peso(Aresta aresta) {
        return extrator.applyAsDouble(aresta);
    }

    /** Converte texto digitado pelo usuário ("distancia", "Tempo", "custo"...). */
    public static Criterio de(String texto) {
        String chave = Texto.chave(texto);
        for (Criterio c : values()) {
            if (Texto.chave(c.name()).equals(chave) || Texto.chave(c.rotulo).equals(chave)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Critério inválido: '" + texto + "'. Use distancia, tempo ou custo.");
    }
}
