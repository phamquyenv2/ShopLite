package com.quyen.shoplite.controller;

import com.quyen.shoplite.service.StoreInvitationService;
import com.quyen.shoplite.util.annotation.ApiMessage;

import com.quyen.shoplite.domain.request.ReqStoreInvitationDTO;
import com.quyen.shoplite.domain.response.ResStoreInvitationAcceptDTO;
import com.quyen.shoplite.domain.response.ResStoreInvitationDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/store-invitations")
@RequiredArgsConstructor
public class StoreInvitationController {

    private final StoreInvitationService storeInvitationService;

    @PostMapping
    @ApiMessage("Create store invitation success")
    public ResponseEntity<ResStoreInvitationDTO> create(@Valid @RequestBody ReqStoreInvitationDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(storeInvitationService.create(req));
    }

    @PostMapping("/{id}/accept")
    @ApiMessage("Accept store invitation success")
    public ResponseEntity<ResStoreInvitationAcceptDTO> accept(
            @PathVariable("id") @Positive(message = "id must be greater than 0") Long id) {
        return ResponseEntity.ok(storeInvitationService.accept(id));
    }

    @PostMapping("/{id}/decline")
    @ApiMessage("Decline store invitation success")
    public ResponseEntity<ResStoreInvitationDTO> decline(
            @PathVariable("id") @Positive(message = "id must be greater than 0") Long id) {
        return ResponseEntity.ok(storeInvitationService.decline(id));
    }
}
