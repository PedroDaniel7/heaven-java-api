/* ============================================================================
   PROJETO: PLATAFORMA DE GAMIFICACAO SUSTENTAVEL (HEAVEN)
   BANCO: ORACLE (Oracle SQL Developer)
   ARQUIVO: DDL PURO — apenas estrutura do banco de dados
   (CREATE TABLE, constraints, indices, triggers e views).
   Nao contem dados de teste (INSERT) nem consultas de demonstracao (SELECT).
   Para a versao com dados de teste e consultas, ver
   gamificacao_sustentavel_ORACLE_FINAL.sql.
   ============================================================================
   Ordem do script:
   1. DROP das tabelas (tratado para nao falhar se nao existirem)
   2. CREATE TABLE (com constraints inline: PK, FK, UNIQUE, CHECK, NOT NULL, DEFAULT)
   3. INDICES adicionais (FKs nao sao indexadas automaticamente no Oracle)
   4. TRIGGERS
   5. VIEWS
   ============================================================================ */


-- ============================================================================
-- 1. DROP DAS TABELAS (ordem inversa de dependencia, ignorando erro se nao existir)
-- ============================================================================
BEGIN
   FOR t IN (
      SELECT ord, table_name
      FROM (
         SELECT 1 ord, 'PENALIZACAO' table_name FROM dual UNION ALL
         SELECT 2, 'INCIDENTE_FRAUDE' FROM dual UNION ALL
         SELECT 3, 'USUARIO_CONQUISTA' FROM dual UNION ALL
         SELECT 4, 'CONQUISTA' FROM dual UNION ALL
         SELECT 5, 'HISTORICO_REPUTACAO' FROM dual UNION ALL
         SELECT 6, 'HISTORICO_PONTOS' FROM dual UNION ALL
         SELECT 7, 'VALIDACAO' FROM dual UNION ALL
         SELECT 8, 'ACAO_USUARIO' FROM dual UNION ALL
         SELECT 9, 'TIPO_ACAO' FROM dual UNION ALL
         SELECT 10, 'PARAMETRO_SISTEMA' FROM dual UNION ALL
         SELECT 11, 'USUARIO' FROM dual UNION ALL
         SELECT 12, 'NIVEL_CONFIANCA' FROM dual
      )
      ORDER BY ord
   ) LOOP
      BEGIN
         EXECUTE IMMEDIATE 'DROP TABLE ' || t.table_name || ' CASCADE CONSTRAINTS PURGE';
      EXCEPTION
         WHEN OTHERS THEN
            -- Ignora qualquer erro de DROP (tabela nao existe, ja foi removida,
            -- ou ficou em estado parcial de uma execucao anterior do script).
            -- Isso garante que o script sempre possa ser reexecutado do zero
            -- sem travar no meio do bloco de limpeza.
            NULL;
      END;
   END LOOP;
END;
/

-- ----------------------------------------------------------------------------
-- Limpeza de INDICES ORFAOS de PK/UNIQUE que podem ter sobrado de uma execucao
-- anterior mal-sucedida (quando o DROP TABLE nao apaga o indice porque a
-- tabela ja nao existia mais, mas o indice da constraint continuou no schema).
-- Sem isso, o CREATE TABLE seguinte falha com ORA-00955 (nome ja em uso).
-- ----------------------------------------------------------------------------
BEGIN
   FOR idx IN (
      SELECT index_name FROM (
         SELECT 'PK_ACAO_USUARIO' index_name FROM dual UNION ALL
         SELECT 'PK_CONQUISTA' FROM dual UNION ALL
         SELECT 'PK_HISTORICO_PONTOS' FROM dual UNION ALL
         SELECT 'PK_HISTORICO_REPUTACAO' FROM dual UNION ALL
         SELECT 'PK_INCIDENTE_FRAUDE' FROM dual UNION ALL
         SELECT 'PK_NIVEL_CONFIANCA' FROM dual UNION ALL
         SELECT 'PK_PARAMETRO_SISTEMA' FROM dual UNION ALL
         SELECT 'PK_PENALIZACAO' FROM dual UNION ALL
         SELECT 'PK_TIPO_ACAO' FROM dual UNION ALL
         SELECT 'PK_USUARIO' FROM dual UNION ALL
         SELECT 'PK_USUARIO_CONQUISTA' FROM dual UNION ALL
         SELECT 'PK_VALIDACAO' FROM dual UNION ALL
         SELECT 'UK_CONQUISTA_NOME' FROM dual UNION ALL
         SELECT 'UK_NIVEL_CONFIANCA_NOME' FROM dual UNION ALL
         SELECT 'UK_PARAMETRO_NOME' FROM dual UNION ALL
         SELECT 'UK_TIPO_ACAO_NOME' FROM dual UNION ALL
         SELECT 'UK_USUARIO_EMAIL' FROM dual UNION ALL
         SELECT 'UK_VALIDACAO_ACAO_VALIDADOR' FROM dual
      )
   ) LOOP
      BEGIN
         EXECUTE IMMEDIATE 'DROP INDEX ' || idx.index_name;
      EXCEPTION
         WHEN OTHERS THEN
            NULL; -- indice nao existe, ou ja foi removido junto com a tabela
      END;
   END LOOP;
END;
/


-- ============================================================================
-- 2. TABELAS
-- ============================================================================

-- ---------------------------------------------------------------------------
-- NIVEL_CONFIANCA
-- ---------------------------------------------------------------------------
CREATE TABLE NIVEL_CONFIANCA (
   ID                     NUMBER GENERATED ALWAYS AS IDENTITY,
   NOME                   VARCHAR2(30)   NOT NULL,
   REPUTACAO_MINIMA       NUMBER(5,2)    NOT NULL,
   PESO_VOTO              NUMBER(4,2)    NOT NULL,
   LIMITE_VALIDACOES_DIA  NUMBER(4)      NOT NULL,
   LIMITE_ACOES_DIA       NUMBER(4)      NOT NULL,
   CONSTRAINT PK_NIVEL_CONFIANCA PRIMARY KEY (ID),
   CONSTRAINT UK_NIVEL_CONFIANCA_NOME UNIQUE (NOME),
   CONSTRAINT CK_NIVEL_PESO_VOTO CHECK (PESO_VOTO > 0),
   CONSTRAINT CK_NIVEL_REPUTACAO_MIN CHECK (REPUTACAO_MINIMA BETWEEN 0 AND 100),
   CONSTRAINT CK_NIVEL_LIM_VALID CHECK (LIMITE_VALIDACOES_DIA > 0),
   CONSTRAINT CK_NIVEL_LIM_ACOES CHECK (LIMITE_ACOES_DIA > 0)
);

-- ---------------------------------------------------------------------------
-- USUARIO
-- ---------------------------------------------------------------------------
CREATE TABLE USUARIO (
   ID                   NUMBER GENERATED ALWAYS AS IDENTITY,
   NOME                 VARCHAR2(120)  NOT NULL,
   EMAIL                VARCHAR2(150)  NOT NULL,
   SENHA_HASH           VARCHAR2(256)  NOT NULL,
   DATA_CADASTRO        DATE           DEFAULT SYSDATE NOT NULL,
   STATUS_CONTA         VARCHAR2(20)   DEFAULT 'ATIVO' NOT NULL,
   PONTOS_ATUAIS        NUMBER(10)     DEFAULT 0 NOT NULL,
   REPUTACAO_ATUAL      NUMBER(5,2)    DEFAULT 50 NOT NULL,
   ID_NIVEL_CONFIANCA   NUMBER         NOT NULL,
   STREAK_ATUAL         NUMBER(6)      DEFAULT 0 NOT NULL,
   MAIOR_STREAK         NUMBER(6)      DEFAULT 0 NOT NULL,
   DATA_ULTIMA_ACAO     DATE,
   CONSTRAINT PK_USUARIO PRIMARY KEY (ID),
   CONSTRAINT UK_USUARIO_EMAIL UNIQUE (EMAIL),
   CONSTRAINT FK_USUARIO_NIVEL FOREIGN KEY (ID_NIVEL_CONFIANCA)
       REFERENCES NIVEL_CONFIANCA (ID),
   CONSTRAINT CK_USUARIO_STATUS CHECK (STATUS_CONTA IN ('ATIVO','SUSPENSO','BLOQUEADO','INATIVO')),
   CONSTRAINT CK_USUARIO_PONTOS CHECK (PONTOS_ATUAIS >= 0),
   CONSTRAINT CK_USUARIO_REPUTACAO CHECK (REPUTACAO_ATUAL BETWEEN 0 AND 100),
   CONSTRAINT CK_USUARIO_STREAK CHECK (STREAK_ATUAL >= 0),
   CONSTRAINT CK_USUARIO_MAIOR_STREAK CHECK (MAIOR_STREAK >= 0)
);

-- ---------------------------------------------------------------------------
-- TIPO_ACAO
-- ---------------------------------------------------------------------------
CREATE TABLE TIPO_ACAO (
   ID                            NUMBER GENERATED ALWAYS AS IDENTITY,
   NOME                          VARCHAR2(100)  NOT NULL,
   DESCRICAO                     VARCHAR2(500),
   CATEGORIA                     VARCHAR2(30)   NOT NULL,
   UNIDADE_MEDIDA                VARCHAR2(20)   NOT NULL,
   IMPACTO_AMBIENTAL_ESTIMADO    NUMBER(10,2)   NOT NULL,
   PONTOS_BASE                   NUMBER(6)      NOT NULL,
   DIFICULDADE                   VARCHAR2(15)   NOT NULL,
   STATUS_ATIVO                  CHAR(1)        DEFAULT 'S' NOT NULL,
   CONSTRAINT PK_TIPO_ACAO PRIMARY KEY (ID),
   CONSTRAINT UK_TIPO_ACAO_NOME UNIQUE (NOME),
   CONSTRAINT CK_TIPO_ACAO_CATEGORIA CHECK (CATEGORIA IN
       ('TRANSPORTE','ENERGIA','AGUA','RECICLAGEM','PLANTIO','CONSUMO_CONSCIENTE','OUTRO')),
   CONSTRAINT CK_TIPO_ACAO_IMPACTO CHECK (IMPACTO_AMBIENTAL_ESTIMADO > 0),
   CONSTRAINT CK_TIPO_ACAO_PONTOS CHECK (PONTOS_BASE > 0),
   CONSTRAINT CK_TIPO_ACAO_DIFICULDADE CHECK (DIFICULDADE IN ('FACIL','MEDIA','DIFICIL')),
   CONSTRAINT CK_TIPO_ACAO_ATIVO CHECK (STATUS_ATIVO IN ('S','N'))
);

-- ---------------------------------------------------------------------------
-- ACAO_USUARIO
-- ---------------------------------------------------------------------------
CREATE TABLE ACAO_USUARIO (
   ID                            NUMBER GENERATED ALWAYS AS IDENTITY,
   ID_USUARIO                    NUMBER         NOT NULL,
   ID_TIPO_ACAO                  NUMBER         NOT NULL,
   DATA_HORA                     TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
   QUANTIDADE                    NUMBER(10,2)   NOT NULL,
   IMPACTO_AMBIENTAL_CALCULADO   NUMBER(12,2)   NOT NULL,
   PONTOS_PREVISTOS              NUMBER(8)      NOT NULL,
   STATUS                        VARCHAR2(15)   DEFAULT 'PENDENTE' NOT NULL,
   DESCRICAO_EVIDENCIA           VARCHAR2(500),
   DATA_CONCLUSAO                DATE,
   CONSTRAINT PK_ACAO_USUARIO PRIMARY KEY (ID),
   CONSTRAINT FK_ACAO_USUARIO_USUARIO FOREIGN KEY (ID_USUARIO)
       REFERENCES USUARIO (ID),
   CONSTRAINT FK_ACAO_USUARIO_TIPO FOREIGN KEY (ID_TIPO_ACAO)
       REFERENCES TIPO_ACAO (ID),
   CONSTRAINT CK_ACAO_QUANTIDADE CHECK (QUANTIDADE > 0),
   CONSTRAINT CK_ACAO_IMPACTO CHECK (IMPACTO_AMBIENTAL_CALCULADO >= 0),
   CONSTRAINT CK_ACAO_PONTOS CHECK (PONTOS_PREVISTOS >= 0),
   CONSTRAINT CK_ACAO_STATUS CHECK (STATUS IN ('PENDENTE','APROVADA','REJEITADA','CANCELADA'))
);

-- ---------------------------------------------------------------------------
-- VALIDACAO
-- ---------------------------------------------------------------------------
CREATE TABLE VALIDACAO (
   ID                             NUMBER GENERATED ALWAYS AS IDENTITY,
   ID_ACAO                        NUMBER        NOT NULL,
   ID_VALIDADOR                   NUMBER        NOT NULL,
   VOTO                            VARCHAR2(10)  NOT NULL,
   PESO_VOTO                       NUMBER(4,2)   NOT NULL,
   REPUTACAO_VALIDADOR_MOMENTO     NUMBER(5,2)   NOT NULL,
   DATA_HORA                       TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
   JUSTIFICATIVA                   VARCHAR2(500),
   CONSTRAINT PK_VALIDACAO PRIMARY KEY (ID),
   CONSTRAINT FK_VALIDACAO_ACAO FOREIGN KEY (ID_ACAO)
       REFERENCES ACAO_USUARIO (ID),
   CONSTRAINT FK_VALIDACAO_VALIDADOR FOREIGN KEY (ID_VALIDADOR)
       REFERENCES USUARIO (ID),
   CONSTRAINT UK_VALIDACAO_ACAO_VALIDADOR UNIQUE (ID_ACAO, ID_VALIDADOR),
   CONSTRAINT CK_VALIDACAO_VOTO CHECK (VOTO IN ('APROVAR','REJEITAR')),
   CONSTRAINT CK_VALIDACAO_PESO CHECK (PESO_VOTO > 0)
);

-- ---------------------------------------------------------------------------
-- HISTORICO_PONTOS
-- ---------------------------------------------------------------------------
CREATE TABLE HISTORICO_PONTOS (
   ID                   NUMBER GENERATED ALWAYS AS IDENTITY,
   ID_USUARIO           NUMBER         NOT NULL,
   QUANTIDADE           NUMBER(10)     NOT NULL,
   TIPO_MOVIMENTACAO    VARCHAR2(25)   NOT NULL,
   DESCRICAO            VARCHAR2(300),
   ID_ACAO              NUMBER,
   DATA_HORA            TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
   CONSTRAINT PK_HISTORICO_PONTOS PRIMARY KEY (ID),
   CONSTRAINT FK_HIST_PONTOS_USUARIO FOREIGN KEY (ID_USUARIO)
       REFERENCES USUARIO (ID),
   CONSTRAINT FK_HIST_PONTOS_ACAO FOREIGN KEY (ID_ACAO)
       REFERENCES ACAO_USUARIO (ID),
   CONSTRAINT CK_HIST_PONTOS_QTD CHECK (QUANTIDADE <> 0),
   CONSTRAINT CK_HIST_PONTOS_TIPO CHECK (TIPO_MOVIMENTACAO IN
       ('GANHO_ACAO','BONUS_STREAK','CONQUISTA','PENALIZACAO','CORRECAO_ADMIN'))
);

-- ---------------------------------------------------------------------------
-- HISTORICO_REPUTACAO
-- ---------------------------------------------------------------------------
CREATE TABLE HISTORICO_REPUTACAO (
   ID                   NUMBER GENERATED ALWAYS AS IDENTITY,
   ID_USUARIO           NUMBER         NOT NULL,
   REPUTACAO_ANTERIOR   NUMBER(5,2)    NOT NULL,
   REPUTACAO_NOVA       NUMBER(5,2)    NOT NULL,
   MOTIVO               VARCHAR2(200)  NOT NULL,
   ID_VALIDACAO         NUMBER,
   DATA_HORA            TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
   CONSTRAINT PK_HISTORICO_REPUTACAO PRIMARY KEY (ID),
   CONSTRAINT FK_HIST_REPUT_USUARIO FOREIGN KEY (ID_USUARIO)
       REFERENCES USUARIO (ID),
   CONSTRAINT FK_HIST_REPUT_VALIDACAO FOREIGN KEY (ID_VALIDACAO)
       REFERENCES VALIDACAO (ID)
);

-- ---------------------------------------------------------------------------
-- CONQUISTA
-- ---------------------------------------------------------------------------
CREATE TABLE CONQUISTA (
   ID              NUMBER GENERATED ALWAYS AS IDENTITY,
   NOME            VARCHAR2(100)  NOT NULL,
   DESCRICAO       VARCHAR2(300),
   REQUISITO       VARCHAR2(300)  NOT NULL,
   PONTOS_BONUS    NUMBER(8)      DEFAULT 0 NOT NULL,
   CONSTRAINT PK_CONQUISTA PRIMARY KEY (ID),
   CONSTRAINT UK_CONQUISTA_NOME UNIQUE (NOME),
   CONSTRAINT CK_CONQUISTA_PONTOS CHECK (PONTOS_BONUS >= 0)
);

-- ---------------------------------------------------------------------------
-- USUARIO_CONQUISTA
-- ---------------------------------------------------------------------------
CREATE TABLE USUARIO_CONQUISTA (
   ID_USUARIO       NUMBER   NOT NULL,
   ID_CONQUISTA     NUMBER   NOT NULL,
   DATA_CONQUISTA   DATE     DEFAULT SYSDATE NOT NULL,
   CONSTRAINT PK_USUARIO_CONQUISTA PRIMARY KEY (ID_USUARIO, ID_CONQUISTA),
   CONSTRAINT FK_USU_CONQ_USUARIO FOREIGN KEY (ID_USUARIO)
       REFERENCES USUARIO (ID),
   CONSTRAINT FK_USU_CONQ_CONQUISTA FOREIGN KEY (ID_CONQUISTA)
       REFERENCES CONQUISTA (ID)
);

-- ---------------------------------------------------------------------------
-- INCIDENTE_FRAUDE
-- ---------------------------------------------------------------------------
CREATE TABLE INCIDENTE_FRAUDE (
   ID                 NUMBER GENERATED ALWAYS AS IDENTITY,
   ID_USUARIO         NUMBER         NOT NULL,
   TIPO_INCIDENTE     VARCHAR2(40)   NOT NULL,
   DESCRICAO          VARCHAR2(500),
   NIVEL_SEVERIDADE   VARCHAR2(10)   NOT NULL,
   DATA_HORA          TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
   STATUS             VARCHAR2(20)   DEFAULT 'ABERTO' NOT NULL,
   CONSTRAINT PK_INCIDENTE_FRAUDE PRIMARY KEY (ID),
   CONSTRAINT FK_INCIDENTE_USUARIO FOREIGN KEY (ID_USUARIO)
       REFERENCES USUARIO (ID),
   CONSTRAINT CK_INCIDENTE_TIPO CHECK (TIPO_INCIDENTE IN
       ('EXCESSO_ACOES','EXCESSO_VALIDACOES','PADRAO_SUSPEITO_VOTOS',
        'MUITAS_REJEICOES','AUTOVALIDACAO','COMPORTAMENTO_ANORMAL')),
   CONSTRAINT CK_INCIDENTE_SEVERIDADE CHECK (NIVEL_SEVERIDADE IN ('BAIXO','MEDIO','ALTO')),
   CONSTRAINT CK_INCIDENTE_STATUS CHECK (STATUS IN ('ABERTO','EM_ANALISE','CONFIRMADO','DESCARTADO'))
);

-- ---------------------------------------------------------------------------
-- PENALIZACAO
-- ---------------------------------------------------------------------------
CREATE TABLE PENALIZACAO (
   ID                     NUMBER GENERATED ALWAYS AS IDENTITY,
   ID_USUARIO             NUMBER         NOT NULL,
   ID_INCIDENTE           NUMBER,
   TIPO_PENALIZACAO       VARCHAR2(30)   NOT NULL,
   DESCRICAO              VARCHAR2(300),
   PONTOS_PERDIDOS        NUMBER(8)      DEFAULT 0 NOT NULL,
   REPUTACAO_REDUZIDA     NUMBER(5,2)    DEFAULT 0 NOT NULL,
   DATA_INICIO            DATE           DEFAULT SYSDATE NOT NULL,
   DATA_FIM               DATE,
   STATUS                 VARCHAR2(15)   DEFAULT 'ATIVA' NOT NULL,
   CONSTRAINT PK_PENALIZACAO PRIMARY KEY (ID),
   CONSTRAINT FK_PENALIZACAO_USUARIO FOREIGN KEY (ID_USUARIO)
       REFERENCES USUARIO (ID),
   CONSTRAINT FK_PENALIZACAO_INCIDENTE FOREIGN KEY (ID_INCIDENTE)
       REFERENCES INCIDENTE_FRAUDE (ID),
   CONSTRAINT CK_PENALIZACAO_TIPO CHECK (TIPO_PENALIZACAO IN
       ('PERDA_PONTOS','REDUCAO_REPUTACAO','BLOQUEIO_VALIDACAO','BLOQUEIO_ACAO','SUSPENSAO_CONTA')),
   CONSTRAINT CK_PENALIZACAO_PONTOS CHECK (PONTOS_PERDIDOS >= 0),
   CONSTRAINT CK_PENALIZACAO_REPUTACAO CHECK (REPUTACAO_REDUZIDA >= 0),
   CONSTRAINT CK_PENALIZACAO_STATUS CHECK (STATUS IN ('ATIVA','ENCERRADA','REVOGADA'))
);

-- ---------------------------------------------------------------------------
-- PARAMETRO_SISTEMA  (limites configuraveis, sem numeros fixos espalhados)
-- ---------------------------------------------------------------------------
CREATE TABLE PARAMETRO_SISTEMA (
   ID               NUMBER GENERATED ALWAYS AS IDENTITY,
   NOME_PARAMETRO   VARCHAR2(50)   NOT NULL,
   VALOR            NUMBER(10,2)   NOT NULL,
   DESCRICAO        VARCHAR2(300),
   CONSTRAINT PK_PARAMETRO_SISTEMA PRIMARY KEY (ID),
   CONSTRAINT UK_PARAMETRO_NOME UNIQUE (NOME_PARAMETRO)
);


-- ============================================================================
-- 3. INDICES  (FKs nao sao indexadas automaticamente no Oracle; UNIQUE ja
--    cria indice implicito, entao nao duplicamos indice para email, etc.)
-- ============================================================================
CREATE INDEX IX_ACAO_USUARIO_USUARIO   ON ACAO_USUARIO (ID_USUARIO);
CREATE INDEX IX_ACAO_USUARIO_TIPO      ON ACAO_USUARIO (ID_TIPO_ACAO);
CREATE INDEX IX_ACAO_USUARIO_STATUS    ON ACAO_USUARIO (STATUS);
CREATE INDEX IX_ACAO_USUARIO_DATA      ON ACAO_USUARIO (DATA_HORA);
CREATE INDEX IX_VALIDACAO_ACAO         ON VALIDACAO (ID_ACAO);
CREATE INDEX IX_VALIDACAO_VALIDADOR    ON VALIDACAO (ID_VALIDADOR);
CREATE INDEX IX_HIST_PONTOS_USUARIO    ON HISTORICO_PONTOS (ID_USUARIO);
CREATE INDEX IX_HIST_REPUT_USUARIO     ON HISTORICO_REPUTACAO (ID_USUARIO);
CREATE INDEX IX_INCIDENTE_USUARIO      ON INCIDENTE_FRAUDE (ID_USUARIO);
CREATE INDEX IX_PENALIZACAO_USUARIO    ON PENALIZACAO (ID_USUARIO);
CREATE INDEX IX_USUARIO_PONTOS         ON USUARIO (PONTOS_ATUAIS DESC);
-- Justificativa resumida:
--  - IX_ACAO_USUARIO_USUARIO/TIPO/STATUS/DATA: usados constantemente em joins
--    e filtros (historico do usuario, fila de pendentes, ordenacao por data).
--  - IX_VALIDACAO_*: usados pelo trigger de finalizacao e por consultas de
--    taxa de acerto do validador.
--  - IX_USUARIO_PONTOS: acelera a VIEW de ranking (ORDER BY pontos DESC).
--  - Nao indexamos colunas de baixa seletividade isoladas (ex.: status_conta)
--    porque teriam poucas distintas e alto custo de manutencao sem retorno.


-- ============================================================================
-- 4. TRIGGERS
-- ============================================================================

-- ---------------------------------------------------------------------------
-- TRG_VALIDACAO_PROPRIA_ACAO
-- Problema que resolve: impedir que um usuario valide a propria acao.
-- Quando executa: BEFORE INSERT em VALIDACAO, para cada linha.
-- O que altera: nada; apenas bloqueia o INSERT levantando erro de aplicacao.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_VALIDACAO_PROPRIA_ACAO
BEFORE INSERT ON VALIDACAO
FOR EACH ROW
DECLARE
   v_dono_acao ACAO_USUARIO.ID_USUARIO%TYPE;
BEGIN
   SELECT ID_USUARIO INTO v_dono_acao
     FROM ACAO_USUARIO
    WHERE ID = :NEW.ID_ACAO;

   IF v_dono_acao = :NEW.ID_VALIDADOR THEN
      RAISE_APPLICATION_ERROR(-20001, 'Um usuario nao pode validar a propria acao.');
   END IF;
END;
/

-- ---------------------------------------------------------------------------
-- TRG_HISTORICO_PONTOS_AIUD
-- Problema que resolve: mantem USUARIO.pontos_atuais e o streak sempre
-- sincronizados com o historico de pontos, sem exigir que a aplicacao faca
-- dois comandos separados (evita inconsistencia).
-- Quando executa: AFTER INSERT em HISTORICO_PONTOS, para cada linha.
-- O que altera: USUARIO.pontos_atuais sempre; e, quando o lancamento e do
-- tipo GANHO_ACAO, tambem recalcula streak_atual, maior_streak e
-- data_ultima_acao (regra: mesma data mantem, +1 dia incrementa, gap reseta).
-- ---------------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_HISTORICO_PONTOS_AIUD
AFTER INSERT ON HISTORICO_PONTOS
FOR EACH ROW
DECLARE
   v_data_ultima  USUARIO.DATA_ULTIMA_ACAO%TYPE;
   v_streak       USUARIO.STREAK_ATUAL%TYPE;
   v_maior        USUARIO.MAIOR_STREAK%TYPE;
BEGIN
   UPDATE USUARIO
      SET PONTOS_ATUAIS = PONTOS_ATUAIS + :NEW.QUANTIDADE
    WHERE ID = :NEW.ID_USUARIO
    RETURNING DATA_ULTIMA_ACAO, STREAK_ATUAL, MAIOR_STREAK
         INTO v_data_ultima, v_streak, v_maior;

   IF :NEW.TIPO_MOVIMENTACAO = 'GANHO_ACAO' THEN
      IF v_data_ultima IS NULL OR TRUNC(SYSDATE) - TRUNC(v_data_ultima) > 1 THEN
         v_streak := 1;
      ELSIF TRUNC(SYSDATE) - TRUNC(v_data_ultima) = 1 THEN
         v_streak := v_streak + 1;
      END IF; -- se a diferenca for 0 (mesmo dia), mantem o streak atual

      v_maior := GREATEST(v_maior, v_streak);

      UPDATE USUARIO
         SET STREAK_ATUAL     = v_streak,
             MAIOR_STREAK     = v_maior,
             DATA_ULTIMA_ACAO = SYSDATE
       WHERE ID = :NEW.ID_USUARIO;
   END IF;
END;
/

-- ---------------------------------------------------------------------------
-- TRG_ATUALIZA_REPUTACAO_USUARIO
-- Problema que resolve: mantem USUARIO.reputacao_atual sincronizada com o
-- historico de reputacao (fonte da verdade), sem sobrescrever o historico.
-- Quando executa: AFTER INSERT em HISTORICO_REPUTACAO, para cada linha.
-- O que altera: USUARIO.reputacao_atual.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_ATUALIZA_REPUTACAO_USUARIO
AFTER INSERT ON HISTORICO_REPUTACAO
FOR EACH ROW
BEGIN
   UPDATE USUARIO
      SET REPUTACAO_ATUAL = :NEW.REPUTACAO_NOVA
    WHERE ID = :NEW.ID_USUARIO;
END;
/

-- ---------------------------------------------------------------------------
-- TRG_PONTOS_ACAO_APROVADA
-- Problema que resolve: quando uma acao passa a APROVADA, gera automatica-
-- mente o lancamento de pontos correspondente (sem depender da aplicacao
-- lembrar de fazer isso em dois passos).
-- Quando executa: AFTER UPDATE OF STATUS em ACAO_USUARIO, para cada linha,
-- apenas na transicao PENDENTE -> APROVADA.
-- O que altera: insere uma linha em HISTORICO_PONTOS (que por sua vez
-- dispara o TRG_HISTORICO_PONTOS_AIUD acima).
-- ---------------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_PONTOS_ACAO_APROVADA
AFTER UPDATE OF STATUS ON ACAO_USUARIO
FOR EACH ROW
WHEN (NEW.STATUS = 'APROVADA' AND OLD.STATUS = 'PENDENTE')
BEGIN
   INSERT INTO HISTORICO_PONTOS (ID_USUARIO, QUANTIDADE, TIPO_MOVIMENTACAO, DESCRICAO, ID_ACAO)
   VALUES (:NEW.ID_USUARIO, :NEW.PONTOS_PREVISTOS, 'GANHO_ACAO',
           'Pontos por acao sustentavel aprovada', :NEW.ID);
END;
/

-- ---------------------------------------------------------------------------
-- TRG_FINALIZA_ACAO  (compound trigger)
-- Problema que resolve: implementa a validacao distribuida em si. Cada voto
-- inserido em VALIDACAO pode ser o voto que fecha o quorum de uma acao.
-- Um trigger comum (FOR EACH ROW) nao pode consultar a propria tabela
-- VALIDACAO enquanto ela ainda esta sendo modificada (erro ORA-04091,
-- "mutating table"). Por isso usamos um COMPOUND TRIGGER: a secao
-- AFTER EACH ROW apenas guarda os IDs de acao afetados, e a secao
-- AFTER STATEMENT roda depois que o INSERT terminou, quando a tabela ja
-- pode ser lida com seguranca.
-- Quando executa: apos o INSERT (statement) em VALIDACAO.
-- O que altera:
--   1) ACAO_USUARIO.STATUS -> APROVADA ou REJEITADA, quando o numero de
--      votos atinge o minimo configurado em PARAMETRO_SISTEMA e o percentual
--      de aprovacao ponderado (por peso_voto) cruza o limiar configurado;
--   2) HISTORICO_REPUTACAO -> registra, para cada validador daquela acao,
--      se o voto convergiu ou divergiu do resultado final, premiando ou
--      penalizando a reputacao (isso e o que da corpo ao "incentivo a
--      honestidade": quem valida bem, ganha reputacao).
-- ---------------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_FINALIZA_ACAO
FOR INSERT ON VALIDACAO
COMPOUND TRIGGER

   TYPE t_ids IS TABLE OF ACAO_USUARIO.ID%TYPE INDEX BY PLS_INTEGER;
   v_ids   t_ids;
   v_count PLS_INTEGER := 0;

   AFTER EACH ROW IS
   BEGIN
      v_count := v_count + 1;
      v_ids(v_count) := :NEW.ID_ACAO;
   END AFTER EACH ROW;

   AFTER STATEMENT IS
      v_min_validacoes   NUMBER;
      v_percentual_min   NUMBER;
      v_qtd_votos        NUMBER;
      v_soma_peso        NUMBER;
      v_soma_peso_aprova NUMBER;
      v_percentual       NUMBER;
      v_status_atual     ACAO_USUARIO.STATUS%TYPE;
      v_status_final     ACAO_USUARIO.STATUS%TYPE;
      v_rep_atual        NUMBER;
      v_rep_nova         NUMBER;
      v_acertou          BOOLEAN;
   BEGIN
      SELECT VALOR INTO v_min_validacoes FROM PARAMETRO_SISTEMA WHERE NOME_PARAMETRO = 'MIN_VALIDACOES_POR_ACAO';
      SELECT VALOR INTO v_percentual_min FROM PARAMETRO_SISTEMA WHERE NOME_PARAMETRO = 'PERCENTUAL_APROVACAO_MINIMO';

      FOR i IN 1 .. v_ids.COUNT LOOP
         SELECT STATUS INTO v_status_atual FROM ACAO_USUARIO WHERE ID = v_ids(i);

         IF v_status_atual = 'PENDENTE' THEN
            SELECT COUNT(*),
                   NVL(SUM(PESO_VOTO), 0),
                   NVL(SUM(CASE WHEN VOTO = 'APROVAR' THEN PESO_VOTO ELSE 0 END), 0)
              INTO v_qtd_votos, v_soma_peso, v_soma_peso_aprova
              FROM VALIDACAO
             WHERE ID_ACAO = v_ids(i);

            IF v_qtd_votos >= v_min_validacoes THEN
               v_percentual := (v_soma_peso_aprova / v_soma_peso) * 100;

               IF v_percentual >= v_percentual_min THEN
                  v_status_final := 'APROVADA';
               ELSE
                  v_status_final := 'REJEITADA';
               END IF;

               UPDATE ACAO_USUARIO
                  SET STATUS = v_status_final, DATA_CONCLUSAO = SYSDATE
                WHERE ID = v_ids(i);

               FOR rec IN (SELECT ID_VALIDADOR, VOTO FROM VALIDACAO WHERE ID_ACAO = v_ids(i)) LOOP

                  IF rec.VOTO = 'APROVAR' AND v_status_final = 'APROVADA' THEN
                     v_acertou := TRUE;
                  ELSIF rec.VOTO = 'REJEITAR' AND v_status_final = 'REJEITADA' THEN
                     v_acertou := TRUE;
                  ELSE
                     v_acertou := FALSE;
                  END IF;

                  SELECT REPUTACAO_ATUAL INTO v_rep_atual FROM USUARIO WHERE ID = rec.ID_VALIDADOR;

                  IF v_acertou THEN
                     v_rep_nova := LEAST(100, v_rep_atual + 1);
                  ELSE
                     v_rep_nova := GREATEST(0, v_rep_atual - 2);
                  END IF;

                  IF v_acertou THEN
                     INSERT INTO HISTORICO_REPUTACAO (ID_USUARIO, REPUTACAO_ANTERIOR, REPUTACAO_NOVA, MOTIVO)
                     VALUES (rec.ID_VALIDADOR, v_rep_atual, v_rep_nova, 'Voto convergente com o resultado final da acao');
                  ELSE
                     INSERT INTO HISTORICO_REPUTACAO (ID_USUARIO, REPUTACAO_ANTERIOR, REPUTACAO_NOVA, MOTIVO)
                     VALUES (rec.ID_VALIDADOR, v_rep_atual, v_rep_nova, 'Voto divergente do resultado final da acao');
                  END IF;

               END LOOP;
            END IF;
         END IF;
      END LOOP;
   END AFTER STATEMENT;

END TRG_FINALIZA_ACAO;
/


-- ============================================================================
-- 5. VIEWS
-- ============================================================================

-- Ranking: view em vez de tabela fisica, pois pontos_atuais ja e a fonte
-- de verdade (mantida por trigger); duplicar em tabela criaria uma segunda
-- fonte para o mesmo dado.
CREATE OR REPLACE VIEW VW_RANKING_USUARIOS AS
SELECT RANK() OVER (ORDER BY U.PONTOS_ATUAIS DESC) AS POSICAO,
       U.ID                AS ID_USUARIO,
       U.NOME              AS USUARIO,
       U.PONTOS_ATUAIS     AS PONTOS,
       N.NOME              AS NIVEL_CONFIANCA,
       U.STREAK_ATUAL      AS STREAK
  FROM USUARIO U
  JOIN NIVEL_CONFIANCA N ON N.ID = U.ID_NIVEL_CONFIANCA
 WHERE U.STATUS_CONTA = 'ATIVO';

CREATE OR REPLACE VIEW VW_ACOES_PENDENTES AS
SELECT A.ID                AS ID_ACAO,
       U.NOME              AS USUARIO,
       T.NOME              AS TIPO_ACAO,
       A.QUANTIDADE,
       T.UNIDADE_MEDIDA,
       A.DATA_HORA,
       A.PONTOS_PREVISTOS,
       (SELECT COUNT(*) FROM VALIDACAO V WHERE V.ID_ACAO = A.ID) AS QTD_VALIDACOES
  FROM ACAO_USUARIO A
  JOIN USUARIO U   ON U.ID = A.ID_USUARIO
  JOIN TIPO_ACAO T ON T.ID = A.ID_TIPO_ACAO
 WHERE A.STATUS = 'PENDENTE';

CREATE OR REPLACE VIEW VW_REPUTACAO_USUARIOS AS
SELECT U.ID                AS ID_USUARIO,
       U.NOME              AS USUARIO,
       U.REPUTACAO_ATUAL,
       N.NOME              AS NIVEL_CONFIANCA,
       N.PESO_VOTO,
       (SELECT COUNT(*) FROM VALIDACAO V WHERE V.ID_VALIDADOR = U.ID) AS TOTAL_VALIDACOES
  FROM USUARIO U
  JOIN NIVEL_CONFIANCA N ON N.ID = U.ID_NIVEL_CONFIANCA
 ORDER BY U.REPUTACAO_ATUAL DESC;
