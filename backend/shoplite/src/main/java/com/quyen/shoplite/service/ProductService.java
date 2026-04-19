package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.Category;
import com.quyen.shoplite.domain.Product;
import com.quyen.shoplite.domain.ProductStatus;
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

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UnitRepository unitRepository;

    @Transactional
    public ResProductDTO create(ReqProductUpsertDTO req) {
        if (req.getSellingPrice() < 0) {
            throw new BadRequestException("Selling price cannot be negative");
        }
        if (req.getCostPrice() < 0) {
            throw new BadRequestException("Cost price cannot be negative");
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

        if (hasText(req.getBarcode()) && productRepository.existsByBarcode(req.getBarcode().trim())) {
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
                .barcode(normalize(req.getBarcode()))
                .stock(req.getStock())
                .sellingPrice(req.getSellingPrice())
                .costPrice(req.getCostPrice())
                .minStock(req.getMinStock())
                .maxStock(req.getMaxStock())
                .image(normalize(req.getImage()))
                .status(determineStatus(req.getStatus(), req.getStock()))
                .isDeleted(false)
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

        if (req.getSellingPrice() < 0) {
            throw new BadRequestException("Selling price cannot be negative");
        }
        if (req.getCostPrice() < 0) {
            throw new BadRequestException("Cost price cannot be negative");
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

        String normalizedBarcode = normalize(req.getBarcode());
        if (hasText(normalizedBarcode) && productRepository.existsByBarcodeAndIdNot(normalizedBarcode, id)) {
            throw new BadRequestException("Barcode already exists: " + req.getBarcode());
        }

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id=" + req.getCategoryId()));
        Unit unit = unitRepository.findById(req.getUnitId())
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with id=" + req.getUnitId()));

        product.setCategory(category);
        product.setUnit(unit);
        product.setName(req.getName().trim());
        product.setBarcode(normalizedBarcode);
        // do not update stock according to controller comment
        product.setSellingPrice(req.getSellingPrice());
        product.setCostPrice(req.getCostPrice());
        product.setMinStock(req.getMinStock());
        product.setMaxStock(req.getMaxStock());
        product.setImage(normalize(req.getImage()));
        product.setStatus(determineStatus(req.getStatus(), product.getStock()));

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

    private ProductStatus determineStatus(ProductStatus requested, Integer stock) {
        ProductStatus result = requested != null ? requested : ProductStatus.ACTIVE;
        int safeStock = stock == null ? 0 : stock;
        if (safeStock <= 0 && result != ProductStatus.INACTIVE) {
            return ProductStatus.OUT_OF_STOCK;
        }
        return result;
    }

    /**
     * POS-optimized search: barcode takes priority (exact match),
     * otherwise fuzzy search by name/SKU. Max 20 results.
     */
    public List<ResProductDTO> searchForPOS(String keyword, String barcode) {
        // Barcode scan — exact match, return single result
        if (hasText(barcode)) {
            return productRepository.findByBarcodeAndIsDeletedFalse(barcode.trim())
                    .map(DTOMapper::toResProductDTO)
                    .map(List::of)
                    .orElse(List.of());
        }

        // Keyword search — name/SKU LIKE
        if (hasText(keyword)) {
            Specification<Product> spec = ProductSpecification.filter(keyword, null, null, null, null);
            Pageable pageable = PageRequest.of(0, 20, Sort.by("name").ascending());
            return productRepository.findAll(spec, pageable)
                    .getContent().stream()
                    .map(DTOMapper::toResProductDTO)
                    .toList();
        }

        return List.of();
    }
}
