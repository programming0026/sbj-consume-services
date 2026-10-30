package com.juniverse.consume.client;

import com.juniverse.consume.exception.ProductNotFoundException;
import com.juniverse.consume.exception.RemoteServiceException;
import com.juniverse.consume.model.NewProduct;
import com.juniverse.consume.model.Product;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Cliente con {@link RestClient} (Spring 6.1+): API <b>fluida</b> y moderna, bloqueante, que reemplaza a RestTemplate en
 * código nuevo. Se construye una vez con la URL base y las cabeceras por defecto, y las llamadas se encadenan:
 * {@code get().uri(...).retrieve().body(...)}.
 *
 * <p>El manejo de errores se declara con {@code onStatus(...)}, sin bloques try/catch dispersos.</p>
 */
@Component
public class RestClientProductClient implements ProductClient {

    private final RestClient rest;

    /**
     * Crea el cliente con la URL base y los tiempos de espera configurados.
     *
     * @param baseUrl URL base del servicio de productos
     * @param connectTimeoutMs tiempo máximo para conectar
     * @param readTimeoutMs tiempo máximo para recibir la respuesta
     */
    public RestClientProductClient(@Value("${app.products.base-url}") String baseUrl,
            @Value("${app.products.connect-timeout-ms:1000}") long connectTimeoutMs,
            @Value("${app.products.read-timeout-ms:500}") long readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.rest = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("X-Client", name())
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                // Cualquier 4xx/5xx se traduce aquí, una sola vez, para todas las llamadas
                .defaultStatusHandler(status -> status.isError(), (request, response) -> {
                    int code = response.getStatusCode().value();
                    throw new RemoteServiceException("RestClient: HTTP " + code, code, null);
                })
                .build();
    }

    @Override
    public String name() {
        return "restclient";
    }

    @Override
    public List<Product> findAll() {
        return call(() -> rest.get().uri("/products").retrieve().body(new ParameterizedTypeReference<List<Product>>() { }));
    }

    @Override
    public Product findById(long id) {
        try {
            return call(() -> rest.get().uri("/products/{id}", id).retrieve().body(Product.class));
        } catch (RemoteServiceException e) {
            if (e.getStatus() == 404) {
                throw new ProductNotFoundException(id);
            }
            throw e;
        }
    }

    @Override
    public Product create(NewProduct product) {
        return call(() -> rest.post().uri("/products").contentType(MediaType.APPLICATION_JSON).body(product).retrieve().body(Product.class));
    }

    @Override
    public void delete(long id) {
        try {
            call(() -> rest.delete().uri("/products/{id}", id).retrieve().toBodilessEntity());
        } catch (RemoteServiceException e) {
            if (e.getStatus() == 404) {
                throw new ProductNotFoundException(id);
            }
            throw e;
        }
    }

    @Override
    public String slow(long delayMs) {
        return call(() -> rest.get().uri(uri -> uri.path("/slow").queryParam("delayMs", delayMs).build()).retrieve().body(String.class));
    }

    /** Traduce los fallos de red de RestClient a {@link RemoteServiceException}. */
    private <T> T call(java.util.function.Supplier<T> action) {
        try {
            return action.get();
        } catch (ResourceAccessException e) {
            throw new RemoteServiceException("RestClient: sin respuesta (" + e.getMessage() + ")", 0, e);
        }
    }
}
