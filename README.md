# Consumir servicios REST con RestTemplate, RestClient y OpenFeign

Ejemplo con **Spring Boot 4** y **Java 21** que consume la misma API con los tres clientes HTTP de Spring, para que compares cómo se escribe y cuándo conviene cada uno.

Para que funcione sin internet, la propia aplicación publica un servicio "proveedor" en `/provider` (productos de ejemplo) y los tres clientes lo consumen por **HTTP real**. Cambiando `app.products.base-url` puedes apuntarlos a otra API.

## Los tres clientes

| | `RestTemplate` | `RestClient` | `OpenFeign` |
|---|---|---|---|
| Estilo | Métodos (`getForObject`, `exchange`...) | API **fluida** encadenada | **Interfaz declarativa** con anotaciones |
| Disponible desde | Spring 3 | Spring 6.1 | Spring Cloud OpenFeign |
| Bloqueante | Sí | Sí | Sí |
| Manejo de errores | Excepciones (`HttpClientErrorException`...) | `onStatus(...)` / `defaultStatusHandler` | `ErrorDecoder` |
| Código necesario | Medio | Medio-bajo | **Mínimo** (solo la interfaz) |
| Estado | **Modo mantenimiento** | **Recomendado** para código nuevo | Muy usado con Spring Cloud |
| Clase del ejemplo | `RestTemplateProductClient` | `RestClientProductClient` | `ProductFeignApi` + `FeignProductClient` |

Cómo se ve la misma llamada ("obtener el producto 2"):

```java
// RestTemplate
Product p = rest.getForObject("/products/{id}", Product.class, 2);

// RestClient
Product p = rest.get().uri("/products/{id}", 2).retrieve().body(Product.class);

// OpenFeign (la interfaz ya declara el método)
Product p = api.findById(2);
```

### Cuál elegir

- **Código nuevo**: `RestClient`.
- **Muchas llamadas a APIs REST con Spring Cloud** (balanceo, circuit breaker, descubrimiento de servicios): `OpenFeign`.
- **Proyectos existentes** que ya usan `RestTemplate`: pueden seguir, y migrar poco a poco a `RestClient` (tiene una API muy parecida y comparte los conversores).
- Para llamadas **reactivas** o no bloqueantes existe además `WebClient` (WebFlux).

## Qué muestra el ejemplo

- URL base, cabecera común (`X-Client`) y tiempos de espera (conexión 1 s, lectura 0,5 s) en cada cliente.
- Lectura de **listas** (`ParameterizedTypeReference`), **GET/POST/DELETE** y rutas con variables.
- Traducción de errores: un **404** se convierte en `ProductNotFoundException`; otros errores 4xx/5xx y los fallos de red o **timeouts** en `RemoteServiceException`. El resto de la aplicación no depende de la biblioteca HTTP.
- Un contrato común `ProductClient` que implementan los tres, de modo que se pueden intercambiar.
- API propia `/consume/{resttemplate|restclient|feign}/...` para probar cada cliente con `curl`.

## Estructura

```text
src/main/java/com/juniverse/consume/
├── ConsumeServicesApplication.java   # @EnableFeignClients
├── client/
│   ├── ProductClient.java            # contrato común
│   ├── RestTemplateProductClient.java
│   ├── RestClientProductClient.java
│   ├── ProductFeignApi.java          # interfaz @FeignClient
│   └── FeignProductClient.java       # adaptador al contrato común
├── config/FeignConfig.java           # interceptor y ErrorDecoder de Feign
├── controller/
│   ├── ProviderController.java       # el servicio "externo" (/provider/products, /provider/slow)
│   └── ConsumerController.java       # /consume/...
├── dto/                              # NewProduct
├── exception/                        # ProductNotFoundException, RemoteServiceException
└── model/                            # Product
```

## Requisitos

- JDK 21 y Maven 3.9 o superior.

## Ejecutar

```bash
mvn spring-boot:run
```

Prueba cada cliente (todos devuelven lo mismo):

```bash
curl http://localhost:8080/consume/resttemplate/products
curl http://localhost:8080/consume/restclient/products/2
curl http://localhost:8080/consume/feign/products/999          # 404: producto inexistente
curl -X POST http://localhost:8080/consume/feign/products -H "Content-Type: application/json" \
     -d '{"name":"Webcam","price":799.00}'
curl http://localhost:8080/consume/compare                     # los tres a la vez
```

Para provocar un **timeout**: `curl "http://localhost:8080/provider/slow?delayMs=2000"` responde en 2 s, pero si un cliente llama a ese endpoint con un `read-timeout` de 0,5 s fallará con un 502 (ver la prueba).

## Pruebas

```bash
mvn test
```

`ClientsIntegrationTest` arranca la aplicación en un puerto libre y ejecuta **las mismas 7 pruebas con los tres clientes** (21 en total): listar, buscar, 404, crear/buscar/borrar, 400 por datos inválidos, timeout y respuesta rápida.

## Configuración (`application.properties`)

| Propiedad | Descripción |
|-----------|-------------|
| `app.products.base-url` | URL de la API que se consume |
| `app.products.connect-timeout-ms` / `read-timeout-ms` | Tiempos de espera de RestTemplate y RestClient |
| `spring.cloud.openfeign.client.config.products.*` | Tiempos de espera y nivel de log del cliente Feign |

Fat jar ejecutable: `mvn clean package` y `java -jar target/sbj-consume-services-0.0.1-SNAPSHOT.jar`.

## Licencia

Código de ejemplo de uso libre con fines educativos.
