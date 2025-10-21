package Electricity.Management.service;

import Electricity.Management.entity.AuditLog;
import Electricity.Management.entity.Invoice;
import Electricity.Management.entity.PricingHistory;
import Electricity.Management.entity.User;
import Electricity.Management.exception.ResourceNotFoundException;
import Electricity.Management.exception.UnauthorizedException;
import Electricity.Management.repository.AuditLogRepository;
import Electricity.Management.repository.InvoiceRepository;
import Electricity.Management.repository.PricingHistoryRepository;
import Electricity.Management.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PricingHistoryRepository pricingHistoryRepository;


    public List<Invoice> getAllInvoicesForAuditor(Integer auditorUserId) {
        logger.info("Auditor {} fetching all invoices", auditorUserId);

        User auditor = userRepository.findById(auditorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Auditor not found"));

        if (auditor.getProvider() == null) {
            throw new UnauthorizedException("Auditor must be assigned to a provider");
        }

        return invoiceRepository.findByProvider_ProviderId(auditor.getProvider().getProviderId());
    }


    public List<AuditLog> getAllAuditLogs(Integer auditorUserId) {
        logger.info("Auditor {} fetching all audit logs", auditorUserId);

        User auditor = userRepository.findById(auditorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Auditor not found"));

        if (auditor.getProvider() == null) {
            throw new UnauthorizedException("Auditor must be assigned to a provider");
        }

        return auditLogRepository.findByInvoice_Provider_ProviderId(auditor.getProvider().getProviderId());
    }


    public List<AuditLog> getInvoiceHistory(Integer invoiceId, Integer auditorUserId) {
        logger.info("Auditor {} fetching history for invoice {}", auditorUserId, invoiceId);

        User auditor = userRepository.findById(auditorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Auditor not found"));

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        // Verify auditor can access this invoice
        if (!invoice.getProvider().getProviderId().equals(auditor.getProvider().getProviderId())) {
            throw new UnauthorizedException("You can only view invoices from your provider");
        }

        return auditLogRepository.findByInvoice_InvoiceId(invoiceId);
    }


   public List<PricingHistory> getPricingHistory(Integer auditorUserId) {
       logger.info("Auditor {} fetching pricing history", auditorUserId);

        User auditor = userRepository.findById(auditorUserId)
               .orElseThrow(() -> new ResourceNotFoundException("Auditor not found"));

       if (auditor.getProvider() == null) {
           throw new UnauthorizedException("Auditor must be assigned to a provider");
       }

       return pricingHistoryRepository.findByProvider_ProviderId(
                auditor.getProvider().getProviderId()
        );
    }


    public List<AuditLog> searchAuditLogsByInvoiceNumber(String invoiceNumber, Integer auditorUserId) {
        logger.info("Auditor {} searching audit logs for invoice number: {}", auditorUserId, invoiceNumber);

        User auditor = userRepository.findById(auditorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Auditor not found"));

        if (auditor.getProvider() == null) {
            throw new UnauthorizedException("Auditor must be assigned to a provider");
        }

        return auditLogRepository.findByInvoice_InvoiceNumberAndInvoice_Provider_ProviderId(
                invoiceNumber,
                auditor.getProvider().getProviderId()
        );
    }


    public List<Invoice> searchInvoiceByNumber(String invoiceNumber, Integer auditorUserId) {
        logger.info("Auditor {} searching for invoice number: {}", auditorUserId, invoiceNumber);

        User auditor = userRepository.findById(auditorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Auditor not found"));

        if (auditor.getProvider() == null) {
            throw new UnauthorizedException("Auditor must be assigned to a provider");
        }

        return invoiceRepository.findByInvoiceNumberAndProvider_ProviderId(
                invoiceNumber,
                auditor.getProvider().getProviderId()
        );
    }

}

