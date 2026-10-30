package com.juniverse.consume.config;

import com.juniverse.consume.exception.ProductNotFoundException;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

/**
 * Configuración propia del cliente Feign de productos (sin {@code @Configuration}: solo se aplica a ese cliente).
 */
public class FeignConfig {

    /**
     * Añade la cabecera {@code X-Client} a todas las peticiones.
     *
     * @return interceptor de peticiones
     */
    @Bean
    public RequestInterceptor clientHeader() {
        return template -> template.header("X-Client", "feign");
    }

    /**
     * Convierte las respuestas 404 en {@link ProductNotFoundException}; el resto usa el decodificador por defecto.
     *
     * @return decodificador de errores
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        ErrorDecoder fallback = new ErrorDecoder.Default();
        return (methodKey, response) -> {
            if (response.status() == 404) {
                String path = response.request().url();
                return new ProductNotFoundException(Long.parseLong(path.substring(path.lastIndexOf('/') + 1)));
            }
            return fallback.decode(methodKey, response);
        };
    }
}
