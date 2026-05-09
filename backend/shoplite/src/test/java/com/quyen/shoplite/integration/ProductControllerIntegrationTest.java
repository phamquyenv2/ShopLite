package com.quyen.shoplite.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quyen.shoplite.domain.Category;
import com.quyen.shoplite.domain.Product;
import com.quyen.shoplite.domain.Unit;
import com.quyen.shoplite.domain.request.ReqProductUpsertDTO;
import com.quyen.shoplite.repository.CategoryRepository;
import com.quyen.shoplite.repository.ProductRepository;
import com.quyen.shoplite.repository.UnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UnitRepository unitRepository;

    private Integer categoryItId;
    private Integer unitItId;

    @BeforeEach
    void setup() {
        Category cat = categoryRepository.save(Category.builder().name("CatIT").build());
        categoryItId = cat.getId();

        Unit u = unitRepository.save(Unit.builder().name("UnitIT").description("u").build());
        unitItId = u.getId();
    }

    @Test
    @DisplayName("create product success")
    void createProduct_Success() throws Exception {
        ReqProductUpsertDTO req = new ReqProductUpsertDTO();
        req.setName("Prod IT");
        req.setSku("SKUIT");
        req.setBarcode("111222");
        req.setCategoryId(categoryItId);
        req.setUnitId(unitItId);
        req.setStock(50);
        req.setSellingPrice(10.5);
        req.setCostPrice(8.0);

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Prod IT"))
                .andExpect(jsonPath("$.data.categoryId").value(categoryItId));

        assertThat(productRepository.existsBySku("SKUIT")).isTrue();
    }

    @Test
    @DisplayName("create product with missing category failure")
    void createProduct_NotFoundCategoryFailure() throws Exception {
        ReqProductUpsertDTO req = new ReqProductUpsertDTO();
        req.setName("Prod IT");
        req.setSku("SKUIT");
        req.setBarcode("111222");
        req.setCategoryId(9999);
        req.setUnitId(unitItId);
        req.setStock(50);
        req.setSellingPrice(10.5);
        req.setCostPrice(8.0);

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("Category not found with id=9999"));
    }

    @Test
    @DisplayName("create product with missing unit failure")
    void createProduct_NotFoundUnitFailure() throws Exception {
        ReqProductUpsertDTO req = new ReqProductUpsertDTO();
        req.setName("Prod IT");
        req.setSku("SKUIT");
        req.setBarcode("111222");
        req.setCategoryId(categoryItId);
        req.setUnitId(9999);
        req.setStock(50);
        req.setSellingPrice(10.5);
        req.setCostPrice(8.0);

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("Unit not found with id=9999"));
    }

    @Test
    @DisplayName("create product duplicate sku failure")
    void createProduct_DuplicateSkuFailure() throws Exception {
        productRepository.save(Product.builder()
                .category(categoryRepository.findById(categoryItId).get())
                .unit(unitRepository.findById(unitItId).get())
                .name("Exst Prod")
                .sku("SKUIT")
                .barcode("999")
                .stock(10)
                .sellingPrice(10.0)
                .costPrice(0.0)
                .build());

        ReqProductUpsertDTO req = new ReqProductUpsertDTO();
        req.setName("Prod IT");
        req.setSku("SKUIT");
        req.setBarcode("111222");
        req.setCategoryId(categoryItId);
        req.setUnitId(unitItId);
        req.setStock(50);
        req.setSellingPrice(10.5);
        req.setCostPrice(8.0);

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("SKU already exists: SKUIT"));
    }

    @Test
    @DisplayName("create product duplicate barcode failure")
    void createProduct_DuplicateBarcodeFailure() throws Exception {
        productRepository.save(Product.builder()
                .category(categoryRepository.findById(categoryItId).get())
                .unit(unitRepository.findById(unitItId).get())
                .name("Exst Prod")
                .sku("SKU999")
                .barcode("111222")
                .stock(10)
                .sellingPrice(10.0)
                .costPrice(0.0)
                .build());

        ReqProductUpsertDTO req = new ReqProductUpsertDTO();
        req.setName("Prod IT");
        req.setSku("SKUIT");
        req.setBarcode("111222");
        req.setCategoryId(categoryItId);
        req.setUnitId(unitItId);
        req.setStock(50);
        req.setSellingPrice(10.5);
        req.setCostPrice(8.0);

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("Barcode already exists: 111222"));
    }

    @Test
    @DisplayName("create product with negative price failure")
    void createProduct_NegativePriceFailure() throws Exception {
        ReqProductUpsertDTO req = new ReqProductUpsertDTO();
        req.setName("Prod IT");
        req.setSku("SKUIT");
        req.setBarcode("111222");
        req.setCategoryId(categoryItId);
        req.setUnitId(unitItId);
        req.setStock(50);
        req.setSellingPrice(-10.5); // Invalid
        req.setCostPrice(0.0);

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("create product with negative stock failure")
    void createProduct_NegativeStockFailure() throws Exception {
        ReqProductUpsertDTO req = new ReqProductUpsertDTO();
        req.setName("Prod IT");
        req.setSku("SKUIT");
        req.setBarcode("111222");
        req.setCategoryId(categoryItId);
        req.setUnitId(unitItId);
        req.setStock(-5); // Invalid
        req.setSellingPrice(10.5);
        req.setCostPrice(8.0);

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("get product by id success")
    void getProduct_Success() throws Exception {
        Product p = productRepository.save(Product.builder()
                .category(categoryRepository.findById(categoryItId).get())
                .unit(unitRepository.findById(unitItId).get())
                .name("TestProd")
                .sku("SKU12345")
                .stock(10)
                .sellingPrice(10.0)
                .costPrice(0.0)
                .isDeleted(false)
                .build());

        mockMvc.perform(get("/api/v1/products/" + p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(p.getId()))
                .andExpect(jsonPath("$.data.name").value("TestProd"));
    }

    @Test
    @DisplayName("get product by id not found")
    void getProduct_NotFoundFailure() throws Exception {
        mockMvc.perform(get("/api/v1/products/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("Product not found with id=9999"));
    }

    @Test
    @DisplayName("update product success")
    void updateProduct_Success() throws Exception {
        Product p = productRepository.save(Product.builder()
                .category(categoryRepository.findById(categoryItId).get())
                .unit(unitRepository.findById(unitItId).get())
                .name("OldProd")
                .sku("SKU111")
                .stock(10)
                .sellingPrice(10.0)
                .costPrice(0.0)
                .isDeleted(false)
                .build());

        ReqProductUpsertDTO req = new ReqProductUpsertDTO();
        req.setName("UpdatedProd");
        req.setSku("SKU222");
        req.setBarcode(null);
        req.setCategoryId(categoryItId);
        req.setUnitId(unitItId);
        req.setStock(20);
        req.setSellingPrice(25.0);
        req.setCostPrice(12.0);
        req.setVersion(p.getVersion());

        mockMvc.perform(put("/api/v1/products/" + p.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("UpdatedProd"))
                .andExpect(jsonPath("$.data.sku").value("SKU222"));

        Product updated = productRepository.findById(p.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("UpdatedProd");
        assertThat(updated.getSku()).isEqualTo("SKU222");
    }

    @Test
    @DisplayName("delete product success")
    void deleteProduct_Success() throws Exception {
        Product p = productRepository.save(Product.builder()
                .category(categoryRepository.findById(categoryItId).get())
                .unit(unitRepository.findById(unitItId).get())
                .name("ToDelProd")
                .sku("SKUDEL")
                .stock(10)
                .sellingPrice(10.0)
                .costPrice(0.0)
                .isDeleted(false)
                .build());

        mockMvc.perform(delete("/api/v1/products/" + p.getId()))
                .andExpect(status().isNoContent());

        Product deleted = productRepository.findById(p.getId()).orElseThrow();
        assertThat(deleted.isDeleted()).isTrue();
    }
}
