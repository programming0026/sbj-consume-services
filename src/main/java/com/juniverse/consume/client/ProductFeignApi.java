package com.juniverse.consume.client;

import com.juniverse.consume.config.FeignConfig;
import com.juniverse.consume.model.NewProduct;
import com.juniverse.consume.model.Product;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Cliente <b>declarativo</b> con OpenFeign: solo se escribe una interfaz con las anotaciones de Spring MVC y Feign genera la
 * implementación. Es la forma más corta de consumir una API REST; además se integra con Spring Cloud (balanceo, circuit breaker).
 *
 * <p>La URL se toma de {@code app.products.base-url} y los tiempos de espera de
 * {@code spring.cloud.openfeign.client.config.products.*}.</p>
 */
@FeignClient(name = "products", url = "${app.products.base-url}", configuration = FeignConfig.class)
public interface ProductFeignApi {

    /**
     * Lista los productos.
     *
     * @return productos
     */
    @GetMapping("/products")
    List<Product> findAll();

    /**
     * Busca un producto.
     *
     * @param id identificador
     * @return el producto
     */
    @GetMapping("/products/{id}")
    Product findById(@PathVariable("id") long id);

    /**
     * Crea un producto.
     *
     * @param product datos
     * @return el producto creado
     */
    @PostMapping("/products")
    Product create(@RequestBody NewProduct product);

    /**
     * Elimina un producto.
     *
     * @param id identificador
     */
    @DeleteMapping("/products/{id}")
    void delete(@PathVariable("id") long id);

    /**
     * Llama al endpoint lento.
     *
     * @param delayMs retraso solicitado
     * @return respuesta
     */
    @GetMapping("/slow")
    String slow(@RequestParam("delayMs") long delayMs);
}
