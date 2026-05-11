package com.quyen.shoplite.controller;

import com.quyen.shoplite.service.UserService;
import com.quyen.shoplite.util.annotation.ApiMessage;

import com.quyen.shoplite.domain.request.ReqUserDTO;
import com.quyen.shoplite.domain.response.ResUserDTO;
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
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @ApiMessage("Create user success")
    public ResponseEntity<ResUserDTO> create(@Valid @RequestBody ReqUserDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(req));
    }

    @GetMapping("/{id}")
    @ApiMessage("Get user success")
    public ResponseEntity<ResUserDTO> findById(@PathVariable("id") @Positive(message = " must be greater than 0") Integer id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @GetMapping
    @ApiMessage("Get users success")
    public ResponseEntity<List<ResUserDTO>> findAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @PutMapping("/{id}")
    @ApiMessage("Update user success")
    public ResponseEntity<ResUserDTO> update(@PathVariable("id") @Positive(message = " must be greater than 0") Integer id, @Valid @RequestBody ReqUserDTO req) {
        return ResponseEntity.ok(userService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Delete user success")
    public ResponseEntity<Void> delete(@PathVariable("id") @Positive(message = " must be greater than 0") Integer id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

