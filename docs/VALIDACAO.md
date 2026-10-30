# Registro da verificação local

Data: 13/09/2026.

- 38 arquivos Java, compilados para Java 17 com o compilador JDK 25 disponível, `--release 17 -encoding UTF-8 -Xlint:all`.
- 42 arquivos de bytecode gerados, incluindo classes/interfaces internas; compilação final sem erros e sem avisos.
- TesteRegras: 38 testes aprovados (execução real local, exit code 0).
- PDF: 22 páginas, renderizadas e inspecionadas visualmente; capa conferida com os cinco nomes, RMs e turma.
- DDL original: cópia idêntica ao Heaven.sql recebido, conferida por SHA-256.
- TesteOracle: compilado, mas NÃO executado. Conexão real, CRUD e triggers ainda exigem validação no schema de testes do usuário.
- Resolução Maven: NÃO executada neste ambiente. A versão declarada do driver está publicada no Maven Central: https://repo1.maven.org/maven2/com/oracle/database/jdbc/ojdbc11/23.6.0.24.10/

A verificação local de Java usou saída do compilador em memória e gravação dos bytecodes pelo processo de apoio, pois o javac do ambiente apresentou erro ao gravar diretamente arquivos .class. Isso não modifica as fontes nem exige essa adaptação no IntelliJ do usuário. Os bytecodes de teste não fazem parte do ZIP de código-fonte.
