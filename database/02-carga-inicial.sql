-- Execute com F5 no SQL Developer, no MESMO schema do Heaven já existente.
-- Não apaga dados. Exemplos didáticos: fatores de impacto NÃO são coeficientes científicos.
MERGE INTO PARAMETRO_SISTEMA p USING (SELECT 'MIN_VALIDACOES_POR_ACAO' nome, 3 valor FROM dual UNION ALL SELECT 'PERCENTUAL_APROVACAO_MINIMO', 60 FROM dual) s
ON (p.NOME_PARAMETRO=s.nome) WHEN NOT MATCHED THEN INSERT (NOME_PARAMETRO,VALOR,DESCRICAO) VALUES (s.nome,s.valor,'Configuração inicial da votação');
MERGE INTO NIVEL_CONFIANCA n USING (SELECT 'Iniciante' nome,0 rep,1 peso,10 validacoes,5 acoes FROM dual UNION ALL SELECT 'Colaborador',60,1.5,15,8 FROM dual UNION ALL SELECT 'Guardiao',80,2,20,10 FROM dual) s
ON (n.NOME=s.nome) WHEN NOT MATCHED THEN INSERT (NOME,REPUTACAO_MINIMA,PESO_VOTO,LIMITE_VALIDACOES_DIA,LIMITE_ACOES_DIA) VALUES(s.nome,s.rep,s.peso,s.validacoes,s.acoes);
MERGE INTO TIPO_ACAO t USING (SELECT 'Transporte publico' nome,'TRANSPORTE' categoria,'km' unidade,0.1 fator,10 pontos FROM dual UNION ALL SELECT 'Economia de energia','ENERGIA','kWh',0.2,10 FROM dual UNION ALL SELECT 'Reciclagem','RECICLAGEM','kg',0.5,10 FROM dual) s
ON(t.NOME=s.nome) WHEN NOT MATCHED THEN INSERT(NOME,DESCRICAO,CATEGORIA,UNIDADE_MEDIDA,IMPACTO_AMBIENTAL_ESTIMADO,PONTOS_BASE,DIFICULDADE) VALUES(s.nome,'Fator de impacto didatico; requer calibracao para uso real.',s.categoria,s.unidade,s.fator,s.pontos,'FACIL');
COMMIT;
SELECT OBJECT_NAME,STATUS FROM USER_OBJECTS WHERE OBJECT_TYPE='TRIGGER';
SELECT NAME,LINE,POSITION,TEXT FROM USER_ERRORS ORDER BY NAME,SEQUENCE;
SELECT NOME_PARAMETRO,VALOR FROM PARAMETRO_SISTEMA;
