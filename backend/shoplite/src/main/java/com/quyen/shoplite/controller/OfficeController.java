package com.quyen.shoplite.controller;

import com.quyen.shoplite.domain.request.ReqOfficeDTO;
import com.quyen.shoplite.domain.response.ResOfficeDTO;
import com.quyen.shoplite.service.OfficeService;
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
@RequestMapping("/api/v1/offices")
@RequiredArgsConstructor
public class OfficeController {

    private final OfficeService officeService;

    @PostMapping
    @ApiMessage("Create office success")
    public ResponseEntity<ResOfficeDTO> create(@Valid @RequestBody ReqOfficeDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(officeService.create(req));
    }

    @GetMapping("/{id}")
    @ApiMessage("Get office success")
    public ResponseEntity<ResOfficeDTO> findById(@PathVariable("id") @Positive(message = " must be greater than 0") Integer id) {
        return ResponseEntity.ok(officeService.findById(id));
    }

    @GetMapping
    @ApiMessage("Get offices success")
    public ResponseEntity<List<ResOfficeDTO>> findAll() {
        return ResponseEntity.ok(officeService.findAll());
    }

    @PutMapping("/{id}")
    @ApiMessage("Update office success")
    public ResponseEntity<ResOfficeDTO> update(@PathVariable("id") @Positive(message = " must be greater than 0") Integer id, @Valid @RequestBody ReqOfficeDTO req) {
        return ResponseEntity.ok(officeService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Delete office success")
    public ResponseEntity<Void> delete(@PathVariable("id") @Positive(message = " must be greater than 0") Integer id) {
        officeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

