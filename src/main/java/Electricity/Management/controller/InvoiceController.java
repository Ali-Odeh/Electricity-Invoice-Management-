package Electricity.Management.controller;


import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import Electricity.Management.dto.CreateInvoiceRequest;
import Electricity.Management.dto.UpdateInvoiceRequest;
import Electricity.Management.entity.Invoice;
import Electricity.Management.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/invoices")
@CrossOrigin(origins = "*")
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;


    @PostMapping
    @Operation(summary = "Create Invoice", description = "Invoice Creator or Super Creator creates a new invoice")
    public ResponseEntity<Invoice> createInvoice(
            @Valid @RequestBody CreateInvoiceRequest request, @RequestParam Integer creatorUserId) {

        Invoice invoice = invoiceService.createInvoice(request, creatorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(invoice);
    }


    @PutMapping("/{id}")
    @Operation(summary = "Update Invoice", description = "Update an existing invoice")
    public ResponseEntity<Invoice> updateInvoice(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateInvoiceRequest request,
            @RequestParam Integer updaterUserId) {

        Invoice invoice = invoiceService.updateInvoice(id, request, updaterUserId);
        return ResponseEntity.ok(invoice);
    }


    @GetMapping("/{id}")
    @Operation(summary = "Get Invoice by ID", description = "Get invoice details")
    public ResponseEntity<Invoice> getInvoiceById(@PathVariable Integer id, @RequestParam Integer userId) {

        Invoice invoice = invoiceService.getInvoiceById(id, userId);
        return ResponseEntity.ok(invoice);
    }


    @GetMapping("/my-invoices")
    @Operation(summary = "Get My Invoices", description = "Customer retrieves their own invoices")
    public ResponseEntity<List<Invoice>> getMyInvoices(@RequestParam Integer customerId) {

        List<Invoice> invoices = invoiceService.getMyInvoices(customerId);
        return ResponseEntity.ok(invoices);
    }


    @GetMapping("/my-created")
    @Operation(summary = "Get My Created Invoices", description = "Invoice Creator retrieves invoices they created")
    public ResponseEntity<List<Invoice>> getMyCreatedInvoices(@RequestParam Integer creatorId) {

        List<Invoice> invoices = invoiceService.getMyCreatedInvoices(creatorId);
        return ResponseEntity.ok(invoices);
    }


    @GetMapping("/provider")
    @Operation(summary = "Get Provider Invoices", description = "Super Creator retrieves all invoices for their provider")
    public ResponseEntity<List<Invoice>> getProviderInvoices(@RequestParam Integer providerId) {

        List<Invoice> invoices = invoiceService.getProviderInvoices(providerId);
        return ResponseEntity.ok(invoices);
    }


}



