package Electricity.Management.service;


import Electricity.Management.Enum.AuditAction;
import Electricity.Management.Enum.PaymentStatus;
import Electricity.Management.Enum.Role;
import Electricity.Management.dto.CreateInvoiceRequest;
import Electricity.Management.dto.UpdateInvoiceRequest;
import Electricity.Management.entity.*;
import Electricity.Management.exception.BadRequestException;
import Electricity.Management.exception.ResourceNotFoundException;
import Electricity.Management.exception.UnauthorizedException;
import Electricity.Management.repository.AuditLogRepository;
import Electricity.Management.repository.InvoiceRepository;
import Electricity.Management.repository.PricingHistoryRepository;
import Electricity.Management.repository.UserRepository;
import Electricity.Management.repository.UserRoleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class InvoiceService {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceService.class);

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PricingHistoryRepository pricingHistoryRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private ObjectMapper objectMapper;


    @Transactional
    public Invoice createInvoice(CreateInvoiceRequest request, Integer creatorUserId) {

        logger.info("Creating invoice for customer ID: {}", request.getCustomerId());

        User creator = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Creator user not found"));

        User customer = userRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        // Check if user has Customer role
        if (!userRoleRepository.existsByUser_UserIdAndRole(customer.getUserId(), Role.Customer)) {
            throw new BadRequestException("Selected user is not a customer");
        }

        // Verify creator and customer are in the same provider
        if (!creator.getProvider().getProviderId().equals(customer.getProvider().getProviderId())) {
            throw new UnauthorizedException("Cannot create invoice for customer from different provider");
        }

        Provider provider = creator.getProvider();

        PricingHistory pricing = pricingHistoryRepository.findCurrentPrice(provider.getProviderId())
                .orElseThrow(() -> new BadRequestException("No active pricing found for provider"));


        BigDecimal totalAmount = request.getKwhConsumed().multiply(pricing.getKwhPrice());

        String invoiceNumber = generateInvoiceNumber(provider.getProviderId());

        Invoice invoice = new Invoice();
        invoice.setCustomer(customer);
        invoice.setProvider(provider);
        invoice.setCreatedByUser(creator);
        invoice.setPricing(pricing);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setKwhConsumed(request.getKwhConsumed());
        invoice.setTotalAmount(totalAmount);
        invoice.setIssueDate(request.getIssueDate());
        invoice.setDueDate(request.getDueDate());
        invoice.setPaymentStatus(PaymentStatus.Pending);

        Invoice savedInvoice = invoiceRepository.save(invoice);


        createAuditLog(savedInvoice, creator, AuditAction.create, null, invoiceToMap(savedInvoice));

        logger.info("Invoice created successfully with number: {}", invoiceNumber);

        return savedInvoice;
    }


    @Transactional
    public Invoice updateInvoice(Integer invoiceId, UpdateInvoiceRequest request, Integer updaterUserId) {

        logger.info("Updating invoice ID: {}", invoiceId);

        User updater = userRepository.findById(updaterUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Updater user not found"));

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));


        // Check permissions based on user's roles
        boolean isInvoiceCreator = userRoleRepository.existsByUser_UserIdAndRole(updaterUserId, Role.Invoice_Creator);
        boolean isSuperCreator = userRoleRepository.existsByUser_UserIdAndRole(updaterUserId, Role.Super_Creator);

        if (isInvoiceCreator && !isSuperCreator) {
            // Invoice Creator can only edit their own invoices
            if (!invoice.getCreatedByUser().getUserId().equals(updaterUserId)) {
                throw new UnauthorizedException("You can only edit invoices you created");
            }
        }
        else if (isSuperCreator) {
            // Super Creator can edit any invoice in their provider
            if (!invoice.getProvider().getProviderId().equals(updater.getProvider().getProviderId())) {
                throw new UnauthorizedException("You can only edit invoices from your provider");
            }
        }
        else {
            throw new UnauthorizedException("You don't have permission to edit invoices");
        }

        // Store old values
        Map<String, Object> oldValues = invoiceToMap(invoice);

        // Update fields
            invoice.setKwhConsumed(request.getKwhConsumed());
            invoice.setTotalAmount(request.getKwhConsumed().multiply(invoice.getPricing().getKwhPrice()));
            invoice.setDueDate(request.getDueDate());
            invoice.setPaymentStatus(request.getPaymentStatus());
            invoice.setPaymentDate(request.getPaymentDate());

        Invoice updatedInvoice = invoiceRepository.save(invoice);

        createAuditLog(updatedInvoice, updater, AuditAction.update, oldValues, invoiceToMap(updatedInvoice));

        logger.info("Invoice updated successfully");

        return updatedInvoice;
    }


    public Invoice getInvoiceById(Integer invoiceId, Integer userId) {
        logger.info("Fetching invoice ID: {}", invoiceId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        // Check permissions based on user's roles
        boolean isCustomer = userRoleRepository.existsByUser_UserIdAndRole(userId, Role.Customer);
        boolean isInvoiceCreator = userRoleRepository.existsByUser_UserIdAndRole(userId, Role.Invoice_Creator);
        boolean isSuperCreator = userRoleRepository.existsByUser_UserIdAndRole(userId, Role.Super_Creator);

        if (isCustomer) {
            if (!invoice.getCustomer().getUserId().equals(userId)) {
                throw new UnauthorizedException("You can only view your own invoices");
            }
        }
        else if (isInvoiceCreator || isSuperCreator) {
            if (!invoice.getProvider().getProviderId().equals(user.getProvider().getProviderId())) {
                throw new UnauthorizedException("You can only view invoices from your provider");
            }
        }

        return invoice;
    }


    public List<Invoice> getMyInvoices(Integer customerId) {
        logger.info("Fetching invoices for customer ID: {}", customerId);
        return invoiceRepository.findByCustomer_UserId(customerId);
    }


    public List<Invoice> getMyCreatedInvoices(Integer creatorId) {
        logger.info("Fetching invoices created by user ID: {}", creatorId);
        return invoiceRepository.findByCreatedByUser_UserId(creatorId);
    }


    public List<Invoice> getProviderInvoices(Integer providerId) {
        logger.info("Fetching all invoices for provider ID: {}", providerId);
        return invoiceRepository.findByProvider_ProviderId(providerId);
    }


    private void createAuditLog(Invoice invoice, User performedBy, AuditAction action,
                                Map<String, Object> oldValue, Map<String, Object> newValue) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setInvoice(invoice);
            auditLog.setPerformedByUser(performedBy);
            auditLog.setAction(action);
            auditLog.setOldValue(oldValue != null ? objectMapper.writeValueAsString(oldValue) : null);
            auditLog.setNewValue(newValue != null ? objectMapper.writeValueAsString(newValue) : null);
            auditLogRepository.save(auditLog);
            logger.info("Audit log created for invoice ID: {} with action: {}", invoice.getInvoiceId(), action);
        }
        catch (Exception e) {
            logger.error("Failed to create audit log: {}", e.getMessage());
        }
    }

    private Map<String, Object> invoiceToMap(Invoice invoice) {
        Map<String, Object> map = new HashMap<>();
        map.put("invoiceId", invoice.getInvoiceId());
        map.put("invoiceNumber", invoice.getInvoiceNumber());
        map.put("customerId", invoice.getCustomer().getUserId());
        map.put("kwhConsumed", invoice.getKwhConsumed());
        map.put("totalAmount", invoice.getTotalAmount());
        map.put("issueDate", invoice.getIssueDate().toString());
        map.put("dueDate", invoice.getDueDate().toString());
        map.put("paymentStatus", invoice.getPaymentStatus().name());
        map.put("paymentDate", invoice.getPaymentDate() != null ? invoice.getPaymentDate().toString() : null);
        return map;
    }


    private String generateInvoiceNumber(Integer providerId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return String.format("INV-%d-%s", providerId, timestamp);
    }





}
