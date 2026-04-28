package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.Category;
import com.quyen.shoplite.domain.Store;
import com.quyen.shoplite.domain.request.ReqCategoryUpsertDTO;
import com.quyen.shoplite.domain.response.ResCategoryDTO;
import com.quyen.shoplite.repository.CategoryRepository;
import com.quyen.shoplite.util.DTOMapper;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CurrentStoreService currentStoreService;

    public CategoryService(CategoryRepository categoryRepository, CurrentStoreService currentStoreService) {
        this.categoryRepository = categoryRepository;
        this.currentStoreService = currentStoreService;
    }

    public ResCategoryDTO create(ReqCategoryUpsertDTO req) {
        Store store = currentStoreService.getCurrentStore();
        String normalizedName = req.getName().trim();
        if (categoryRepository.existsByStoreIdAndName(store.getId(), normalizedName)) {
            throw new BadRequestException("Category name already exists: " + normalizedName);
        }
        Category category = Category.builder()
                .store(store)
                .name(normalizedName)
                .build();
        return DTOMapper.toResCategoryDTO(categoryRepository.save(category));
    }

    public ResCategoryDTO findById(Integer id) {
        Long storeId = currentStoreService.getCurrentStoreId();
        Category category = categoryRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id=" + id));
        return DTOMapper.toResCategoryDTO(category);
    }

    public List<ResCategoryDTO> findAll() {
        Long storeId = currentStoreService.getCurrentStoreId();
        return categoryRepository.findAllByStoreIdOrderByNameAsc(storeId).stream()
                .map(DTOMapper::toResCategoryDTO)
                .toList();
    }

    public ResCategoryDTO update(Integer id, ReqCategoryUpsertDTO req) {
        Long storeId = currentStoreService.getCurrentStoreId();
        Category category = categoryRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id=" + id));
        String normalizedName = req.getName().trim();
        if (categoryRepository.existsByStoreIdAndNameAndIdNot(storeId, normalizedName, id)) {
            throw new BadRequestException("Category name already exists: " + normalizedName);
        }
        category.setName(normalizedName);
        return DTOMapper.toResCategoryDTO(categoryRepository.save(category));
    }

    public void delete(Integer id) {
        Long storeId = currentStoreService.getCurrentStoreId();
        Category category = categoryRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id=" + id));
        categoryRepository.delete(category);
    }
}
