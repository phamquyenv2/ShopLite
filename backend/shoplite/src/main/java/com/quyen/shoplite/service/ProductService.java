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
import com.quyen.shoplite.util.DTOMapper;
import com.quyen.shoplite.util.ProductSpecification;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UnitRepository unitRepository;

    @Transactional
    public ResProductDTO create(ReqProductUpsertDTO req) {
        if (req.getPrice() < 0) {
            throw new BadRequestException("Price cannot be negative");
        }
        if (req.getStock() < 0) {
            throw new BadRequestException("Stock cannot be negative");
        }

        String normalizedSku = normalize(req.getSku());
        if (hasText(normalizedSku)) {
            if (productRepository.existsBySku(normalizedSku)) {
                throw new BadRequestException("SKU already exists: " + normalizedSku);
            }
        } else {
            normalizedSku = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        if (req.getBarcode() != null && productRepository.existsByBarcode(req.getBarcode())) {
            throw new BadRequestException("Barcode already exists: " + req.getBarcode());
        }

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id=" + req.getCategoryId()));
        Unit unit = unitRepository.findById(req.getUnitId())
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with id=" + req.getUnitId()));

        Product product = Product.builder()
                .category(category)
                .unit(unit)
                .name(req.getName().trim())
                .sku(normalizedSku)
                .barcode(req.getBarcode())
                .stock(req.getStock())
                .price(req.getPrice())
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .build();

        return DTOMapper.toResProductDTO(productRepository.save(product));
    }

    public ResProductDTO findById(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id=" + id));
        if (product.isDeleted()) {
            throw new ResourceNotFoundException("Product not found with id=" + id);
        }
        return DTOMapper.toResProductDTO(product);
    }

    public ResProductPageDTO getProducts(String keyword, Integer categoryId,
                                         Double minPrice, Double maxPrice,
                                         int page, int size, String sortBy, String sortDir, Integer unitId) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Product> spec = ProductSpecification.filter(keyword, categoryId, minPrice, maxPrice, unitId);
        Page<Product> productPage = productRepository.findAll(spec, pageable);

        ResProductPageDTO result = new ResProductPageDTO();
        result.setTotalElements(productPage.getTotalElements());
        result.setTotalPages(productPage.getTotalPages());
        result.setPage(page);
        result.setSize(size);
        result.setData(productPage.getContent().stream().map(DTOMapper::toResProductDTO).toList());

        return result;
    }

    @Transactional
    public ResProductDTO update(Integer id, ReqProductUpsertDTO req) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id=" + id));

        if (product.isDeleted()) {
            throw new ResourceNotFoundException("Product not found with id=" + id);
        }

        if (req.getPrice() < 0) {
            throw new BadRequestException("Price cannot be negative");
        }

        if (req.getVersion() != null && !req.getVersion().equals(product.getVersion())) {
            throw new BadRequestException("Product has been modified by another user. Please refresh and try again.");
        }

        String normalizedSku = normalize(req.getSku());
        if (hasText(normalizedSku)) {
            if (productRepository.existsBySkuAndIdNot(normalizedSku, id)) {
                throw new BadRequestException("SKU already exists: " + normalizedSku);
            }
            product.setSku(normalizedSku);
        }

        if (req.getBarcode() != null && productRepository.existsByBarcodeAndIdNot(req.getBarcode(), id)) {
            throw new BadRequestException("Barcode already exists: " + req.getBarcode());
        }

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id=" + req.getCategoryId()));
        Unit unit = unitRepository.findById(req.getUnitId())
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with id=" + req.getUnitId()));

        product.setCategory(category);
        product.setUnit(unit);
        product.setName(req.getName().trim());
        product.setBarcode(req.getBarcode());
        // do not update stock according to controller comment
        product.setPrice(req.getPrice());

        return DTOMapper.toResProductDTO(productRepository.save(product));
    }

    @Transactional
    public void softDelete(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id=" + id));
        if (product.isDeleted()) {
            throw new ResourceNotFoundException("Product not found with id=" + id);
        }
        product.setDeleted(true);
        productRepository.save(product);
    }

    public Product findEntityById(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id=" + id));
        if (product.isDeleted()) {
            throw new ResourceNotFoundException("Product not found with id=" + id);
        }
        return product;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

