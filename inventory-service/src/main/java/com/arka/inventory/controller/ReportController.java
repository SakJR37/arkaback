package com.arka.inventory.controller;

import com.arka.inventory.model.Product;
import com.arka.inventory.service.StockReportService;
import com.arka.inventory.service.ProductService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
  private final StockReportService reportService;
  private final ProductService productService;

  public ReportController(StockReportService reportService, ProductService productService) {
    this.reportService = reportService;
    this.productService = productService;
  }

  /**
   * Generate and upload weekly report to S3
   */
  @PostMapping("/weekly")
  public ResponseEntity<Map<String, String>> generateWeeklyReport() {
    try {
      reportService.generateWeeklyReport();
      return ResponseEntity.ok(Map.of(
        "status", "success",
        "message", "Weekly report generated and uploaded to S3"
      ));
    } catch (Exception e) {
      return ResponseEntity.status(500).body(Map.of(
        "status", "error",
        "message", e.getMessage()
      ));
    }
  }

  /**
   * Generate CSV for all products (download directly)
   */
  @GetMapping("/all-products/csv")
  public ResponseEntity<String> downloadAllProductsCsv() {
    List<Product> products = productService.listAll();
    String csv = reportService.exportToCsv(products);
    
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=all-products.csv")
        .contentType(MediaType.parseMediaType("text/csv"))
        .body(csv);
  }

  /**
   * Generate CSV for low stock products (download directly)
   */
  @GetMapping("/low-stock/csv")
  public ResponseEntity<String> downloadLowStockCsv(@RequestParam(defaultValue = "10") int threshold) {
    List<Product> lowStock = productService.lowStock(threshold);
    String csv = reportService.exportToCsv(lowStock);
    
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=low-stock.csv")
        .contentType(MediaType.parseMediaType("text/csv"))
        .body(csv);
  }

  /**
   * Preview CSV content in JSON format
   */
  @GetMapping("/preview")
  public ResponseEntity<Map<String, Object>> previewReport() {
    List<Product> allProducts = productService.listAll();
    List<Product> lowStock = productService.lowStock(10);
    
    return ResponseEntity.ok(Map.of(
      "totalProducts", allProducts.size(),
      "lowStockProducts", lowStock.size(),
      "lowStockItems", lowStock
    ));
  }
}