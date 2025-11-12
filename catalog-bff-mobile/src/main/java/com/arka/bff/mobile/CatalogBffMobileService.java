package com.arka.bff.mobile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
public class CatalogBffMobileService {
  private static final Logger log = LoggerFactory.getLogger(CatalogBffMobileService.class);

  private final RestTemplate restTemplate;
  private final DiscoveryClient discoveryClient;

  public CatalogBffMobileService(RestTemplate restTemplate, DiscoveryClient discoveryClient) {
    this.restTemplate = restTemplate;
    this.discoveryClient = discoveryClient;
  }
  
  public Map<String, Object> aggregateCatalogData() {
    Map<String, Object> result = new HashMap<>();
    
    try {
      String catalogUrl = "http://catalog-service/api/catalog";
      log.info("📡 Llamando a catalog-service: {}", catalogUrl);
      
      // Obtener productos del catalog-service
      List<Map<String, Object>> products = restTemplate.getForObject(catalogUrl, List.class);
      
      log.info("📦 Productos recibidos del catalog-service: {}", 
               products != null ? products.size() : 0);
      
      if (products == null || products.isEmpty()) {
        log.warn("⚠️ No hay productos disponibles en catalog-service");
        result.put("products", Collections.emptyList());
        result.put("count", 0);
        result.put("message", "Mobile BFF - No products available");
        return result;
      }
      
      // Optimizar para mobile: solo campos esenciales
      List<Map<String, Object>> optimizedProducts = new ArrayList<>();
      for (Map<String, Object> product : products) {
        try {
          // Verificar que el producto esté activo
          Boolean active = (Boolean) product.get("active");
          if (Boolean.FALSE.equals(active)) {
            continue; // Skip productos inactivos
          }
          
          Map<String, Object> optimized = new HashMap<>();
          
          // Usar 'id' como identificador principal
          Object productId = product.get("id");
          optimized.put("id", productId);
          optimized.put("name", product.get("name"));
          optimized.put("price", product.get("price"));
          optimized.put("imageUrl", product.get("imageUrl"));
          optimized.put("category", product.get("category"));
          
          // Calcular rating (por ahora mock, luego integrar con review-service)
          optimized.put("rating", 4.5);
          optimized.put("stock", product.get("stock"));
          
          optimizedProducts.add(optimized);
          log.debug(" Producto optimizado: {} - {}", productId, product.get("name"));
          
        } catch (Exception e) {
          log.error(" Error procesando producto: {}", product, e);
        }
      }
      
      result.put("products", optimizedProducts);
      result.put("count", optimizedProducts.size());
      result.put("message", "Mobile BFF - Lightweight catalog data");
      log.info("Retornando {} productos optimizados para mobile", optimizedProducts.size());
      
    } catch (Exception e) {
      log.error(" Error agregando datos del catálogo mobile", e);
      log.error("Detalle: {}", e.getMessage());
      
      result.put("error", "Failed to load catalog");
      result.put("errorDetail", e.getMessage());
      result.put("products", Collections.emptyList());
      result.put("count", 0);
      result.put("message", "Mobile BFF - Error loading catalog");
    }
    
    return result;
  }

  public Map<String, Object> aggregateProductDetails(Long productId) {
    Map<String, Object> result = new HashMap<>();
    
    try {
      log.info("📡 Obteniendo detalles del producto ID: {}", productId);
      
      // Fetch product details
      String productUrl = "http://catalog-service/api/catalog/" + productId;
      Map<String, Object> product = restTemplate.getForObject(productUrl, Map.class);
      
      if (product == null) {
        log.warn("Producto no encontrado: {}", productId);
        result.put("error", "Product not found");
        result.put("message", "Mobile BFF - Product not found");
        return result;
      }
      
      log.info("Producto encontrado: {}", product.get("name"));
      
      // Optimizar producto para mobile
      Map<String, Object> optimizedProduct = new HashMap<>();
      optimizedProduct.put("id", product.get("id"));
      optimizedProduct.put("name", product.get("name"));
      optimizedProduct.put("price", product.get("price"));
      optimizedProduct.put("description", product.get("description"));
      optimizedProduct.put("imageUrl", product.get("imageUrl"));
      optimizedProduct.put("category", product.get("category"));
      optimizedProduct.put("stock", product.get("stock"));
      optimizedProduct.put("active", product.get("active"));
      
      // Intentar obtener reviews (manejo de error si review-service no está disponible)
      List<Map<String, Object>> topReviews = new ArrayList<>();
      int reviewCount = 0;
      
      try {
        String reviewsUrl = "http://review-service/api/reviews/product/" + productId;
        log.info("📡 Intentando obtener reviews de: {}", reviewsUrl);
        
        List<Map<String, Object>> allReviews = restTemplate.getForObject(reviewsUrl, List.class);
        
        if (allReviews != null && !allReviews.isEmpty()) {
          reviewCount = allReviews.size();
          // Tomar solo top 3 reviews para mobile
          topReviews = allReviews.size() > 3 
              ? allReviews.subList(0, 3) 
              : allReviews;
          log.info("Reviews obtenidos: {} total, {} mostrados", reviewCount, topReviews.size());
        } else {
          log.info(" No hay reviews para el producto {}", productId);
        }
        
      } catch (Exception e) {
        log.warn(" No se pudieron obtener reviews (review-service no disponible): {}", 
                 e.getMessage());
        // Continuar sin reviews, no es crítico
      }
      
      result.put("product", optimizedProduct);
      result.put("topReviews", topReviews);
      result.put("reviewCount", reviewCount);
      result.put("message", "Mobile BFF - Optimized product details");
      
      log.info(" Detalles agregados para producto {} (mobile)", productId);
      
    } catch (Exception e) {
      log.error(" Error agregando detalles del producto {} (mobile)", productId, e);
      log.error("Detalle: {}", e.getMessage());
      
      result.put("error", "Failed to load product details");
      result.put("errorDetail", e.getMessage());
      result.put("message", "Mobile BFF - Error loading product");
    }
    
    return result;
  }
}