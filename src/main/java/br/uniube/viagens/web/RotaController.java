package br.uniube.viagens.web;

import br.uniube.viagens.algoritmos.Planejador;
import br.uniube.viagens.algoritmos.ResultadoBusca;
import br.uniube.viagens.modelo.Criterio;
import br.uniube.viagens.modelo.Grafo;
import br.uniube.viagens.modelo.Restricoes;
import br.uniube.viagens.modelo.Rota;
import br.uniube.viagens.web.Dtos.ErroDto;
import br.uniube.viagens.web.Dtos.EstradaDto;
import br.uniube.viagens.web.Dtos.LocalidadeDto;
import br.uniube.viagens.web.Dtos.RestricoesDto;
import br.uniube.viagens.web.Dtos.RotaDto;
import br.uniube.viagens.web.Dtos.RotaRequisicao;
import br.uniube.viagens.web.Dtos.RotaResposta;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

/** API REST: expõe o grafo e o cálculo de rotas para o front-end. */
@RestController
@RequestMapping("/api")
public class RotaController {

    private static final int MAXIMO_PADRAO = 10;

    private final Grafo grafo;
    private final Planejador planejador;

    public RotaController(Grafo grafo, Planejador planejador) {
        this.grafo = grafo;
        this.planejador = planejador;
    }

    @GetMapping("/localidades")
    public List<LocalidadeDto> localidades() {
        return grafo.localidades().stream()
                .map(l -> new LocalidadeDto(l.nome(), l.uf(), l.latitude(), l.longitude()))
                .toList();
    }

    @GetMapping("/estradas")
    public List<EstradaDto> estradas() {
        return grafo.estradas().stream()
                .map(a -> new EstradaDto(grafo.localidade(a.origem()).nome(), grafo.localidade(a.destino()).nome(),
                        a.distanciaKm(), a.tempoMin(), a.custoReais()))
                .toList();
    }

    @PostMapping("/rotas")
    public RotaResposta calcular(@RequestBody RotaRequisicao requisicao) {
        if (vazio(requisicao.origem()) || vazio(requisicao.destino())) {
            throw new IllegalArgumentException("Informe a origem e o destino.");
        }
        String algoritmo = vazio(requisicao.algoritmo()) ? "DIJKSTRA" : requisicao.algoritmo().trim().toUpperCase(Locale.ROOT);
        Criterio criterio = vazio(requisicao.criterio()) ? Criterio.DISTANCIA : Criterio.de(requisicao.criterio());
        Restricoes restricoes = montarRestricoes(requisicao.restricoes());

        return switch (algoritmo) {
            case "DIJKSTRA" -> resposta(algoritmo, criterio,
                    planejador.melhorRota(requisicao.origem(), requisicao.destino(), criterio, restricoes));
            case "BFS" -> resposta(algoritmo, criterio,
                    planejador.menosTrechos(requisicao.origem(), requisicao.destino(), restricoes));
            case "DFS" -> respostaDfs(requisicao, criterio, restricoes);
            default -> throw new IllegalArgumentException("Algoritmo inválido: '" + requisicao.algoritmo()
                    + "'. Use DIJKSTRA, BFS ou DFS.");
        };
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroDto requisicaoInvalida(IllegalArgumentException e) {
        return new ErroDto(e.getMessage());
    }

    // ------------------------------------------------------------------ interno

    private RotaResposta resposta(String algoritmo, Criterio criterio, ResultadoBusca resultado) {
        List<RotaDto> rotas = resultado.rota().map(r -> List.of(paraDto(r))).orElse(List.of());
        return new RotaResposta(algoritmo, criterio.name(), resultado.encontrada(), rotas,
                nomes(resultado.ordemVisita()), mensagem(resultado.encontrada(), resultado.observacao()));
    }

    private RotaResposta respostaDfs(RotaRequisicao req, Criterio criterio, Restricoes restricoes) {
        int maximo = req.maximo() == null ? MAXIMO_PADRAO : req.maximo();
        ResultadoBusca primeiro = planejador.primeiroCaminho(req.origem(), req.destino(), restricoes);
        List<Rota> rotas = planejador.alternativas(req.origem(), req.destino(), criterio, restricoes, maximo);
        return new RotaResposta("DFS", criterio.name(), !rotas.isEmpty(),
                rotas.stream().map(this::paraDto).toList(),
                nomes(primeiro.ordemVisita()),
                mensagem(!rotas.isEmpty(), rotas.isEmpty() ? null : rotas.size() + " caminho(s), ordenados por "
                        + criterio.rotulo().toLowerCase(Locale.ROOT) + "."));
    }

    private String mensagem(boolean encontrada, String observacao) {
        if (!encontrada) {
            return observacao != null ? observacao : "Nenhuma rota encontrada com as restrições atuais.";
        }
        return observacao;
    }

    private RotaDto paraDto(Rota rota) {
        return new RotaDto(nomes(rota.vertices()), rota.distanciaKm(), rota.tempoMin(),
                rota.custoReais(), rota.trechos());
    }

    private List<String> nomes(List<Integer> indices) {
        return indices.stream().map(i -> grafo.localidade(i).nome()).toList();
    }

    private Restricoes montarRestricoes(RestricoesDto dto) {
        Restricoes restricoes = planejador.novasRestricoes();
        if (dto == null) {
            return restricoes;
        }
        if (dto.localidadesBloqueadas() != null) {
            dto.localidadesBloqueadas().forEach(restricoes::bloquearLocalidade);
        }
        if (dto.trechosBloqueados() != null) {
            for (List<String> par : dto.trechosBloqueados()) {
                if (par == null || par.size() != 2) {
                    throw new IllegalArgumentException("Cada trecho bloqueado deve ter exatamente duas localidades.");
                }
                restricoes.bloquearTrecho(par.get(0), par.get(1));
            }
        }
        restricoes.distanciaMaxima(dto.distanciaMaximaKm());
        restricoes.tempoMaximo(dto.tempoMaximoMin());
        restricoes.custoMaximo(dto.custoMaximo());
        return restricoes;
    }

    private static boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }
}
