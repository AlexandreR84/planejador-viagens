package br.uniube.viagens.modelo;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Utilitário de normalização de texto. Permite que o usuário digite
 * "uberlandia", "Uberlândia" ou "  UBERLÂNDIA " e encontre a mesma localidade.
 */
public final class Texto {

    private Texto() {
    }

    /** Remove acentos, espaços extras e diferenças de caixa. */
    public static String chave(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return semAcento.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
