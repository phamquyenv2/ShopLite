package com.quyen.shoplite.controller;

import com.quyen.shoplite.domain.FundAccount;
import com.quyen.shoplite.domain.response.ResFundAccountDTO;
import com.quyen.shoplite.repository.FundAccountRepository;
import com.quyen.shoplite.service.CurrentStoreService;
import com.quyen.shoplite.util.DTOMapper;
import com.quyen.shoplite.util.annotation.ApiMessage;
import com.quyen.shoplite.util.constant.FundTypeEnum;
import com.quyen.shoplite.util.error.IdInvalidException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/fund-accounts")
@RequiredArgsConstructor
public class FundAccountController {

    private final FundAccountRepository fundAccountRepository;
    private final CurrentStoreService currentStoreService;

    @Getter
    @Setter
    public static class ReqFundAccountDTO {
        @NotBlank(message = "name không được để trống")
        private String name;

        @NotNull(message = "type không được để trống")
        private FundTypeEnum type;

        private BigDecimal openingBalance;
    }

    @PostMapping
    @ApiMessage("Create fund account success")
    public ResponseEntity<ResFundAccountDTO> create(@Valid @RequestBody ReqFundAccountDTO req) {
        var store = currentStoreService.getCurrentStore();
        BigDecimal opening = req.getOpeningBalance() != null ? req.getOpeningBalance() : BigDecimal.ZERO;
        FundAccount account = FundAccount.builder()
                .store(store)
                .name(req.getName())
                .type(req.getType())
                .openingBalance(opening)
                .balance(opening)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DTOMapper.toResFundAccountDTO(fundAccountRepository.save(account)));
    }

    @GetMapping
    @ApiMessage("Get fund accounts success")
    public ResponseEntity<List<ResFundAccountDTO>> findAll() {
        return ResponseEntity.ok(
                fundAccountRepository.findAllByStoreId(currentStoreService.getCurrentStoreId()).stream()
                        .map(DTOMapper::toResFundAccountDTO)
                        .toList());
    }

    @GetMapping("/active")
    @ApiMessage("Get active fund accounts success")
    public ResponseEntity<List<ResFundAccountDTO>> findAllActive() {
        return ResponseEntity.ok(
                fundAccountRepository.findAllByStoreIdAndIsActiveTrue(currentStoreService.getCurrentStoreId()).stream()
                        .map(DTOMapper::toResFundAccountDTO)
                        .toList());
    }

    @GetMapping("/{id}")
    @ApiMessage("Get fund account success")
    public ResponseEntity<ResFundAccountDTO> findById(@PathVariable Integer id) {
        FundAccount account = fundAccountRepository.findByIdAndStoreId(id, currentStoreService.getCurrentStoreId())
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy FundAccount id=" + id));
        return ResponseEntity.ok(DTOMapper.toResFundAccountDTO(account));
    }

    @PatchMapping("/{id}/deactivate")
    @ApiMessage("Deactivate fund account success")
    public ResponseEntity<ResFundAccountDTO> deactivate(@PathVariable Integer id) {
        FundAccount account = fundAccountRepository.findByIdAndStoreId(id, currentStoreService.getCurrentStoreId())
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy FundAccount id=" + id));
        account.setIsActive(false);
        return ResponseEntity.ok(DTOMapper.toResFundAccountDTO(fundAccountRepository.save(account)));
    }
}
