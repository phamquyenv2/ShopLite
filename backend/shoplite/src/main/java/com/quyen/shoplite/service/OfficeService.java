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

import java.time.LocalTime;
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
                .shiftStart(defaultShiftStart(req.getShiftStart()))
                .shiftEnd(defaultShiftEnd(req.getShiftEnd()))
                .lateGraceMinutes(defaultLateGrace(req.getLateGraceMinutes()))
                .autoCheckoutTime(defaultAutoCheckoutTime(req.getAutoCheckoutTime()))
                .build();
        validateSchedule(office.getShiftStart(), office.getShiftEnd(), office.getAutoCheckoutTime());
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
        office.setShiftStart(defaultShiftStart(req.getShiftStart()));
        office.setShiftEnd(defaultShiftEnd(req.getShiftEnd()));
        office.setLateGraceMinutes(defaultLateGrace(req.getLateGraceMinutes()));
        office.setAutoCheckoutTime(defaultAutoCheckoutTime(req.getAutoCheckoutTime()));
        validateSchedule(office.getShiftStart(), office.getShiftEnd(), office.getAutoCheckoutTime());
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

    private LocalTime defaultShiftStart(LocalTime value) {
        return value != null ? value : LocalTime.of(8, 0);
    }

    private LocalTime defaultShiftEnd(LocalTime value) {
        return value != null ? value : LocalTime.of(17, 0);
    }

    private Integer defaultLateGrace(Integer value) {
        return value != null ? value : 0;
    }

    private LocalTime defaultAutoCheckoutTime(LocalTime value) {
        return value != null ? value : LocalTime.of(23, 59);
    }

    private void validateSchedule(LocalTime shiftStart, LocalTime shiftEnd, LocalTime autoCheckoutTime) {
        if (!shiftEnd.isAfter(shiftStart)) {
            throw new BadRequestException("shiftEnd must be after shiftStart");
        }
        if (autoCheckoutTime.isBefore(shiftEnd)) {
            throw new BadRequestException("autoCheckoutTime must be equal to or after shiftEnd");
        }
    }
}
