package com.juniverse.consume.client;

import com.juniverse.consume.exception.RemoteServiceException;
import com.juniverse.consume.model.NewProduct;
import com.juniverse.consume.model.Product;
import feign.FeignException;
import feign.RetryableException;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Adaptador que expone el cliente Feign con el contrato común {@link ProductClient}. Feign lanza {@code FeignException}
 * (o {@code RetryableException} en fallos de red y timeouts); aquí se traducen a {@link RemoteServiceException}.
 * El 404 ya llega convertido en {@code ProductNotFoundException} por el {@code ErrorDecoder} de {@code FeignConfig}.
 */
@Component
public class FeignProductClient implements ProductClient {

    private final ProductFeignApi api;

    /**
     * Crea el adaptador.
     *
     * @param api interfaz Feign generada por Spring
     */
    public FeignProductClient(ProductFeignApi api) {
        this.api = api;
    }

    @Override
    public String name() {
        return "feign";
    }

    @Override
    public List<Product> findAll() {
        return call(api::findAll);
    }

    @Override
    public Product findById(long id) {
        return call(() -> api.findById(id));
    }

    @Override
    public Product create(NewProduct product) {
        return call(() -> api.create(product));
    }

    @Override
    public void delete(long id) {
        call(() -> {
            api.delete(id);
            return null;
        });
    }

    @Override
    public String slow(long delayMs) {
        return call(() -> api.slow(delayMs));
    }

    private <T> T call(java.util.function.Supplier<T> action) {
        try {
            return action.get();
        } catch (RetryableException e) {
            throw new RemoteServiceException("Feign: sin respuesta (" + e.getMessage() + ")", 0, e);
        } catch (FeignException e) {
            throw new RemoteServiceException("Feign: HTTP " + e.status(), e.status(), e);
        }
    }
}
