package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.Product;
import com.quyen.shoplite.domain.response.ResCategoryDTO;
import com.quyen.shoplite.domain.response.ResProductDTO;
import com.quyen.shoplite.domain.response.ResSalesInitDTO;
import com.quyen.shoplite.repository.CategoryRepository;
import com.quyen.shoplite.repository.ProductRepository;
import com.quyen.shoplite.util.DTOMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesInitService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CurrentStoreService currentStoreService;

    @Transactional(readOnly = true)
    public ResSalesInitDTO getInitData() {
        Long storeId = currentStoreService.getCurrentStoreId();

        List<ResProductDTO> products = productRepository.findAllByStoreIdAndIsDeletedFalse(storeId).stream()
                .map(DTOMapper::toResProductDTO)
                .toList();

        List<ResCategoryDTO> categories = categoryRepository.findAllByStoreIdOrderByNameAsc(storeId).stream()
                .map(DTOMapper::toResCategoryDTO)
                .toList();

        return ResSalesInitDTO.builder()
                .products(products)
                .categories(categories)
                .build();
    }
}
