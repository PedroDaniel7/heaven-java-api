package br.com.heaven.resource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Status de erro que a operação pode devolver; o OpenAPI os documenta com o corpo {@link ErroResposta}. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RespostasDeErro {
    int[] value();

    /** Status de sucesso, quando não é 200 (o springdoc assume 200 para ResponseEntity). */
    int sucesso() default 200;
}
