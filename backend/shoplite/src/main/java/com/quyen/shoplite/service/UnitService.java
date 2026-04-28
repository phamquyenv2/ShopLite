package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.Store;
import com.quyen.shoplite.domain.Unit;
import com.quyen.shoplite.domain.request.ReqUnitUpsertDTO;
import com.quyen.shoplite.domain.response.ResUnitDTO;
import com.quyen.shoplite.repository.UnitRepository;
import com.quyen.shoplite.util.DTOMapper;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UnitService {

    private final UnitRepository unitRepository;
    private final CurrentStoreService currentStoreService;

    @Transactional
    public ResUnitDTO create(ReqUnitUpsertDTO req) {
        Store store = currentStoreService.getCurrentStore();
        String normalizedName = req.getName().trim();
        if (unitRepository.existsByStoreIdAndName(store.getId(), normalizedName)) {
            throw new BadRequestException("Unit name already exists: " + normalizedName);
        }

        Unit unit = Unit.builder()
                .store(store)
                .name(normalizedName)
                .description(normalize(req.getDescription()))
                .build();
        return DTOMapper.toResUnitDTO(unitRepository.save(unit));
    }

    public ResUnitDTO findById(Integer id) {
        return DTOMapper.toResUnitDTO(findEntityById(id));
    }

    public List<ResUnitDTO> findAll() {
        Long storeId = currentStoreService.getCurrentStoreId();
        return unitRepository.findAllByStoreIdOrderByNameAsc(storeId).stream()
                .map(DTOMapper::toResUnitDTO)
                .toList();
    }

    @Transactional
    public ResUnitDTO update(Integer id, ReqUnitUpsertDTO req) {
        Unit unit = findEntityById(id);
        String normalizedName = req.getName().trim();
        Long storeId = currentStoreService.getCurrentStoreId();
        if (unitRepository.existsByStoreIdAndNameAndIdNot(storeId, normalizedName, id)) {
            throw new BadRequestException("Unit name already exists: " + normalizedName);
        }

        unit.setName(normalizedName);
        unit.setDescription(normalize(req.getDescription()));
        return DTOMapper.toResUnitDTO(unitRepository.save(unit));
    }

    @Transactional
    public void delete(Integer id) {
        Unit unit = findEntityById(id);
        unitRepository.delete(unit);
    }

    private Unit findEntityById(Integer id) {
        Long storeId = currentStoreService.getCurrentStoreId();
        return unitRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with id=" + id));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
