package com.quyen.shoplite.service;

import com.quyen.shoplite.repository.OfficeRepository;
import com.quyen.shoplite.util.DTOMapper;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;

import com.quyen.shoplite.domain.Office;
import com.quyen.shoplite.domain.Store;
import com.quyen.shoplite.domain.request.ReqOfficeDTO;
import com.quyen.shoplite.domain.response.ResOfficeDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OfficeService {

    private final OfficeRepository officeRepository;
    private final CurrentStoreService currentStoreService;

    @Transactional
    public ResOfficeDTO create(ReqOfficeDTO req) {
        Store store = currentStoreService.getCurrentStore();
        validateDuplicateName(store.getId(), req.getName(), null);

        Office office = Office.builder()
                .store(store)
                .name(req.getName().trim())
                .officeLat(req.getOfficeLat())
                .officeLng(req.getOfficeLng())
                .radius(req.getRadius())
                .build();
        return DTOMapper.toResOfficeDTO(officeRepository.save(office));
    }

    public ResOfficeDTO findById(Integer id) {
        return DTOMapper.toResOfficeDTO(findEntityById(id));
    }

    public List<ResOfficeDTO> findAll() {
        Long storeId = currentStoreService.getCurrentStoreId();
        return officeRepository.findAllByStoreIdOrderByIdAsc(storeId).stream()
                .map(DTOMapper::toResOfficeDTO)
                .toList();
    }

    @Transactional
    public ResOfficeDTO update(Integer id, ReqOfficeDTO req) {
        Office office = findEntityById(id);
        Long storeId = currentStoreService.getCurrentStoreId();
        validateDuplicateName(storeId, req.getName(), id);

        office.setName(req.getName().trim());
        office.setOfficeLat(req.getOfficeLat());
        office.setOfficeLng(req.getOfficeLng());
        office.setRadius(req.getRadius());
        return DTOMapper.toResOfficeDTO(officeRepository.save(office));
    }

    @Transactional
    public void delete(Integer id) {
        Office office = findEntityById(id);
        officeRepository.delete(office);
    }

    private Office findEntityById(Integer id) {
        Long storeId = currentStoreService.getCurrentStoreId();
        return officeRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Office not found with id=" + id));
    }

    private void validateDuplicateName(Long storeId, String name, Integer officeId) {
        String normalizedName = name.trim();
        boolean duplicate = officeId == null
                ? officeRepository.existsByStoreIdAndName(storeId, normalizedName)
                : officeRepository.existsByStoreIdAndNameAndIdNot(storeId, normalizedName, officeId);
        if (duplicate) {
            throw new BadRequestException("Office name already exists: " + normalizedName);
        }
    }

}
