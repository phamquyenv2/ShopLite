package com.quyen.shoplite.controller;

import com.quyen.shoplite.domain.response.ResDashboardDTO;
import com.quyen.shoplite.service.DashboardService;
import com.quyen.shoplite.util.annotation.ApiMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/today")
    @ApiMessage("Get dashboard data")
    public ResponseEntity<ResDashboardDTO> getTodayDashboard(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(dashboardService.getDashboard(jwt.getSubject()));
    }
}
