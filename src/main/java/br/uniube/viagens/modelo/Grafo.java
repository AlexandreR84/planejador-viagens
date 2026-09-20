package br.uniube.viagens.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Grafo ponderado e não direcionado, representado por <b>lista de adjacência</b>.
 *
 * <ul>
 *   <li>Vértices: {@link Localidade}, identificadas por um índice inteiro (0 .. n-1).</li>
 *   <li>Arestas: {@link Aresta}, com distância, tempo e custo.</li>
 * </ul>
 *
 * <p>Uma estrada de mão dupla é gravada como duas arestas (uma em cada lista de
 * adjacência). Com isso, percorrer os vizinhos de um vértice custa O(grau(v)),
 * e o espaço total é O(V + E) — mais adequado que uma matriz O(V²) para redes
 * viárias, que são esparsas.
 */
public final class Grafo {

    private final List<Localidade> localidades = new ArrayList<>();
    private final List<List<Aresta>> adjacencia = new ArrayList<>();
    private final Map<String, Integer> indicePorChave = new HashMap<>();
    private int totalEstradas = 0;

    // ---------------------------------------------------------------- construção

    public Localidade adicionarLocalidade(String nome, String uf, double latitude, double longitude) {
        String chave = Texto.chave(nome);
        if (chave.isEmpty()) {
            throw new IllegalArgumentException("O nome da localidade não pode ser vazio.");
        }
        if (indicePorChave.containsKey(chave)) {
            throw new IllegalArgumentException("Localidade duplicada: " + nome);
        }
        int id = localidades.size();
        Localidade localidade = new Localidade(id, nome.trim(), uf == null ? "" : uf.trim(), latitude, longitude);
        localidades.add(localidade);
        adjacencia.add(new ArrayList<>());
        indicePorChave.put(chave, id);
        return localidade;
    }

    public Localidade adicionarLocalidade(String nome, String uf) {
        return adicionarLocalidade(nome, uf, Double.NaN, Double.NaN);
    }

    /** Adiciona uma estrada de mão dupla entre dois vértices. */
    public void adicionarEstrada(int a, int b, double distanciaKm, int tempoMin, double custoReais) {
        validarIndice(a);
        validarIndice(b);
        if (a == b) {
            throw new IllegalArgumentException("Uma estrada precisa ligar duas localidades diferentes.");
        }
        if (distanciaKm < 0 || tempoMin < 0 || custoReais < 0) {
            throw new IllegalArgumentException("Distância, tempo e custo não podem ser negativos.");
        }
        if (existeEstrada(a, b)) {
            throw new IllegalArgumentException("Estrada duplicada entre "
                    + localidades.get(a).nome() + " e " + localidades.get(b).nome() + ".");
        }
        adjacencia.get(a).add(new Aresta(a, b, distanciaKm, tempoMin, custoReais));
        adjacencia.get(b).add(new Aresta(b, a, distanciaKm, tempoMin, custoReais));
        totalEstradas++;
    }

    public void adicionarEstrada(String origem, String destino, double distanciaKm, int tempoMin, double custoReais) {
        adicionarEstrada(indiceDe(origem), indiceDe(destino), distanciaKm, tempoMin, custoReais);
    }

    // ------------------------------------------------------------------ consulta

    /** Índice do vértice com este nome (ignora acentos e caixa). */
    public int indiceDe(String nome) {
        Integer indice = indicePorChave.get(Texto.chave(nome));
        if (indice == null) {
            throw new LocalidadeInexistenteException(nome);
        }
        return indice;
    }

    public Localidade localidade(int id) {
        validarIndice(id);
        return localidades.get(id);
    }

    public List<Localidade> localidades() {
        return Collections.unmodifiableList(localidades);
    }

    /** Arestas que saem do vértice (somente leitura). */
    public List<Aresta> vizinhos(int id) {
        validarIndice(id);
        return Collections.unmodifiableList(adjacencia.get(id));
    }

    public Optional<Aresta> estrada(int a, int b) {
        validarIndice(a);
        validarIndice(b);
        for (Aresta aresta : adjacencia.get(a)) {
            if (aresta.destino() == b) {
                return Optional.of(aresta);
            }
        }
        return Optional.empty();
    }

    public boolean existeEstrada(int a, int b) {
        return estrada(a, b).isPresent();
    }

    /** Cada estrada uma única vez (sem repetir o sentido inverso). */
    public List<Aresta> estradas() {
        List<Aresta> unicas = new ArrayList<>(totalEstradas);
        for (List<Aresta> lista : adjacencia) {
            for (Aresta aresta : lista) {
                if (aresta.origem() < aresta.destino()) {
                    unicas.add(aresta);
                }
            }
        }
        return unicas;
    }

    public int numeroLocalidades() {
        return localidades.size();
    }

    public int numeroEstradas() {
        return totalEstradas;
    }

    private void validarIndice(int id) {
        if (id < 0 || id >= localidades.size()) {
            throw new IllegalArgumentException("Índice de localidade inválido: " + id);
        }
    }
}
