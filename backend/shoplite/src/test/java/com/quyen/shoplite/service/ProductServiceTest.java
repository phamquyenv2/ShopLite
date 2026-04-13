package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.Category;
import com.quyen.shoplite.domain.Product;
import com.quyen.shoplite.domain.Unit;
import com.quyen.shoplite.domain.request.ReqProductUpsertDTO;
import com.quyen.shoplite.domain.response.ResProductDTO;
import com.quyen.shoplite.domain.response.ResProductPageDTO;
import com.quyen.shoplite.repository.CategoryRepository;
import com.quyen.shoplite.repository.ProductRepository;
import com.quyen.shoplite.repository.UnitRepository;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UnitRepository unitRepository;

    @InjectMocks
    private ProductService productService;

    // --- Success cases ---

    @Test
    void create_ShouldReturnProduct_WhenValidRequest() {
        // Arrange
        ReqProductUpsertDTO req = new ReqProductUpsertDTO();
        req.setName("Coke");
        req.setSku("SKU-123");
        req.setBarcode(12345L);
        req.setCategoryId(1);
        req.setUnitId(2);
        req.setStock(100);
        req.setPrice(15.0);

        when(productRepository.existsBySku("SKU-123")).thenReturn(false);
        when(productRepository.existsByBarcode(12345L)).thenReturn(false);
        when(categoryRepository.findById(1)).thenReturn(Optional.of(Category.builder().id(1).build()));
        when(unitRepository.findById(2)).thenReturn(Optional.of(Unit.builder().id(2).build()));

        Product savedProduct = Product.builder()
                .id(10)
                .name("Coke")
                .sku("SKU-123")
                .barcode(12345L)
                .stock(100)
                .price(15.0)
                .isDeleted(false)
                .build();

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        // Act
        ResProductDTO result = productService.create(req);

        // Assert
        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals("Coke", result.getName());
        assertEquals("SKU-123", result.getSku());
        assertEquals(12345L, result.getBarcode());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void create_ShouldGenerateSku_WhenSkuNotProvided() {
        ReqProductUpsertDTO req = new ReqProductUpsertDTO();
        req.setName("Water");
        req.setCategoryId(1);
        req.setUnitId(2);
        req.setStock(50);
        req.setPrice(5.0);

        when(categoryRepository.findById(1)).thenReturn(Optional.of(Category.builder().id(1).build()));
        when(unitRepository.findById(2)).thenReturn(Optional.of(Unit.builder().id(2).build()));

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(11);
            return p;
        });

        ResProductDTO result = productService.create(req);

        assertNotNull(result);
        assertEquals(11, result.getId());
        assertNotNull(result.getSku());
        assertEquals(8, result.getSku().length());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void update_ShouldReturnProduct_WhenValidRequest() {
        // Arrange
        Integer id = 10;
        ReqProductUpsertDTO req = new ReqProductUpsertDTO();
        req.setName("Updated Coke");
        req.setSku("SKU-999");
        req.setBarcode(9999L);
        req.setCategoryId(1);
        req.setUnitId(2);
        req.setStock(200);
        req.setPrice(20.0);
        req.setVersion(1);

        Product existingProduct = Product.builder()
                .id(id).version(1).isDeleted(false).stock(50).build();
        
        when(productRepository.findById(id)).thenReturn(Optional.of(existingProduct));
        when(productRepository.existsBySkuAndIdNot("SKU-999", id)).thenReturn(false);
        when(productRepository.existsByBarcodeAndIdNot(9999L, id)).thenReturn(false);
        when(categoryRepository.findById(1)).thenReturn(Optional.of(Category.builder().id(1).build()));
        when(unitRepository.findById(2)).thenReturn(Optional.of(Unit.builder().id(2).build()));

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ResProductDTO result = productService.update(id, req);

        // Assert
        assertNotNull(result);
        assertEquals(id, result.getId());
        assertEquals("Updated Coke", result.getName());
        assertEquals("SKU-999", result.getSku());
        assertEquals(50, result.getStock()); // stock must not be overridden
        assertEquals(20.0, result.getPrice());
        verify(productRepository).save(existingProduct);
    }

    @Test
    void findById_ShouldReturnProduct_WhenProductExists() {
        Integer id = 10;
        Product existingProduct = Product.builder().id(id).isDeleted(false).build();
        when(productRepository.findById(id)).thenReturn(Optional.of(existingProduct));

        ResProductDTO result = productService.findById(id);

        assertNotNull(result);
        assertEquals(id, result.getId());
    }

    @Test
    void getProducts_ShouldReturnPageDTO_WhenValidRequest() {
        Page<Product> page = new PageImpl<>(Collections.singletonList(Product.builder().id(1).build()));
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        ResProductPageDTO result = productService.getProducts("keyword", 1, 0.0, 100.0, 0, 10, "createdAt", "desc", 2);

        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void softDelete_ShouldSetIsDeletedTrue_WhenProductExists() {
        // Arrange
        Integer id = 10;
        Product existingProduct = Product.builder().id(id).isDeleted(false).build();
        when(productRepository.findById(id)).thenReturn(Optional.of(existingProduct));

        // Act
        productService.softDelete(id);

        // Assert
        assertTrue(existingProduct.isDeleted());
        verify(productRepository).save(existingProduct);
    }

    // --- Failure cases ---

    @Test
    void create_ShouldThrowBadRequest_WhenNegativePrice() {
        ReqProductUpsertDTO req = new ReqProductUpsertDTO();
        req.setPrice(-10.0);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> productService.create(req));
        assertEquals("Price cannot be negative", ex.getMessage());
    }

    @Test
    void create_ShouldThrowBadRequest_WhenNegativeStock() {
        ReqProductUpsertDTO req = new ReqProductUpsertDTO();
        req.setPrice(10.0);
        req.setStock(-5);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> productService.create(req));
        assertEquals("Stock cannot be negative", ex.getMessage());
    }

    @Test
    void create_ShouldThrowBadRequest_WhenDuplicateSku() {
        // Arrange
        ReqProductUpsertDTO req = new ReqProductUpsertDTO();
        req.setPrice(10.0);
        req.setStock(10);
        req.setSku("SKU-123");

        when(productRepository.existsBySku("SKU-123")).thenReturn(true);

        // Act & Assert
        BadRequestException ex = assertThrows(BadRequestException.class, () -> productService.create(req));
        assertEquals("SKU already exists: SKU-123", ex.getMessage());
    }

    @Test
    void create_ShouldThrowBadRequest_WhenDuplicateBarcode() {
        // Arrange
        ReqProductUpsertDTO req = new ReqProductUpsertDTO();
        req.setPrice(10.0);
        req.setStock(10);
        req.setSku("SKU-123");
        req.setBarcode(123L);

        when(productRepository.existsBySku("SKU-123")).thenReturn(false);
        when(productRepository.existsByBarcode(123L)).thenReturn(true);

        // Act & Assert
        BadRequestException ex = assertThrows(BadRequestException.class, () -> productService.create(req));
        assertEquals("Barcode already exists: 123", ex.getMessage());
    }

    @Test
    void create_ShouldThrowNotFound_WhenCategoryNotFound() {
        // Arrange
        ReqProductUpsertDTO req = new ReqProductUpsertDTO();
        req.setPrice(10.0);
        req.setStock(10);
        req.setCategoryId(99);

        when(categoryRepository.findById(99)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> productService.create(req));
        assertEquals("Category not found with id=99", ex.getMessage());
    }

    @Test
    void create_ShouldThrowNotFound_WhenUnitNotFound() {
        // Arrange
        ReqProductUpsertDTO req = new ReqProductUpsertDTO();
        req.setPrice(10.0);
        req.setStock(10);
        req.setCategoryId(1);
        req.setUnitId(99);

        when(categoryRepository.findById(1)).thenReturn(Optional.of(Category.builder().id(1).build()));
        when(unitRepository.findById(99)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> productService.create(req));
        assertEquals("Unit not found with id=99", ex.getMessage());
    }

    @Test
    void update_ShouldThrowNotFound_WhenProductNotFound() {
        // Arrange
        ReqProductUpsertDTO req = new ReqProductUpsertDTO();
        when(productRepository.findById(99)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> productService.update(99, req));
        assertEquals("Product not found with id=99", ex.getMessage());
    }

    @Test
    void update_ShouldThrowNotFound_WhenProductIsDeleted() {
        ReqProductUpsertDTO req = new ReqProductUpsertDTO();
        Product p = Product.builder().id(1).isDeleted(true).build();
        when(productRepository.findById(1)).thenReturn(Optional.of(p));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> productService.update(1, req));
        assertEquals("Product not found with id=1", ex.getMessage());
    }

    @Test
    void update_ShouldThrowBadRequest_WhenNegativePrice() {
        // Arrange
        ReqProductUpsertDTO req = new ReqProductUpsertDTO();
        req.setPrice(-10.0);
        Product p = Product.builder().id(1).isDeleted(false).build();
        when(productRepository.findById(1)).thenReturn(Optional.of(p));

        // Act & Assert
        BadRequestException ex = assertThrows(BadRequestException.class, () -> productService.update(1, req));
        assertEquals("Price cannot be negative", ex.getMessage());
    }

    @Test
    void update_ShouldThrowBadRequest_WhenVersionMismatch() {
        ReqProductUpsertDTO req = new ReqProductUpsertDTO();
        req.setPrice(10.0);
        req.setVersion(2);
        Product p = Product.builder().id(1).version(1).isDeleted(false).build();
        when(productRepository.findById(1)).thenReturn(Optional.of(p));

        // Act & Assert
        BadRequestException ex = assertThrows(BadRequestException.class, () -> productService.update(1, req));
        assertEquals("Product has been modified by another user. Please refresh and try again.", ex.getMessage());
    }

    @Test
    void update_ShouldThrowBadRequest_WhenDuplicateSku() {
        // Arrange
        ReqProductUpsertDTO req = new ReqProductUpsertDTO();
        req.setPrice(10.0);
        req.setSku("DUP-SKU");
        Product p = Product.builder().id(1).isDeleted(false).build();
        when(productRepository.findById(1)).thenReturn(Optional.of(p));
        when(productRepository.existsBySkuAndIdNot("DUP-SKU", 1)).thenReturn(true);

        // Act & Assert
        BadRequestException ex = assertThrows(BadRequestException.class, () -> productService.update(1, req));
        assertEquals("SKU already exists: DUP-SKU", ex.getMessage());
    }

    @Test
    void update_ShouldThrowBadRequest_WhenDuplicateBarcode() {
        // Arrange
        ReqProductUpsertDTO req = new ReqProductUpsertDTO();
        req.setPrice(10.0);
        req.setBarcode(111L);
        Product p = Product.builder().id(1).isDeleted(false).build();
        when(productRepository.findById(1)).thenReturn(Optional.of(p));
        when(productRepository.existsByBarcodeAndIdNot(111L, 1)).thenReturn(true);

        // Act & Assert
        BadRequestException ex = assertThrows(BadRequestException.class, () -> productService.update(1, req));
        assertEquals("Barcode already exists: 111", ex.getMessage());
    }
    
    @Test
    void update_ShouldThrowNotFound_WhenCategoryNotFound() {
        // Arrange
        ReqProductUpsertDTO req = new ReqProductUpsertDTO();
        req.setPrice(10.0);
        req.setCategoryId(99);
        Product p = Product.builder().id(1).isDeleted(false).build();
        
        when(productRepository.findById(1)).thenReturn(Optional.of(p));
        when(categoryRepository.findById(99)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> productService.update(1, req));
        assertEquals("Category not found with id=99", ex.getMessage());
    }

    @Test
    void findById_ShouldThrowNotFound_WhenProductNotFound() {
        // Arrange  
        when(productRepository.findById(99)).thenReturn(Optional.empty());
        
        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> productService.findById(99));
        assertEquals("Product not found with id=99", ex.getMessage());
    }

    @Test
    void softDelete_ShouldThrowNotFound_WhenProductNotFound() { 
        // Arrange
        when(productRepository.findById(99)).thenReturn(Optional.empty());
        
        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> productService.softDelete(99));
        assertEquals("Product not found with id=99", ex.getMessage());
    }
}
