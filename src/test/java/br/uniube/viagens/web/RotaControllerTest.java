package br.uniube.viagens.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RotaControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void listaLocalidadesEEstradas() throws Exception {
        mvc.perform(get("/api/localidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(17))
                .andExpect(jsonPath("$[0].nome").value("Uberlândia"));

        mvc.perform(get("/api/estradas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(25));
    }

    @Test
    void dijkstraDevolveMelhorRota() throws Exception {
        String corpo = """
                {"origem":"Uberlândia","destino":"Araxá","algoritmo":"DIJKSTRA","criterio":"DISTANCIA"}
                """;
        mvc.perform(post("/api/rotas").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encontrada").value(true))
                .andExpect(jsonPath("$.rotas[0].localidades[1]").value("Uberaba"))
                .andExpect(jsonPath("$.rotas[0].distanciaKm").value(228.0));
    }

    @Test
    void restricoesChegamPelaRequisicao() throws Exception {
        String corpo = """
                {"origem":"Uberlândia","destino":"Araxá","criterio":"DISTANCIA",
                 "restricoes":{"localidadesBloqueadas":["Uberaba"]}}
                """;
        mvc.perform(post("/api/rotas").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rotas[0].localidades[1]").value("Monte Carmelo"));
    }

    @Test
    void dfsDevolveVariosCaminhos() throws Exception {
        String corpo = """
                {"origem":"Uberlândia","destino":"Goiânia","algoritmo":"DFS","maximo":3}
                """;
        mvc.perform(post("/api/rotas").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rotas.length()").value(3));
    }

    @Test
    void localidadeInexistenteRetorna400ComMensagem() throws Exception {
        String corpo = """
                {"origem":"Atlantis","destino":"Goiânia"}
                """;
        mvc.perform(post("/api/rotas").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Localidade não encontrada: 'Atlantis'."));
    }

    @Test
    void algoritmoInvalidoRetorna400() throws Exception {
        String corpo = """
                {"origem":"Uberlândia","destino":"Goiânia","algoritmo":"ASTAR"}
                """;
        mvc.perform(post("/api/rotas").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest());
    }
}
