package Electricity.Management.controller;


import Electricity.Management.dto.LoginRequest;
import Electricity.Management.dto.LoginResponse;
import Electricity.Management.dto.RoleSelectionRequest;
import Electricity.Management.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate user and get JWT token. If user has multiple roles, returns list without token.")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {

        LoginResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/select-role")
    @Operation(summary = "Select Role", description = "Select a role after login (for users with multiple roles)")
    public ResponseEntity<LoginResponse> selectRole(
            @RequestParam Integer userId,
            @Valid @RequestBody RoleSelectionRequest request) {

        LoginResponse response = authService.selectRole(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/switch-role")
    @Operation(summary = "Switch Role", description = "Switch to a different role (requires valid token)")
    public ResponseEntity<LoginResponse> switchRole(
            @RequestParam Integer userId,
            @Valid @RequestBody RoleSelectionRequest request) {

        // Same logic as select-role, but can be called after already logged in
        LoginResponse response = authService.selectRole(userId, request);
        return ResponseEntity.ok(response);
    }

}
