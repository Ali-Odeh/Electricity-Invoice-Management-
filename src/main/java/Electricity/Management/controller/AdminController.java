package Electricity.Management.controller;


import Electricity.Management.Enum.Role;
import Electricity.Management.dto.AssignRoleRequest;
import Electricity.Management.dto.CreateProviderRequest;
import Electricity.Management.dto.CreateUserRequest;
import Electricity.Management.dto.UpdatePriceRequest;
import Electricity.Management.entity.Provider;
import Electricity.Management.entity.User;
import Electricity.Management.entity.UserRole;
import Electricity.Management.service.AdminService;
import Electricity.Management.service.UserRoleService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserRoleService userRoleService;

    @PostMapping("/users")
    @Operation(summary = "Create Customer", description = "Admin creates a new customer user")
    public ResponseEntity<User> createCustomer(@Valid @RequestBody CreateUserRequest request) {

        User user = adminService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }


    @PostMapping("/providers")
    @Operation(summary = "Create Provider", description = "Admin creates a new electricity provider")
    public ResponseEntity<Provider> createProvider(
            @Valid @RequestBody CreateProviderRequest request,
            HttpServletRequest httpRequest) {

        Integer adminUserId = (Integer) httpRequest.getAttribute("userId");
        Provider provider = adminService.createProvider(request, adminUserId);

        return ResponseEntity.status(HttpStatus.CREATED).body(provider);
    }


    @PutMapping("/providers/{id}/price")
    @Operation(summary = "Update Provider Price", description = "Admin updates kWh price for a provider")
    public ResponseEntity<Provider> updateProviderPrice(
            @PathVariable Integer id,
            @Valid @RequestBody UpdatePriceRequest request,
            HttpServletRequest httpRequest) {
        
        Integer adminUserId = (Integer) httpRequest.getAttribute("userId");
        Provider provider = adminService.updateProviderPrice(id, request, adminUserId);

        return ResponseEntity.ok(provider);
    }


    @GetMapping("/providers/{id}")
    @Operation(summary = "Get Provider by ID", description = "Admin retrieves a specific provider")
    public ResponseEntity<Provider> getProviderById(@PathVariable Integer id) {

        Provider provider = adminService.getProviderById(id);
        return ResponseEntity.ok(provider);

    }


    // ========== Role Management Endpoints ==========

    @PostMapping("/users/roles")
    @Operation(summary = "Assign Role to User", description = "Admin assigns a role to a user")
    public ResponseEntity<UserRole> assignRole(@Valid @RequestBody AssignRoleRequest request) {
        UserRole userRole = userRoleService.assignRole(request.getUserId(), request.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(userRole);
    }

    @DeleteMapping("/users/{userId}/roles/{role}")
    @Operation(summary = "Remove Role from User", description = "Admin removes a role from a user")
    public ResponseEntity<Void> removeRole(
            @PathVariable Integer userId,
            @PathVariable Role role) {
        userRoleService.removeRole(userId, role);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{userId}/roles")
    @Operation(summary = "Get User Roles", description = "Admin retrieves all roles for a user")
    public ResponseEntity<List<Role>> getUserRoles(@PathVariable Integer userId) {
        List<Role> roles = userRoleService.getUserRoles(userId);
        return ResponseEntity.ok(roles);
    }

/*
    @DeleteMapping("/users/{id}")
    @Operation(summary = "Delete User", description = "Admin deletes a user")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
*/

}
