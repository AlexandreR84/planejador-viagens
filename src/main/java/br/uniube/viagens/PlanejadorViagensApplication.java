package br.uniube.viagens;

import br.uniube.viagens.cli.ConsoleApp;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;

/**
 * Ponto de entrada.
 * <ul>
 *   <li>Sem argumentos: sobe a API REST + front-end web em http://localhost:8080</li>
 *   <li>Com {@code --cli}: abre o menu de console, sem iniciar o servidor.</li>
 * </ul>
 */
@SpringBootApplication
public class PlanejadorViagensApplication {

    public static void main(String[] args) {
        if (Arrays.asList(args).contains("--cli")) {
            ConsoleApp.main(new String[0]);
            return;
        }
        SpringApplication.run(PlanejadorViagensApplication.class, args);
    }
}
