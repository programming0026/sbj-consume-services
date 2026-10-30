package com.juniverse.consume;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Consumo de servicios REST con tres clientes HTTP de Spring: {@code RestTemplate}, {@code RestClient} y {@code OpenFeign}.
 *
 * <p>Para que el ejemplo funcione sin internet, la misma aplicación publica un servicio "proveedor" en {@code /provider}
 * y los tres clientes lo consumen por HTTP real. Cambiando {@code app.products.base-url} se pueden apuntar a otra API.</p>
 */
@SpringBootApplication
@EnableFeignClients
public class ConsumeServicesApplication {

    /**
     * Arranca la aplicación.
     *
     * @param args argumentos de la línea de comandos
     */
    public static void main(String[] args) {
        SpringApplication.run(ConsumeServicesApplication.class, args);
    }
}
