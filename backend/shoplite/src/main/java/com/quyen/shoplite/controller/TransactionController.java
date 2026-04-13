package com.quyen.shoplite.controller;

import com.quyen.shoplite.domain.request.ReqTransactionDTO;
import com.quyen.shoplite.domain.response.ResTransactionDTO;
import com.quyen.shoplite.service.TransactionService;
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
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @ApiMessage("Create transaction success")
    public ResponseEntity<ResTransactionDTO> create(@Valid @RequestBody ReqTransactionDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.create(req));
    }

    @GetMapping("/{id}")
    @ApiMessage("Get transaction success")
    public ResponseEntity<ResTransactionDTO> findById( @Positive(message = " must be greater than 0") Integer id) {
        return ResponseEntity.ok(transactionService.findById(id));
    }

    @GetMapping
    @ApiMessage("Get transactions success")
    public ResponseEntity<List<ResTransactionDTO>> findAll() {
        return ResponseEntity.ok(transactionService.findAll());
    }

    @GetMapping("/order/{orderId}")
    @ApiMessage("Get order transactions success")
    public ResponseEntity<List<ResTransactionDTO>> findByOrderId( @Positive(message = " must be greater than 0") Integer orderId) {
        return ResponseEntity.ok(transactionService.findByOrderId(orderId));
    }
}

