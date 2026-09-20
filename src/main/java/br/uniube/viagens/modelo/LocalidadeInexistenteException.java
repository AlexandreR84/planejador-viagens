package br.uniube.viagens.modelo;

/** Lançada quando o nome informado não corresponde a nenhum vértice do grafo. */
public class LocalidadeInexistenteException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    public LocalidadeInexistenteException(String nome) {
        super("Localidade não encontrada: '" + nome + "'.");
    }
}
