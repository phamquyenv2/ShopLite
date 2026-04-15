package com.quyen.shoplite.controller;

import com.quyen.shoplite.domain.request.ReqUnitUpsertDTO;
import com.quyen.shoplite.domain.response.ResUnitDTO;
import com.quyen.shoplite.service.UnitService;
import com.quyen.shoplite.util.annotation.ApiMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/units")
@RequiredArgsConstructor
public class UnitController {

    private final UnitService unitService;

    @PostMapping
    @ApiMessage("Create unit success")
    public ResponseEntity<ResUnitDTO> create(@Valid @RequestBody ReqUnitUpsertDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(unitService.create(req));
    }

    @GetMapping("/{id}")
    @ApiMessage("Get unit success")
    public ResponseEntity<ResUnitDTO> findById(@PathVariable("id") @Positive(message = " must be greater than 0") Integer id) {
        return ResponseEntity.ok(unitService.findById(id));
    }

    @GetMapping
    @ApiMessage("Get units success")
    public ResponseEntity<List<ResUnitDTO>> findAll() {
        return ResponseEntity.ok(unitService.findAll());
    }

    @PutMapping("/{id}")
    @ApiMessage("Update unit success")
    public ResponseEntity<ResUnitDTO> update(@PathVariable("id") @Positive(message = " must be greater than 0") Integer id, @Valid @RequestBody ReqUnitUpsertDTO req) {
        return ResponseEntity.ok(unitService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Delete unit success")
    public ResponseEntity<Void> delete(@PathVariable("id") @Positive(message = " must be greater than 0") Integer id) {
        unitService.delete(id);
        return ResponseEntity.noContent().build();
    }
}


