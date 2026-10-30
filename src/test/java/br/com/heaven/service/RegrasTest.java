package br.com.heaven.service;

import br.com.heaven.model.Usuario;
import br.com.heaven.persistence.Tabela;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Os 38 casos de regras puras do antigo TesteRegras (sem banco), um método por caso. */
class RegrasTest {

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    private static void rejeita(Runnable acao, String caso) {
        assertThrows(RegraNegocioException.class, acao::run, "Não rejeitou: " + caso);
    }

    private static final LocalDate HOJE = LocalDate.of(2026, 9, 13);

    @Test void impactoPorQuantidade() { assertEquals(d("1.00"), Regras.calcularImpacto(d("10"), d("0.10")), "impacto por quantidade"); }
    @Test void arredondamentoDoImpacto() { assertEquals(d("0.01"), Regras.calcularImpacto(d("0.05"), d("0.1")), "arredondamento do impacto"); }
    @Test void quantidadeZero() { rejeita(() -> Regras.calcularImpacto(d("0"), d("1")), "quantidade zero"); }
    @Test void quantidadeNegativa() { rejeita(() -> Regras.calcularImpacto(d("-1"), d("1")), "quantidade negativa"); }
    @Test void precisaoDaQuantidade() { rejeita(() -> Regras.calcularImpacto(d("1.001"), d("1")), "precisão da quantidade"); }
    @Test void overflowQuantidade() { rejeita(() -> Regras.calcularImpacto(d("100000000"), d("1")), "overflow quantidade"); }
    @Test void overflowImpacto() { rejeita(() -> Regras.calcularImpacto(d("99999999"), d("99999999")), "overflow impacto"); }

    @Test void pontosPorImpacto() { assertEquals(10L, Regras.calcularPontos(d("1"), 10), "pontos por impacto"); }
    @Test void minimoCompativelComTrigger() { assertEquals(1L, Regras.calcularPontos(d("0.01"), 1), "mínimo compatível com trigger"); }
    @Test void arredondamentoDePontos() { assertEquals(3L, Regras.calcularPontos(d("0.25"), 10), "arredondamento de pontos"); }
    @Test void overflowPontos() { rejeita(() -> Regras.calcularPontos(d("100000000"), 10), "overflow pontos"); }
    @Test void impactoArredondadoAZero() { rejeita(() -> Regras.calcularPontos(d("0"), 10), "impacto arredondado a zero"); }

    @Test void quorumIncompleto() { assertEquals("PENDENTE", Regras.decidirResultado(2, 3, d("2"), d("2"), d("60")), "quórum incompleto"); }
    @Test void limiarExato60() { assertEquals("APROVADA", Regras.decidirResultado(3, 3, d("3"), d("5"), d("60")), "limiar exato 60%"); }
    @Test void abaixoDoLimiar() { assertEquals("REJEITADA", Regras.decidirResultado(3, 3, d("2.99"), d("5"), d("60")), "abaixo do limiar"); }
    @Test void maioriaPorPeso() { assertEquals("APROVADA", Regras.decidirResultado(3, 3, d("3"), d("4"), d("60")), "maioria por peso"); }
    @Test void quorumInvalido() { rejeita(() -> Regras.decidirResultado(3, 0, d("1"), d("3"), d("60")), "quórum inválido"); }
    @Test void pesoTotalZero() { rejeita(() -> Regras.decidirResultado(3, 3, d("0"), d("0"), d("60")), "peso total zero"); }

    @Test void primeiraAprovacao() { assertEquals(1L, Regras.calcularStreak(null, HOJE, 0), "primeira aprovação"); }
    @Test void mesmoDiaMantemStreak() { assertEquals(4L, Regras.calcularStreak(HOJE, HOJE, 4), "mesmo dia mantém streak"); }
    @Test void diaConsecutivo() { assertEquals(5L, Regras.calcularStreak(HOJE.minusDays(1), HOJE, 4), "dia consecutivo"); }
    @Test void intervaloReiniciaStreak() { assertEquals(1L, Regras.calcularStreak(HOJE.minusDays(2), HOJE, 4), "intervalo reinicia streak"); }
    @Test void dataFutura() { rejeita(() -> Regras.calcularStreak(HOJE.plusDays(1), HOJE, 1), "data futura"); }

    @Test void convergenciaMaisUm() { assertEquals(d("51"), Regras.ajustarReputacao(d("50"), true), "convergência +1"); }
    @Test void divergenciaMenosDois() { assertEquals(d("48"), Regras.ajustarReputacao(d("50"), false), "divergência -2"); }
    @Test void tetoReputacao() { assertEquals(d("100"), Regras.ajustarReputacao(d("100"), true), "teto reputação"); }
    @Test void pisoReputacao() { assertEquals(d("0"), Regras.ajustarReputacao(d("1"), false), "piso reputação"); }

    @Test void limitePermiteUltimaAcao() { Regras.verificarLimite(4, 5); }
    @Test void limiteAtingido() { rejeita(() -> Regras.verificarLimite(5, 5), "limite atingido"); }

    @Test void evidenciaVazia() { rejeita(() -> Regras.texto(" ", "Evidência", 500), "evidência vazia"); }
    @Test void evidenciaLonga() { rejeita(() -> Regras.texto("x".repeat(501), "Evidência", 500), "evidência longa"); }

    @Test void senhaCorreta() {
        char[] senha = "HeavenTeste2026!".toCharArray();
        String hash = Senhas.criar(senha);
        assertEquals(true, Senhas.conferir(senha, hash), "senha correta");
        Arrays.fill(senha, '\0');
    }

    @Test void senhaIncorreta() {
        char[] senha = "HeavenTeste2026!".toCharArray();
        String hash = Senhas.criar(senha);
        assertEquals(false, Senhas.conferir("outraSenha".toCharArray(), hash), "senha incorreta");
    }

    @Test void saltAleatorio() {
        char[] senha = "HeavenTeste2026!".toCharArray();
        String hash = Senhas.criar(senha);
        assertEquals(false, hash.equals(Senhas.criar(senha)), "salt aleatório");
    }

    @Test void hashInvalido() { assertEquals(false, Senhas.conferir("HeavenTeste2026!".toCharArray(), "valor-inválido"), "hash inválido"); }
    @Test void senhaCurta() { rejeita(() -> Senhas.criar("curta".toCharArray()), "senha curta"); }

    @Test void encapsulamentoModelo() {
        Usuario u = new Usuario();
        u.setNome("Teste");
        assertEquals("Teste", u.getNome(), "encapsulamento modelo");
    }

    @Test void mapeamentoTabela() { assertEquals("USUARIO", Usuario.class.getAnnotation(Tabela.class).value(), "mapeamento tabela"); }
}
