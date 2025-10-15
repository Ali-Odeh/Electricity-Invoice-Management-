package Electricity.Management.controller;


import Electricity.Management.dto.CreateProviderRequest;
import Electricity.Management.dto.CreateUserRequest;
import Electricity.Management.dto.UpdatePriceRequest;
import Electricity.Management.entity.PricingHistory;
import Electricity.Management.entity.Provider;
import Electricity.Management.entity.User;
import Electricity.Management.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
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

    @PostMapping("/users")
    @Operation(summary = "Create Customer", description = "Admin creates a new customer user")
    public ResponseEntity<User> createCustomer(@Valid @RequestBody CreateUserRequest request) {

        User user = adminService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PostMapping("/providers")
    @Operation(summary = "Create Provider", description = "Admin creates a new electricity provider")
    public ResponseEntity<Provider> createProvider(@Valid @RequestBody CreateProviderRequest request, Integer adminUserId) {

        Provider provider = adminService.createProvider(request, adminUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(provider);
    }


    @GetMapping("/providers")
    @Operation(summary = "Get All Providers", description = "Admin retrieves all providers")
    public ResponseEntity<List<Provider>> getAllProviders() {

        List<Provider> providers = adminService.getAllProviders();
        return ResponseEntity.ok(providers);
    }


    @PutMapping("/providers/{id}/price")
    @Operation(summary = "Update Provider Price", description = "Admin updates kWh price for a provider")
    public ResponseEntity<Provider> updateProviderPrice(
            @PathVariable Integer id,
            @Valid @RequestBody UpdatePriceRequest request,
            @RequestParam Integer adminUserId) {

        Provider provider = adminService.updateProviderPrice(id, request, adminUserId);
        return ResponseEntity.ok(provider);

    }



    @DeleteMapping("/users/{id}")
    @Operation(summary = "Delete User", description = "Admin deletes a user")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/providers/{id}")
    @Operation(summary = "Get Provider by ID", description = "Admin retrieves a specific provider")
    public ResponseEntity<Provider> getProviderById(@PathVariable Integer id) {
        Provider provider = adminService.getProviderById(id);
        return ResponseEntity.ok(provider);
    }



}
