package Electricity.Management.controller;

import Electricity.Management.dto.NaturalLanguageQueryRequest;
import Electricity.Management.entity.PricingHistory;
import Electricity.Management.service.GeminiService;
import io.swagger.v3.oas.annotations.Operation;
import Electricity.Management.entity.AuditLog;
import Electricity.Management.entity.Invoice;
import Electricity.Management.service.AuditService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// import Electricity.Management.dto.NaturalLanguageQueryRequest;


import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    @Autowired
    private AuditService auditService;

    @Autowired
    private GeminiService geminiService;


    @GetMapping("/invoices")
    @Operation(summary = "Get All Invoices", description = "Auditor retrieves all invoices from their provider (read-only)")
    public ResponseEntity<List<Invoice>> getAllInvoices(@RequestParam Integer auditorUserId) {

        List<Invoice> invoices = auditService.getAllInvoicesForAuditor(auditorUserId);
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/invoices/search")
    @Operation(summary = "Search Invoice by Number", description = "Auditor searches for a specific invoice by invoice number")
    public ResponseEntity<List<Invoice>> searchInvoiceByNumber(
            @RequestParam String invoiceNumber,
            @RequestParam Integer auditorUserId) {

        List<Invoice> invoices = auditService.searchInvoiceByNumber(invoiceNumber, auditorUserId);
        return ResponseEntity.ok(invoices);
    }


    @GetMapping("/logs")
    @Operation(summary = "Get All Audit Logs", description = "Auditor retrieves all audit logs from their provider")
    public ResponseEntity<List<AuditLog>> getAllAuditLogs(@RequestParam Integer auditorUserId) {

        List<AuditLog> logs = auditService.getAllAuditLogs(auditorUserId);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/logs/search")
    @Operation(summary = "Search Audit Logs by Invoice Number", description = "Auditor searches audit logs for a specific invoice number")
    public ResponseEntity<List<AuditLog>> searchAuditLogsByInvoiceNumber(
            @RequestParam String invoiceNumber,
            @RequestParam Integer auditorUserId) {

        List<AuditLog> logs = auditService.searchAuditLogsByInvoiceNumber(invoiceNumber, auditorUserId);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/invoices/{id}/history")
    @Operation(summary = "Get Invoice History", description = "Auditor retrieves change history for a specific invoice")
    public ResponseEntity<List<AuditLog>> getInvoiceHistory(@PathVariable Integer id,@RequestParam Integer auditorUserId) {

        List<AuditLog> history = auditService.getInvoiceHistory(id, auditorUserId);
        return ResponseEntity.ok(history);
    }


    @PostMapping("/query")
    @Operation(summary = "Natural Language Query", description = "Auditor asks questions in natural language (Text-to-SQL using Gemini)")
    public ResponseEntity<Map<String, Object>> naturalLanguageQuery(
            @Valid @RequestBody NaturalLanguageQueryRequest request, @RequestParam Integer auditorUserId) {

        Map<String, Object> result = geminiService.processNaturalLanguageQuery(request.getQuery(), auditorUserId);
        return ResponseEntity.ok(result);
    }



    @GetMapping("/pricing-history")
    @Operation(summary = "Get Pricing History", description = "Auditor retrieves pricing history for their provider")
    public ResponseEntity<List<PricingHistory>> getPricingHistory(@RequestParam Integer auditorUserId) {

        List<PricingHistory> history = auditService.getPricingHistory(auditorUserId);
        return ResponseEntity.ok(history);
    }


}
