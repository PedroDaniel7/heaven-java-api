# Heaven — API

API REST do **Heaven**, plataforma de gamificação de ações sustentáveis. O usuário registra ações do dia a dia (transporte público, reciclagem, economia de energia), outros usuários as validam de forma anônima, e as ações aprovadas geram pontos, sequência de dias (streak) e reputação. Quem valida de acordo com o resultado final ganha reputação e mais peso de voto.

Projeto da disciplina de Java (FIAP, turma 1TDSPW), 2º semestre, Challenge.

## Links

| O quê | Endereço |
| --- | --- |
| Repositório (GitHub) | https://github.com/PedroDaniel7/heaven-java-api |
| API em produção | https://heaven-java-api.onrender.com |
| Swagger (produção) | https://heaven-java-api.onrender.com/swagger-ui/index.html |
| Front-end | https://heaven-frontend-2semestre.vercel.app |
| Contrato da API | [docs/contrato-api.md](docs/contrato-api.md) |
| Modelo entidade-relacionamento | [docs/mer.mmd](docs/mer.mmd) |
| Diagrama de classes (models) | [docs/classes-modelo.mmd](docs/classes-modelo.mmd) |
| Diagrama de classes (camadas) | [docs/classes-camadas.mmd](docs/classes-camadas.mmd) |

> **Cold start no Render:** a hospedagem gratuita põe a API para dormir quando fica sem uso. A primeira requisição depois disso pode levar cerca de um minuto. Chame `GET /api/saude` para acordar o servidor antes de usar o front ou o Swagger.

## Stack

- Java 17, Spring Boot 3.5 (Web, Validation, JDBC) e Maven Wrapper
- Oracle Database (schema da FIAP), acesso por JDBC puro com pool HikariCP
- Token próprio HMAC-SHA256, sem Spring Security
- springdoc-openapi (Swagger UI) e Docker

## Ferramentas e versões

| Ferramenta | Versão |
| --- | --- |
| JDK | 17 (o código é compilado com `--release 17`; roda em 17 ou superior) |
| Maven | Maven Wrapper (`./mvnw`), Maven 3.9.16; não precisa instalar o Maven |
| Spring Boot | 3.5.16 (Web, Validation, JDBC) |
| Banco de dados | Oracle Database (schema da FIAP) por JDBC, driver `ojdbc11` 23.6.0.24.10 |
| Documentação da API | springdoc-openapi 2.8.13 (Swagger UI) |
| IDE | IntelliJ IDEA |

## Como rodar

Pré-requisito: JDK 17 ou superior. As variáveis abaixo são lidas do ambiente; **nunca** coloque valores em arquivos versionados.

| Variável | Obrigatória | Para que serve |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | sim | URL JDBC do Oracle |
| `SPRING_DATASOURCE_USERNAME` | sim | usuário do banco |
| `SPRING_DATASOURCE_PASSWORD` | sim | senha do banco |
| `HEAVEN_TOKEN_SECRET` | sim | segredo do token (mínimo de 32 caracteres); sem ele a API não sobe |
| `HEAVEN_ADMIN_KEY` | não | chave das rotas `/api/admin` (mínimo de 24 caracteres); sem ela essas rotas devolvem 401 |
| `HEAVEN_CORS_ORIGINS` | não | origens do front permitidas, separadas por vírgula (padrão: `http://localhost:5173`) |
| `HEAVEN_SORTEIO_RESORTEAR` | não | **modo de teste** do sorteio (padrão: desligado). Não ligue em produção |
| `PORT` | não | porta HTTP (padrão: 8080) |

Build com testes e execução:

```bash
./mvnw clean package
```

```bash
java -jar target/heaven.jar
```

A API sobe em `http://localhost:8080` e o Swagger em `http://localhost:8080/swagger-ui/index.html`.

### Docker

```bash
docker build -t heaven-api .
```

```bash
docker run --rm -p 8080:8080 -e SPRING_DATASOURCE_URL -e SPRING_DATASOURCE_USERNAME -e SPRING_DATASOURCE_PASSWORD -e HEAVEN_TOKEN_SECRET heaven-api
```

Os `-e NOME` sem valor repassam as variáveis do seu ambiente para o contêiner.

## Banco de dados

O DDL está em [database/Heaven-original.sql](database/Heaven-original.sql) (12 tabelas, triggers e views) e a carga inicial em [database/02-carga-inicial.sql](database/02-carga-inicial.sql). **Não reexecute o DDL em um schema com dados**: o início dele apaga as tabelas.

## Verificação rápida

Os scripts em `docs/` testam a API em execução, criam usuários descartáveis em `example.test` e os desativam no fim:

```powershell
powershell -File docs/smoke.ps1 -BaseUrl https://heaven-java-api.onrender.com
```

`smoke-jornada.ps1` cobre sorteio, voto, pontos e reputação (exige o modo de teste do sorteio ligado no servidor) e `smoke-admin.ps1` cobre o CRUD administrativo (exige `HEAVEN_ADMIN_KEY` no ambiente).

## Integrantes

| Nome | RM |
| --- | --- |
| Arthur de Souza | 573023 |
| Eduardo Oliveira | 573163 |
| Lucas Bossi | 572876 |
| Pedro Moraes | 569786 |
| Rick Nascimento | 571828 |
