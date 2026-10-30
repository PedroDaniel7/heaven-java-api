package br.com.heaven.persistence;
import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD)
public @interface Coluna { String nome(); boolean chave() default false; boolean gerada() default false; }
