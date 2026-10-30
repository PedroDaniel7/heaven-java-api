package br.com.heaven.persistence;
import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
public @interface Tabela { String value(); }
