package br.uniube.viagens.algoritmos;

import br.uniube.viagens.modelo.Rota;

import java.util.List;
import java.util.Optional;

/**
 * Retorno padrão dos algoritmos de busca.
 *
 * @param rota        melhor rota encontrada (vazio se o destino é inalcançável com as restrições)
 * @param ordemVisita índices dos vértices na ordem em que o algoritmo os visitou/fechou —
 *                    útil para mostrar na apresentação como cada busca explora o grafo
 * @param observacao  aviso opcional (ex.: "limites violados, usada busca exaustiva"); pode ser {@code null}
 */
public record ResultadoBusca(Optional<Rota> rota, List<Integer> ordemVisita, String observacao) {

    public ResultadoBusca {
        ordemVisita = List.copyOf(ordemVisita);
    }

    public static ResultadoBusca de(Optional<Rota> rota, List<Integer> ordemVisita) {
        return new ResultadoBusca(rota, ordemVisita, null);
    }

    public boolean encontrada() {
        return rota.isPresent();
    }
}
