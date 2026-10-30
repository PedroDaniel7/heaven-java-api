# Contrato da API Heaven (Sprint 4)

Fonte única para o Java e o front. Se algo precisar mudar, atualize este arquivo nos DOIS repositórios antes de mexer no código.

- Base: `/api`. JSON UTF-8. Ids numéricos. Datas ISO-8601. Quantidades: número com até 2 casas.
- Autenticação: header `Authorization: Bearer <token>`. O `usuarioId` vem só do token, nunca do corpo.
- Token: HMAC-SHA256, validade de 12 h, sem Spring Security.
- Erros: sempre o mesmo corpo (abaixo). O front decide por `codigo`, nunca pelo texto de `mensagem`.

## Formato de erro (todo erro)

```json
{
  "status": 409,
  "codigo": "CONFLITO",
  "mensagem": "E-mail já cadastrado.",
  "campos": [ { "campo": "email", "mensagem": "já cadastrado" } ],
  "dataHora": "2026-10-30T00:18:20-03:00"
}
```

`campos` só vem preenchido em erro de validação (400). Nunca devolver SQL, host, stack trace nem `ORA-`.

Códigos: `VALIDACAO` (400), `NAO_AUTENTICADO` (401), `PROIBIDO` (403), `NAO_ENCONTRADO` (404), `CONFLITO` (409), `REGRA` (422), `OFERTA_EXPIRADA` (409), `BANCO_INDISPONIVEL` (503), `ERRO_INTERNO` (500).

O login confere a senha ANTES de revelar que a conta está inativa, para não vazar quais e-mails existem. Mensagem única para credencial inválida.

## Tipos

- `Usuario` = `{ id, nome, email, pontos, reputacao (0–100), nivel, streak, maiorStreak, dataCadastro }`. Nunca `senhaHash`, peso do voto ou nível interno.
- `TipoAcao` = `{ id, nome, desafio, categoria, unidade, dificuldade }`.
- `Acao` = `{ id, tipo: { id, nome, unidade }, quantidade, evidencia, status, pontosPrevistos, dataHora, dataConclusao }`, com `status` em `PENDENTE | APROVADA | REJEITADA | CANCELADA`. A lista de votos nunca aparece.
- `AcaoParaValidar` = `{ id, tipo: { nome, unidade, desafio }, quantidade, evidencia, dataHora }`. Sem autor (anonimato).
- `Voto` = `APROVAR | REJEITAR`.

## Endpoints do front (16)

### Saúde, cadastro e perfil

| Endpoint | Acesso | Entrada | Sucesso | Erros |
| --- | --- | --- | --- | --- |
| `GET /api/saude` | Público | — | 200 `{status, dataHora}` (sem banco; serve para acordar o servidor) | — |
| `GET /api/saude/banco` | Público | — | 200 `{status, dataHora}` | 503 |
| `POST /api/usuarios` | Público | `{nome, email, senha}` (senha ≥ 8) | 201 `Usuario` + `Location` | 400, 409 e-mail já usado |
| `POST /api/auth/login` | Público | `{email, senha}` | 200 `{token, expiraEm, usuario}` | 400, 401 |
| `GET /api/usuarios/me` | Token | — | 200 `Usuario` | 401 |
| `PUT /api/usuarios/me` | Token | `{nome, email, novaSenha?}` | 200 `Usuario` | 400, 401, 409 |
| `DELETE /api/usuarios/me` | Token | — | 204 (desativa a conta: `INATIVO`) | 401 |

### Tipos de ação e ações do usuário

| Endpoint | Acesso | Entrada | Sucesso | Erros |
| --- | --- | --- | --- | --- |
| `GET /api/tipos-acao` | Público | — | 200 lista de `TipoAcao` ativos (`[]` se vazio) | — |
| `GET /api/tipos-acao/{id}` | Público | — | 200 `TipoAcao` | 404 |
| `POST /api/acoes` | Token | `{idTipoAcao, quantidade, evidencia}` | 201 `Acao` + `Location` | 400, 401, 403 bloqueio, 404 tipo, 422 limite diário ou tipo inativo |
| `GET /api/acoes/minhas` | Token | — | 200 lista de `Acao`, mais recentes primeiro | 401 |
| `GET /api/acoes/{id}` | Token | — | 200 `Acao` | 401, 404 (também para ação de outro usuário) |
| `DELETE /api/acoes/{id}` | Token | — | 204 (cancelamento lógico: `CANCELADA`) | 401, 404, 409 já concluída ou com votos |

### Validação e ranking

| Endpoint | Acesso | Entrada | Sucesso | Erros |
| --- | --- | --- | --- | --- |
| `POST /api/validacoes/sorteio` | Token | — | 200 `{acao: AcaoParaValidar, expiraEm}`; 204 se não há ação elegível | 401, 403 bloqueio, 422 limite diário |
| `POST /api/validacoes` | Token | `{idAcao, voto, justificativa?}` | 201 `{idAcao, voto, acaoConcluida}` | 400, 401, 403 autovalidação ou bloqueio, 404, 409 (`OFERTA_EXPIRADA`, voto duplicado ou ação concluída), 422 limite |
| `GET /api/ranking?limite=20` | Público | `limite` de 1 a 100 | 200 lista `{posicao, nome, pontos, nivel, streak}` | 400 |

O sorteio usa POST porque guarda a oferta por 10 minutos (em memória). Sem oferta válida, o voto devolve 409 `OFERTA_EXPIRADA` e o front pede novo sorteio.

## Administração (header `X-Admin-Key`, valor em variável de ambiente `HEAVEN_ADMIN_KEY`)

| Endpoint | Sucesso | Erros |
| --- | --- | --- |
| `GET /api/admin/tipos-acao` | 200 todos, inclusive inativos | 401 |
| `POST /api/admin/tipos-acao` | 201 | 400, 401, 409 nome repetido |
| `PUT /api/admin/tipos-acao/{id}` | 200 | 400, 401, 404 |
| `DELETE /api/admin/tipos-acao/{id}` | 204 | 401, 404, 409 em uso |

## Decisões que afetam o contrato

- Voto binário. `justificativa` é opcional (a regra antiga que a exigia sai).
- Evidência é texto nesta sprint (D6). `quantidade` é obrigatória (D7).
- `TipoAcao.desafio` vem de `TIPO_ACAO.DESCRICAO` (D8).
- O banco já tem carga própria da disciplina de BD (10 usuários com hash SHA-256, que NÃO logam pela API; cadastre usuários novos pelo front).
