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
    public ResponseEntity<ResTransactionDTO> findById(
            @PathVariable("id") @Positive(message = "id must be greater than 0") Integer id) {
        return ResponseEntity.ok(transactionService.findById(id));
    }

    @GetMapping
    @ApiMessage("Get transactions success")
    public ResponseEntity<List<ResTransactionDTO>> findAll() {
        return ResponseEntity.ok(transactionService.findAll());
    }

    @GetMapping("/fund-account/{fundAccountId}")
    @ApiMessage("Get fund account transactions success")
    public ResponseEntity<List<ResTransactionDTO>> findByFundAccountId(
            @PathVariable("fundAccountId") @Positive(message = "fundAccountId must be greater than 0") Integer fundAccountId) {
        return ResponseEntity.ok(transactionService.findByFundAccountId(fundAccountId));
    }

    @GetMapping("/payment/{paymentId}")
    @ApiMessage("Get payment transactions success")
    public ResponseEntity<List<ResTransactionDTO>> findByPaymentId(
            @PathVariable("paymentId") @Positive(message = "paymentId must be greater than 0") Integer paymentId) {
        return ResponseEntity.ok(transactionService.findByPaymentId(paymentId));
    }
}
