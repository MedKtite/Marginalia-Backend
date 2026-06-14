package com.marginalia.dev.controllers;

import com.marginalia.dev.dto.device.DeviceRegisterRequest;
import com.marginalia.dev.dto.device.DeviceResponse;
import com.marginalia.dev.security.CustomUserDetails;
import com.marginalia.dev.services.DeviceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/me/devices")
public class DeviceController {

    @Autowired
    private DeviceService deviceService;

    @GetMapping
    public List<DeviceResponse> list(@AuthenticationPrincipal CustomUserDetails me) {
        return deviceService.list(me.getUserId()).stream()
                .map(DeviceResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<DeviceResponse> register(@AuthenticationPrincipal CustomUserDetails me,
                                                   @Valid @RequestBody DeviceRegisterRequest req) {
        var saved = deviceService.register(req, me.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(DeviceResponse.from(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserDetails me,
                                       @PathVariable UUID id) {
        deviceService.delete(id, me.getUserId());
        return ResponseEntity.noContent().build();
    }
}
