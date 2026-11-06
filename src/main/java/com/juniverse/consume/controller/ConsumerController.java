package com.juniverse.consume.controller;

import com.juniverse.consume.client.ProductClient;
import com.juniverse.consume.exception.ProductNotFoundException;
import com.juniverse.consume.exception.RemoteServiceException;
import com.juniverse.consume.dto.NewProduct;
import com.juniverse.consume.model.Product;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API propia que delega en el cliente elegido por la URL: {@code /consume/{resttemplate|restclient|feign}/products}.
 * Así se puede probar cada biblioteca con el mismo navegador o {@code curl}.
 */
@RestController
@RequestMapping("/consume")
public class ConsumerController {

    private final Map<String, ProductClient> clients;

    /**
     * Crea el controlador con los tres clientes disponibles.
     *
     * @param clients todos los beans {@link ProductClient}
     */
    public ConsumerController(List<ProductClient> clients) {
        this.clients = clients.stream().collect(Collectors.toMap(ProductClient::name, Function.identity()));
    }

    private ProductClient client(String name) {
        ProductClient client = clients.get(name);
        if (client == null) {
            throw new IllegalArgumentException("Cliente desconocido: " + name + ". Usa " + clients.keySet());
        }
        return client;
    }

    /**
     * Lista los productos con el cliente indicado.
     *
     * @param client resttemplate, restclient o feign
     * @return productos
     */
    @GetMapping("/{client}/products")
    public List<Product> list(@PathVariable String client) {
        return client(client).findAll();
    }

    /**
     * Obtiene un producto con el cliente indicado.
     *
     * @param client nombre del cliente
     * @param id identificador
     * @return el producto
     */
    @GetMapping("/{client}/products/{id}")
    public Product get(@PathVariable String client, @PathVariable long id) {
        return client(client).findById(id);
    }

    /**
     * Crea un producto con el cliente indicado.
     *
     * @param client nombre del cliente
     * @param product datos
     * @return el producto creado
     */
    @PostMapping("/{client}/products")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    public Product create(@PathVariable String client, @RequestBody NewProduct product) {
        return client(client).create(product);
    }

    /**
     * Elimina un producto con el cliente indicado.
     *
     * @param client nombre del cliente
     * @param id identificador
     * @return 204
     */
    @DeleteMapping("/{client}/products/{id}")
    public ResponseEntity<Void> delete(@PathVariable String client, @PathVariable long id) {
        client(client).delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Llama con los tres clientes a la vez y devuelve cuántos productos vio cada uno.
     *
     * @return mapa nombre del cliente -> cantidad de productos
     */
    @GetMapping("/compare")
    public Map<String, Integer> compare() {
        Map<String, Integer> result = new LinkedHashMap<>();
        clients.values().stream().sorted((a, b) -> a.name().compareTo(b.name()))
                .forEach(c -> result.put(c.name(), c.findAll().size()));
        return result;
    }

    /**
     * 404: el producto no existe en el servicio remoto.
     *
     * @param e excepción
     * @return respuesta de error
     */
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound(ProductNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    /**
     * 502 (Bad Gateway) si el servicio remoto falla o no responde; 4xx se propaga.
     *
     * @param e excepción
     * @return respuesta de error
     */
    @ExceptionHandler(RemoteServiceException.class)
    public ResponseEntity<Map<String, String>> remote(RemoteServiceException e) {
        int status = e.getStatus() >= 400 && e.getStatus() < 500 ? e.getStatus() : HttpStatus.BAD_GATEWAY.value();
        return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
    }

    /**
     * 400: cliente desconocido.
     *
     * @param e excepción
     * @return respuesta de error
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
