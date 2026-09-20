package br.uniube.viagens.modelo;

/**
 * Vértice do grafo: uma cidade/localidade que pode ser origem, destino ou parada.
 *
 * @param id        índice do vértice na lista de adjacência (0 .. n-1)
 * @param nome      nome da localidade
 * @param uf        sigla do estado
 * @param latitude  usada apenas para desenhar o mapa no front-end
 * @param longitude usada apenas para desenhar o mapa no front-end
 */
public record Localidade(int id, String nome, String uf, double latitude, double longitude) {

    /** Ex.: "Uberlândia/MG". */
    public String rotulo() {
        return (uf == null || uf.isBlank()) ? nome : nome + "/" + uf;
    }
}
