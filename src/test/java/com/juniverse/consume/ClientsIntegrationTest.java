package com.juniverse.consume;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.juniverse.consume.client.ProductClient;
import com.juniverse.consume.exception.ProductNotFoundException;
import com.juniverse.consume.exception.RemoteServiceException;
import com.juniverse.consume.model.NewProduct;
import com.juniverse.consume.model.Product;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Prueba que los tres clientes (RestTemplate, RestClient y Feign) se comportan igual contra el mismo servicio real.
 * La aplicación arranca en un puerto libre y los clientes hacen llamadas HTTP de verdad al proveedor incluido.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@DisplayName("Los tres clientes contra el servicio real")
class ClientsIntegrationTest {

    private static final int PORT = freePort();

    @DynamicPropertySource
    static void ports(DynamicPropertyRegistry registry) {
        registry.add("server.port", () -> PORT);
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Autowired
    private List<ProductClient> clients;

    static List<String> clientNames() {
        return List.of("resttemplate", "restclient", "feign");
    }

    private ProductClient client(String name) {
        return clients.stream().filter(c -> c.name().equals(name)).findFirst().orElseThrow();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("clientNames")
    @DisplayName("GET lista: devuelve los productos de ejemplo")
    void findAll(String name) {
        List<Product> products = client(name).findAll();

        assertTrue(products.size() >= 3);
        assertEquals("Teclado mecánico", products.get(0).name());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("clientNames")
    @DisplayName("GET por id: devuelve el producto")
    void findById(String name) {
        Product product = client(name).findById(2);

        assertEquals("Ratón inalámbrico", product.name());
        assertEquals(0, new BigDecimal("349.50").compareTo(product.price()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("clientNames")
    @DisplayName("GET de un id inexistente: ProductNotFoundException (404)")
    void notFound(String name) {
        assertThrows(ProductNotFoundException.class, () -> client(name).findById(9999));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("clientNames")
    @DisplayName("POST crea, GET lo encuentra y DELETE lo elimina")
    void createFindDelete(String name) {
        ProductClient client = client(name);

        Product created = client.create(new NewProduct("Producto de " + name, new BigDecimal("10.00")));

        assertNotNull(created.id());
        assertEquals("Producto de " + name, client.findById(created.id()).name());
        client.delete(created.id());
        assertThrows(ProductNotFoundException.class, () -> client.findById(created.id()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("clientNames")
    @DisplayName("POST con datos inválidos: RemoteServiceException con el 400 del servidor")
    void badRequest(String name) {
        RemoteServiceException e = assertThrows(RemoteServiceException.class,
                () -> client(name).create(new NewProduct(" ", new BigDecimal("1"))));

        assertEquals(400, e.getStatus());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("clientNames")
    @DisplayName("tiempo de espera agotado: RemoteServiceException sin código HTTP")
    void timeout(String name) {
        RemoteServiceException e = assertThrows(RemoteServiceException.class, () -> client(name).slow(2000));

        assertEquals(0, e.getStatus(), "no hubo respuesta: se agotó el read-timeout de 500 ms");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("clientNames")
    @DisplayName("una respuesta rápida no agota el tiempo de espera")
    void fastResponse(String name) {
        assertEquals("respuesta lenta", client(name).slow(50));
    }
}
