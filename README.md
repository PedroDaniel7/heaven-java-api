# Heaven - 3ª Sprint Java / FIAP

Turma: **1TDSPW**. Projeto: **Heaven**.

| Integrante | RM |
|---|---|
| Lucas Bossi | 572876 |
| Arthur de Souza | 573023 |
| Pedro Moraes | 569786 |
| Rick Nascimento | 571828 |
| Eduardo Oliveira | 573163 |

## Abrir no IntelliJ

1. Extraia o ZIP. Abra **File > Open** e selecione a pasta `Heaven` (ou seu `pom.xml`). Aguarde a importação Maven.
2. Em **File > Project Structure > Project**, selecione um **JDK 17 ou superior**, linguagem Java 17. O código foi compilado com `--release 17` no JDK 25 disponível neste ambiente.
3. No painel Maven, clique em **Reload All Maven Projects** para baixar `com.oracle.database.jdbc:ojdbc11:23.6.0.24.10`. A compilação usa apenas APIs do JDK; o driver é necessário para conectar ao Oracle.
4. O banco original já existe: **não execute novamente `database/Heaven-original.sql`**, pois seu início contém DROP TABLE. A cópia foi incluída para rastreabilidade e reconstrução em um schema novo e descartável.
5. No SQL Developer, conecte ao mesmo schema da aplicação. Execute com F5 apenas `database/02-carga-inicial.sql`. A carga usa MERGE, inclui parâmetros, níveis e tipos de exemplo e não substitui valores já existentes. Confira `USER_ERRORS` e os triggers VALID.
6. Edite `src/main/java/br/com/heaven/config/ConexaoOracle.java`: preencha as constantes URL, USUARIO e SENHA. As credenciais não foram fornecidas, então os valores atuais são marcadores, não credenciais funcionais. Exemplo de formato: `jdbc:oracle:thin:@//servidor:1521/servico`. Use o host, a porta e o serviço reais do seu Oracle; não confunda SID com service name.
7. Alternativamente, em **Run > Edit Configurations > Environment variables**, defina `HEAVEN_DB_URL`, `HEAVEN_DB_USER`, `HEAVEN_DB_PASSWORD`. Elas têm prioridade sobre as constantes. A opção de constantes existe para atender ao enunciado acadêmico.
8. Abra `br.com.heaven.app.Main` e execute o método `main`. Cadastre uma conta, registre uma ação e entre com outras três contas para votar em ações sorteadas. Com quórum 3 e limiar 60%, três aprovações aprovam a ação.

## Executar testes

Abra `br.com.heaven.test.TesteRegras` e execute `main`. **38 verificações locais passaram**; nenhum Oracle é necessário. Os testes fazem asserções explícitas e lançam AssertionError, sem depender de `-ea`.

No PowerShell também é possível definir JAVA_HOME para o JDK e executar `./executar-testes.ps1`. Esse script compila todas as classes e roda as regras. O Maven compila com `mvn compile`; **mvn test não executa esses testes main**.

Para integração, prepare **outro schema Oracle de testes**, execute o DDL original somente nesse schema vazio e depois a carga inicial. Configure as credenciais desse schema e execute `br.com.heaven.test.TesteOracle` com o argumento `--schema-teste`. O programa recusa schemas com usuários existentes, testa os seis cadastros CRUD, login, sorteio, aprovação/rejeição por triggers, pontos, reputação, cancelamento, limite e penalização. Todos os dados de teste terminam em rollback; as sequences de identity podem deixar lacunas. O teste usa uma conexão emprestada com savepoints para que os commits dos serviços não persistam fixtures.

**TesteOracle foi escrito e compilado, mas não executado neste ambiente: não foram fornecidos servidor e credenciais Oracle. A resolução Maven e o driver em execução também não foram verificados aqui.** Não há alegação de CRUD validado em banco real até executar esse teste.

## Arquitetura e regras

- `model`: 12 entidades do SQL com atributos privados, dois construtores (vazio/completo), getters e setters. As chaves estrangeiras são IDs Long, sem carregamento automático de associações.
- `persistence`: metadados Tabela/Coluna e helpers JDBC; não há Hibernate nem Spring.
- `dao`: um DAO por entidade. LeituraDAO mapeia ResultSet; RegistroDAO adiciona INSERT com retorno da identity; CrudDAO adiciona UPDATE e DELETE. Os seis cadastros CRUD são Usuario, NivelConfianca, TipoAcao, Conquista, ParametroSistema e IncidenteFraude.
- Históricos, votos, ações, penalizações e concessões de conquista são append-only nos DAOs: não se apaga o histórico para alterar saldos. A ação pode ser cancelada no serviço quando ainda está pendente e sem votos. UsuarioDAO edita apenas nome, email, senhaHash e statusConta; pontos, reputação, nível e streak são controlados pelas regras.
- `service`: cadastro/login, cálculo de impacto e pontos, limites diários, sorteio, voto com peso, cancelamento, ranking e penalização administrativa por incidente confirmado. `Regras` contém funções puras; a decisão de votação, reputação por voto e streak são espelhos testáveis das regras efetivamente executadas pelos triggers.
- `app/Main`: interface de console funcional; protótipos visuais no PDF representam uma futura interface gráfica, não telas já implementadas.
- `test`: classes main para testes locais e integração real.

Impacto = quantidade × fator do tipo, arredondado em 2 casas. Pontos = impacto arredondado × pontosBase, arredondado para inteiro, mínimo 1; impacto que arredonda para zero é recusado. Os fatores da carga são **didáticos**, não coeficientes ambientais comprovados. Não se somam unidades ambientais diferentes como se fossem kg de CO2.

A ação nasce PENDENTE. O usuário recebe uma ação aleatória de outro autor e pode votar uma vez. A oferta fica em memória por 10 minutos; enquanto válida é reutilizada. Reiniciar o programa perde as ofertas. O voto grava peso e reputação do momento. Os triggers encerram no primeiro quórum: aprovação quando a soma dos pesos favoráveis / soma de todos os pesos × 100 atinge o limiar; caso contrário, rejeição. Depois de encerrada não entram votos pelo serviço.

Uma aprovação credita os pontos pelo trigger, uma única vez no fluxo Java. O streak conta **dias em que aprovações geram GANHO_ACAO**, usando SYSDATE do Oracle, e não o dia em que a ação foi registrada. Mesmo dia mantém, dia seguinte incrementa, lacuna reinicia. O número armazenado não zera automaticamente durante inatividade; é recalculado na próxima aprovação.

Convergência com o resultado final dá +1 de reputação, divergência -2, limitado a 0..100. Convergência não prova a verdade da evidência. A reputação maior habilita níveis com mais peso e limites; não há recompensa extra em pontos por votar nesta sprint. O nível é atualizado ao operar o serviço e após o encerramento da votação. Não há bônus de streak automático nem motor de conquistas nesta versão.

Todas as mutações do serviço usam commit/rollback e bloqueiam a linha MIN_VALIDACOES_POR_ACAO com SELECT FOR UPDATE. Essa escolha conservadora serializa as operações Java e evita corridas entre triggers que alteram múltiplos usuários. É apropriada à demonstração, mas reduz throughput. Clientes que gravam SQL direto ou DAOs sem o serviço podem contornar esses controles; a solução não é uma API multiusuário de produção.

Os limites contam registros do dia Oracle, inclusive ações canceladas, para não permitir contorno por exclusão. Contas inativas e penalizações de bloqueio vigentes impedem operações. AplicarPenalizacao exige incidente CONFIRMADO, recusa duplicidade por incidente e reduz saldo/reputação sem ficar abaixo de zero. Incidentes são cadastrados/confirmados por operação administrativa via DAO; não há detecção automática de conluio ou painel administrativo. A penalização pontual é gravada como ENCERRADA porque já foi aplicada. Penalizações de bloqueio respeitam início/fim, mas o sistema não altera automaticamente STATUS_CONTA após expirar uma suspensão.

## Limites e cuidados do banco existente

Não foram modificados os triggers nem as tabelas do SQL recebido. O trigger de pontos só trata INSERT; atualizar/excluir HISTORICO_PONTOS diretamente deixa os agregados inconsistentes. O serviço e os DAOs de histórico evitam essas operações. O trigger de reputação grava motivos, mas não preenche ID_VALIDACAO. O sorteio em memória e a serialização global são limites conhecidos. A evidência é textual: não há upload, análise de imagem, geolocalização, confirmação científica de impacto ou proteção contra múltiplas contas.

Se a carga encontrar parâmetros preexistentes, confira que MIN_VALIDACOES_POR_ACAO seja inteiro >= 1, PERCENTUAL_APROVACAO_MINIMO esteja em (0,100], e exista um nível de reputação mínima zero. Não reexecute o DDL sobre dados que precisam ser preservados. Exclusões de cadastros referenciados falham pelas FKs, intencionalmente.

## Entrega

O ZIP contém a pasta completa `Heaven` e `Documentacao-Heaven.pdf`. A documentação contém capa preenchida, sumário paginado, escopo, funcionalidades, protótipos explicados, MER com todos os atributos, UML das 12 entidades e da infraestrutura, execução e validação. Os fontes de diagramas estão em `docs/mer.mmd` e `docs/classes.puml`, para edição futura. A correspondência segue o SQL e o código entregues; conceitos particulares ensinados em aula precisam ser conferidos pela equipe, pois esse material não foi fornecido.

## Modo de teste do sorteio (API)

`HEAVEN_SORTEIO_RESORTEAR` (padrão `false`; só vale `true` quando definida explicitamente) faz cada `POST /api/validacoes/sorteio` trocar a oferta anterior do usuário. É um **modo de teste**, usado pelo `docs/smoke-jornada.ps1`, e **NÃO deve ficar ligado em produção**: com ele, um validador pode chamar o sorteio várias vezes até cair na ação de um cúmplice. Na inicialização o log registra apenas `ativo` ou `inativo`.
