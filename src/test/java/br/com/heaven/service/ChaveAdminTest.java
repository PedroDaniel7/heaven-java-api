package br.com.heaven.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChaveAdminTest {

    private static final String CHAVE = "uma-chave-admin-com-mais-de-24-caracteres";

    @Test
    void chaveCorretaConfere() {
        assertTrue(new ChaveAdmin(CHAVE).confere(CHAVE));
    }

    @Test
    void espacosNasPontasSaoIgnorados() {
        assertTrue(new ChaveAdmin(CHAVE + "\n").confere(" " + CHAVE));
    }

    @Test
    void chaveErradaOuAusenteNaoConfere() {
        ChaveAdmin c = new ChaveAdmin(CHAVE);
        assertFalse(c.confere(CHAVE + "x"));
        assertFalse(c.confere(CHAVE.substring(1)));
        assertFalse(c.confere(""));
        assertFalse(c.confere(null));
    }

    @Test
    void semVariavelOuComMenosDe24CaracteresNadaConfere() {
        String vinte3 = "x".repeat(ChaveAdmin.TAMANHO_MINIMO - 1);
        assertFalse(new ChaveAdmin(vinte3).confere(vinte3));
        assertFalse(new ChaveAdmin("").confere(""));
        assertFalse(new ChaveAdmin(null).confere(null));
        assertTrue(new ChaveAdmin("x".repeat(ChaveAdmin.TAMANHO_MINIMO)).confere("x".repeat(ChaveAdmin.TAMANHO_MINIMO)));
    }
}
