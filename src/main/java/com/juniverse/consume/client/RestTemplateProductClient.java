package com.juniverse.consume.client;

import com.juniverse.consume.exception.ProductNotFoundException;
import com.juniverse.consume.exception.RemoteServiceException;
import com.juniverse.consume.model.NewProduct;
import com.juniverse.consume.model.Product;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

/**
 * Cliente con {@link RestTemplate}: la API clásica de Spring (bloqueante, con métodos como {@code getForObject},
 * {@code postForObject}, {@code exchange} y {@code delete}). Está en modo mantenimiento: para código nuevo se recomienda
 * {@code RestClient}.
 *
 * <p>Los errores 4xx/5xx llegan como excepciones ({@code HttpClientErrorException}, {@code HttpServerErrorException}) y los
 * fallos de red o timeouts como {@code ResourceAccessException}.</p>
 */
@Component
public class RestTemplateProductClient implements ProductClient {

    private final RestTemplate rest;

    /**
     * Crea el cliente con la URL base y los tiempos de espera configurados.
     *
     * @param baseUrl URL base del servicio de productos
     * @param connectTimeoutMs tiempo máximo para conectar
     * @param readTimeoutMs tiempo máximo para recibir la respuesta
     */
    public RestTemplateProductClient(@Value("${app.products.base-url}") String baseUrl,
            @Value("${app.products.connect-timeout-ms:1000}") long connectTimeoutMs,
            @Value("${app.products.read-timeout-ms:500}") long readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.rest = new RestTemplate(factory);
        this.rest.setUriTemplateHandler(new DefaultUriBuilderFactory(baseUrl)); // permite usar rutas relativas
        this.rest.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add("X-Client", name()); // cabecera común a todas las peticiones
            return execution.execute(request, body);
        });
    }

    @Override
    public String name() {
        return "resttemplate";
    }

    @Override
    public List<Product> findAll() {
        return call(() -> rest.exchange("/products", HttpMethod.GET, null, new ParameterizedTypeReference<List<Product>>() { }).getBody());
    }

    @Override
    public Product findById(long id) {
        try {
            return call(() -> rest.getForObject("/products/{id}", Product.class, id));
        } catch (RemoteServiceException e) {
            if (e.getStatus() == 404) {
                throw new ProductNotFoundException(id);
            }
            throw e;
        }
    }

    @Override
    public Product create(NewProduct product) {
        return call(() -> rest.postForObject("/products", product, Product.class));
    }

    @Override
    public void delete(long id) {
        try {
            call(() -> {
                rest.delete("/products/{id}", id);
                return null;
            });
        } catch (RemoteServiceException e) {
            if (e.getStatus() == 404) {
                throw new ProductNotFoundException(id);
            }
            throw e;
        }
    }

    @Override
    public String slow(long delayMs) {
        return call(() -> rest.getForObject("/slow?delayMs={d}", String.class, delayMs));
    }

    /** Traduce las excepciones de RestTemplate a {@link RemoteServiceException}. */
    private <T> T call(java.util.function.Supplier<T> action) {
        try {
            return action.get();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new RemoteServiceException("RestTemplate: HTTP " + e.getStatusCode().value(), e.getStatusCode().value(), e);
        } catch (ResourceAccessException e) {
            throw new RemoteServiceException("RestTemplate: sin respuesta (" + e.getMessage() + ")", 0, e);
        }
    }
}
