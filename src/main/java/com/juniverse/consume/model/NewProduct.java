package com.juniverse.consume.model;

import java.math.BigDecimal;

/**
 * Datos para crear un producto.
 *
 * @param name nombre (obligatorio)
 * @param price precio (no negativo)
 */
public record NewProduct(String name, BigDecimal price) {
}
