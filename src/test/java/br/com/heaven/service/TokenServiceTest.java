package br.com.heaven.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres";
    private static final Instant AGORA = Instant.parse("2026-10-30T12:00:00Z");

    private static TokenService com(String segredo, Instant instante) {
        return new TokenService(segredo, Clock.fixed(instante, ZoneOffset.UTC));
    }

    private static void assertNaoAutenticado(String token) {
        RegraNegocioException e = assertThrows(RegraNegocioException.class, () -> com(SEGREDO, AGORA).validar(token));
        assertEquals(CodigoErro.NAO_AUTENTICADO, e.getCodigo());
    }

    @Test
    void tokenValidoDevolveOIdDoUsuario() {
        TokenService s = com(SEGREDO, AGORA);
        TokenService.TokenEmitido t = s.emitir(42);
        assertEquals(42, s.validar(t.token()));
        assertEquals(AGORA.plus(Duration.ofHours(12)), t.expiraEm().toInstant());
    }

    @Test
    void tokenAindaValeUmSegundoAntesDeExpirar() {
        String token = com(SEGREDO, AGORA).emitir(7).token();
        assertEquals(7, com(SEGREDO, AGORA.plus(Duration.ofHours(12)).minusSeconds(1)).validar(token));
    }

    @Test
    void tokenExpiradoVira401() {
        String token = com(SEGREDO, AGORA).emitir(7).token();
        RegraNegocioException e = assertThrows(RegraNegocioException.class,
                () -> com(SEGREDO, AGORA.plus(Duration.ofHours(12))).validar(token));
        assertEquals(CodigoErro.NAO_AUTENTICADO, e.getCodigo());
    }

    @Test
    void assinaturaAdulteradaVira401() {
        String token = com(SEGREDO, AGORA).emitir(7).token();
        char ultimo = token.charAt(token.length() - 1);
        assertNaoAutenticado(token.substring(0, token.length() - 1) + (ultimo == 'A' ? 'B' : 'A'));
    }

    @Test
    void cargaAdulteradaVira401() {
        TokenService s = com(SEGREDO, AGORA);
        String assinatura = s.emitir(7).token().split("[.]")[1];
        String outraCarga = s.emitir(8).token().split("[.]")[0];
        assertNaoAutenticado(outraCarga + "." + assinatura);
    }

    @Test
    void tokenDeOutroSegredoVira401() {
        assertNaoAutenticado(com("outro-segredo-tambem-com-32-caracteres!!", AGORA).emitir(7).token());
    }

    @Test
    void tokenMalformadoVira401() {
        for (String ruim : new String[]{null, "", ".", "abc", "a.b.c", "!!!.???", "a."})
            assertNaoAutenticado(ruim);
    }

    @Test
    void segredoCurtoOuAusenteImpedeACriacao() {
        assertThrows(IllegalStateException.class, () -> new TokenService("curto"));
        assertThrows(IllegalStateException.class, () -> new TokenService(""));
        assertThrows(IllegalStateException.class, () -> new TokenService(null));
    }

    @Test
    void trocarOUltimoCaractereDaAssinaturaNuncaValida() {
        TokenService s = com(SEGREDO, AGORA);
        for (long id = 1; id <= 60; id++) {
            String token = s.emitir(id).token();
            char ultimo = token.charAt(token.length() - 1);
            for (char c : "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".toCharArray()) {
                if (c == ultimo) continue;
                String ruim = token.substring(0, token.length() - 1) + c;
                assertThrows(RegraNegocioException.class, () -> s.validar(ruim));
            }
        }
    }
}
