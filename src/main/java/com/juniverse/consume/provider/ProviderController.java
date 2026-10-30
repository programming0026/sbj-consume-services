package com.juniverse.consume.provider;

import com.juniverse.consume.model.NewProduct;
import com.juniverse.consume.model.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Servicio "proveedor" (la API externa que se va a consumir). Tiene datos de ejemplo en memoria y un endpoint lento
 * para probar los tiempos de espera.
 */
@RestController
@RequestMapping("/provider")
public class ProviderController {

    private final Map<Long, Product> products = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(3);

    /** Carga tres productos de ejemplo. */
    public ProviderController() {
        products.put(1L, new Product(1L, "Teclado mecánico", new BigDecimal("899.00")));
        products.put(2L, new Product(2L, "Ratón inalámbrico", new BigDecimal("349.50")));
        products.put(3L, new Product(3L, "Monitor 27\"", new BigDecimal("4599.90")));
    }

    /**
     * Lista los productos ordenados por id.
     *
     * @return productos
     */
    @GetMapping("/products")
    public List<Product> list() {
        return products.values().stream().sorted((a, b) -> Long.compare(a.id(), b.id())).toList();
    }

    /**
     * Obtiene un producto.
     *
     * @param id identificador
     * @return 200 con el producto o 404
     */
    @GetMapping("/products/{id}")
    public ResponseEntity<Product> get(@PathVariable long id) {
        Product product = products.get(id);
        return product == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(product);
    }

    /**
     * Crea un producto.
     *
     * @param request datos
     * @return 201 con el producto o 400 si los datos no son válidos
     */
    @PostMapping("/products")
    public ResponseEntity<Product> create(@RequestBody NewProduct request) {
        if (request.name() == null || request.name().isBlank() || request.price() == null || request.price().signum() < 0) {
            return ResponseEntity.badRequest().build();
        }
        Product product = new Product(sequence.incrementAndGet(), request.name(), request.price());
        products.put(product.id(), product);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    /**
     * Elimina un producto.
     *
     * @param id identificador
     * @return 204 o 404
     */
    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        return products.remove(id) == null ? ResponseEntity.notFound().build() : ResponseEntity.noContent().build();
    }

    /**
     * Endpoint lento: responde tras esperar el tiempo indicado (para provocar un timeout en el cliente).
     *
     * @param delayMs milisegundos de espera
     * @return texto fijo
     * @throws InterruptedException si se interrumpe la espera
     */
    @GetMapping("/slow")
    public String slow(@RequestParam(defaultValue = "1000") long delayMs) throws InterruptedException {
        Thread.sleep(delayMs);
        return "respuesta lenta";
    }
}
