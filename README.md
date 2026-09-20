# Planejador Inteligente de Viagens

Trabalho de integralização de **Algoritmos em Grafos** (Uniube, 2026/2). A aplicação modela uma rede
de estradas como um grafo ponderado e calcula rotas entre duas cidades com **BFS**, **DFS** e
**Dijkstra**, respeitando critérios de preferência (distância, tempo ou custo) e restrições
(localidades/trechos bloqueados e limites de km, minutos e R$).

## Como executar

Requisitos: **JDK 21** e **Maven 3.9+**.

```bash
# Testes (JUnit 5 + MockMvc)
mvn test

# Interface web + API REST  ->  http://localhost:8080
mvn spring-boot:run

# Interface de console (menu interativo), sem subir servidor
mvn -q compile
java -cp target/classes br.uniube.viagens.cli.ConsoleApp

# Ou, depois de "mvn package":
java -jar target/planejador-viagens.jar          # web
java -jar target/planejador-viagens.jar --cli    # console
```

No Windows, se os acentos aparecerem trocados no console, rode `chcp 65001` antes.

## Estrutura

```
src/main/java/br/uniube/viagens
├── modelo/        Grafo, Localidade (vértice), Aresta, Criterio, Restricoes, Rota
├── algoritmos/    BuscaLargura (BFS), BuscaProfundidade (DFS), Dijkstra, Planejador (fachada)
├── dados/         RedeViaria: carrega o grafo dos CSVs
├── cli/           ConsoleApp: menu de console
└── web/           RotaController (API REST), DTOs, configuração dos beans
src/main/resources
├── dados/         localidades.csv e estradas.csv  (a rede; troque os CSVs para usar outra região)
└── static/        index.html (front-end com mapa SVG)
```

O núcleo (`modelo`, `algoritmos`, `dados`) **não depende do Spring**: console, API e testes usam as mesmas classes.

## Modelagem (item 1 do roteiro)

| Elemento | Representação |
|---|---|
| Vértice | `Localidade` (cidade), identificada por índice 0..n-1 |
| Aresta | `Aresta`: trecho de estrada, de mão dupla (gravada nos dois sentidos) |
| Pesos | distância (km), tempo (min) e custo (R$ = km × 0,55 + pedágio) em **toda** aresta |
| Peso usado na busca | o `Criterio` escolhido pelo usuário |

A rede padrão tem 17 cidades (Triângulo Mineiro, Alto Paranaíba, Goiás e interior de SP) e 25 estradas.
**Os valores de distância, tempo e pedágio são aproximações didáticas** e não substituem um GPS.

## Estruturas de dados (item 2)

- **Lista de adjacência** (`List<List<Aresta>>` no `Grafo`): O(V + E) de espaço, ideal para redes viárias esparsas.
- **Fila** (`ArrayDeque`) na BFS.
- **Fila de prioridade / heap binário** (`PriorityQueue`) no Dijkstra.
- **Vetor de aresta-pai + `Rota.reconstruir`** para guardar e remontar os caminhos.
- **Pilha** (pilha de chamadas da recursão + `Deque` de arestas) na DFS com backtracking.

## Algoritmos (item 3)

| Algoritmo | Para que serve aqui | Complexidade |
|---|---|---|
| BFS | Rota com **menos trechos**; alcançabilidade | O(V + E) |
| DFS clássica | Um caminho qualquer e a ordem de visita | O(V + E) |
| DFS com backtracking | **Todos** os caminhos simples (rotas alternativas) | exponencial no pior caso |
| Dijkstra + heap | **Melhor rota** no critério escolhido | O((V + E) log V) |
| DFS com poda (*branch and bound*) | Melhor rota quando o limite de outra métrica é violado | exponencial no pior caso |

BFS e DFS não consideram pesos; por isso a "melhor rota" ponderada é resolvida com Dijkstra, e BFS/DFS
respondem a "menos paradas" e "quais caminhos existem".

### Restrições

`Restricoes` é consultada a cada aresta explorada, então pode mudar entre uma busca e outra:
bloquear/liberar **localidade**, bloquear/liberar **trecho**, e definir **limites** de km, minutos e R$.
Quando o menor caminho de Dijkstra estoura um limite de outra métrica, o `Planejador` recorre à DFS com
poda para achar a melhor rota dentro dos limites (ou informar que não existe).

## API REST

| Método | Caminho | Descrição |
|---|---|---|
| GET | `/api/localidades` | vértices (nome, uf, coordenadas) |
| GET | `/api/estradas` | arestas (origem, destino, km, min, R$) |
| POST | `/api/rotas` | calcula rota(s) |

Exemplo de corpo do `POST /api/rotas` (a API não guarda estado; as restrições vão em cada chamada):

```json
{
  "origem": "Uberlândia",
  "destino": "Goiânia",
  "algoritmo": "DIJKSTRA",
  "criterio": "CUSTO",
  "restricoes": {
    "localidadesBloqueadas": ["Itumbiara"],
    "trechosBloqueados": [["Uberlândia", "Araguari"]],
    "distanciaMaximaKm": 500,
    "tempoMaximoMin": null,
    "custoMaximo": 300
  }
}
```

`algoritmo`: `DIJKSTRA` (padrão), `BFS` ou `DFS` (com `"maximo"` para limitar os caminhos devolvidos).
`criterio`: `DISTANCIA` (padrão), `TEMPO` ou `CUSTO`. Erros de entrada retornam HTTP 400 com `{"erro": "..."}`.

## Roteiro rápido para a apresentação

1. Uberlândia → Goiânia por **distância** (Dijkstra): passa por Tupaciguara e Itumbiara (321 km).
2. **Bloquear Itumbiara** e recalcular: desvia por Araguari e Catalão (403 km).
3. Uberlândia → Araxá por **distância** e por **custo**: rotas diferentes (pedágios de Uberaba vs. Monte Carmelo).
4. Definir **custo máximo** menor que a rota mais curta: o sistema muda para a rota que cabe no orçamento.
5. **BFS** (menos trechos) e **DFS** (todos os caminhos) para o mesmo par de cidades.
