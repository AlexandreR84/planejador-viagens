package br.uniube.viagens.web;

import br.uniube.viagens.algoritmos.Planejador;
import br.uniube.viagens.dados.RedeViaria;
import br.uniube.viagens.modelo.Grafo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra o núcleo (que não depende do Spring) como beans para injetar no controller. */
@Configuration(proxyBeanMethods = false)
public class ConfiguracaoNucleo {

    @Bean
    public Grafo grafo() {
        return RedeViaria.carregarPadrao();
    }

    @Bean
    public Planejador planejador(Grafo grafo) {
        return new Planejador(grafo);
    }
}
