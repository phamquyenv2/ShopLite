package com.quyen.shoplite.controller;

import com.quyen.shoplite.domain.response.ResSalesInitDTO;
import com.quyen.shoplite.service.SalesInitService;
import com.quyen.shoplite.util.annotation.ApiMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
public class SalesInitController {

    private final SalesInitService salesInitService;

    @GetMapping("/init")
    @ApiMessage("Get sales init data")
    public ResponseEntity<ResSalesInitDTO> getInitData() {
        return ResponseEntity.ok(salesInitService.getInitData());
    }
}
