package com.juniverse.consume.client;

import com.juniverse.consume.dto.NewProduct;
import com.juniverse.consume.model.Product;
import java.util.List;

/**
 * Contrato común de los tres clientes: la aplicación puede cambiar de biblioteca HTTP sin tocar al resto del código.
 */
public interface ProductClient {

    /**
     * Nombre del cliente.
     *
     * @return resttemplate, restclient o feign
     */
    String name();

    /**
     * Lista los productos.
     *
     * @return productos
     */
    List<Product> findAll();

    /**
     * Busca un producto.
     *
     * @param id identificador
     * @return el producto
     * @throws com.juniverse.consume.exception.ProductNotFoundException si el servicio responde 404
     */
    Product findById(long id);

    /**
     * Crea un producto.
     *
     * @param product datos
     * @return el producto creado
     */
    Product create(NewProduct product);

    /**
     * Elimina un producto.
     *
     * @param id identificador
     */
    void delete(long id);

    /**
     * Llama al endpoint lento del proveedor.
     *
     * @param delayMs retraso que pide al proveedor
     * @return respuesta del proveedor
     * @throws com.juniverse.consume.exception.RemoteServiceException si se agota el tiempo de espera
     */
    String slow(long delayMs);
}
