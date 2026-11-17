# API de Conteúdos com Cache Próprio

API em **Spring Boot 4** (JDK 21+) que serve conteúdos (artigos, posts, páginas) a partir de uma
"fonte lenta" e usa um **cache feito à mão** com `ConcurrentHashMap` — sem `@Cacheable`, Caffeine ou Redis.

**Dupla:** João Pedro Coelho Guimarães

## Como rodar

Requisitos: JDK 21+ e Maven.

```bash
mvn spring-boot:run
```

A API sobe em `http://localhost:8080`. O banco é o H2 em memória (console em `/h2-console`,
JDBC URL `jdbc:h2:mem:conteudosdb`, usuário `sa`, senha vazia).

## Configuração (`application.properties`)

| Propriedade | Padrão | Significado |
|---|---|---|
| `app.cache.max-itens` | 5 | máximo de itens no cache |
| `app.cache.ttl-segundos` | 60 | tempo de vida de um item |
| `app.fonte.atraso-ms` | 2000 | atraso artificial da fonte lenta |

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| GET | `/conteudos/{id}` | busca no cache; se MISS, busca na fonte e armazena. Header `X-Cache: HIT` ou `MISS` |
| POST | `/conteudos` | cadastra |
| PUT | `/conteudos/{id}` | atualiza e invalida a entrada do cache |
| DELETE | `/conteudos/{id}` | exclui e invalida a entrada do cache |
| GET | `/cache/estatisticas` | tamanho, limite, hits, misses, evictions, hit ratio |
| DELETE | `/cache` | limpa o cache |

Corpo do POST/PUT:

```json
{ "titulo": "Meu artigo", "corpo": "Texto...", "autor": "Maria" }
```

## Como o cache funciona

```
GET /conteudos/3
      |
      v
 ConteudoService --> ConteudoCache.buscar(3)
      |                   |
      |        existe e não expirou? --sim--> HIT  (rápido, ~1 ms)
      |                   |
      |                  não
      v                   v
 busca na fonte lenta (sleep 2s + banco) --> ConteudoCache.guardar() --> MISS (lento)
```

- **Estrutura:** `Map<Long, EntradaCache>` (um `ConcurrentHashMap`). Cada `EntradaCache` guarda o
  conteúdo, o momento em que foi armazenado (para o TTL) e o último acesso (para o LRU).
- **TTL:** se `agora - armazenadoEm > ttl`, a entrada é removida e a busca conta como MISS.
- **LRU:** ao guardar um item novo com o cache cheio, percorre as entradas e remove a de menor
  `ultimoAcesso`. Cada remoção por limite conta como *eviction*.
- **Invalidação:** `PUT` e `DELETE` chamam `cache.invalidar(id)`.
- **Concorrência:** `ConcurrentHashMap` + contadores `AtomicLong`; o método `guardar` é
  `synchronized` para o limite não ser estourado por duas threads ao mesmo tempo.
- **Logs (SLF4J):**
  ```
  INFO  ConteudoCache : Cache MISS - id=3 - 2015 ms
  INFO  ConteudoCache : Cache HIT  - id=3 - 1 ms
  ```

## Organização do código

```
controller/  recebe/devolve DTO (conversão via Mapper)
service/     regra de negócio; recebe e retorna entidade; usa o cache
cache/       ConteudoCache e EntradaCache
repository/  JPA
model/       entidade Conteudo
dto/         ConteudoRequestDTO, ConteudoResponseDTO, EstatisticasCacheDTO
mapper/      ConteudoMapper
exception/   ConteudoNaoEncontradoException (404)
```

> Observação: o service devolve um `ResultadoBusca(conteudo, hit)`, que apenas embrulha a entidade
> com a informação de HIT/MISS para a controller montar o header `X-Cache`.

## Roteiro da demonstração (limite = 5)

Cadastre 7 conteúdos (ids 1 a 7):

```bash
for i in 1 2 3 4 5 6 7; do
  curl -s -X POST localhost:8080/conteudos -H "Content-Type: application/json" \
    -d "{\"titulo\":\"Artigo $i\",\"corpo\":\"Texto $i\",\"autor\":\"Maria\"}"
done
```

1. **MISS e HIT:** `curl -i localhost:8080/conteudos/1` duas vezes (compare `X-Cache` e o tempo).
2. **Limite/LRU:** busque os ids 1 a 5 (cache cheio), acesse o 1 de novo, depois busque o 6 →
   o id 2 (menos usado recentemente) é removido. Veja o log `Cache EVICTION`.
3. **TTL:** troque `app.cache.ttl-segundos=10`, busque um id, espere 11 s e busque de novo → MISS.
4. **Invalidação:** busque o id 1 (HIT), faça
   `curl -X PUT localhost:8080/conteudos/1 -H "Content-Type: application/json" -d '{"titulo":"Novo","corpo":"x","autor":"Ana"}'`
   e busque de novo → MISS.
5. **Estatísticas:** `curl localhost:8080/cache/estatisticas`

   Nota sobre as estatísticas: o print de /cache/estatisticas foi tirado em uma execução separada da que gerou os logs de LRU. Como os contadores ficam em memória, reiniciar a aplicação zera hits, misses e evictions. Por isso o valor de evictions ali aparece como 0, enquanto os logs de remoção mostram as evictions da sequência de testes.

## Prints da demonstração

<img width="1568" height="362" alt="image" src="https://github.com/user-attachments/assets/9d14e0c9-0a4c-4cd1-95bb-53f5a9bbfe38" />
<img width="1447" height="55" alt="image" src="https://github.com/user-attachments/assets/afb76849-26e9-4c74-9acf-c74a5be33f4c" />
<img width="1568" height="251" alt="image" src="https://github.com/user-attachments/assets/d26b5dd9-21c2-4814-872d-7d9d4fe22795" />
<img width="1568" height="300" alt="image" src="https://github.com/user-attachments/assets/3006ad51-732e-444c-b51a-a19bc50d434c" />
<img width="1438" height="55" alt="image" src="https://github.com/user-attachments/assets/6e02cdd9-e1a9-4a01-9556-52e6882a82fc" />
<img width="1568" height="386" alt="image" src="https://github.com/user-attachments/assets/4d49d1ea-4328-49e6-95e0-1c9107fd0b1f" />
<img width="735" height="105" alt="image" src="https://github.com/user-attachments/assets/190e114e-426b-4784-ad3b-010ef69a948f" />
<img width="1568" height="333" alt="image" src="https://github.com/user-attachments/assets/a1a1066e-d48d-43a1-9534-4fadee1c6917" />


