package Electricity.Management.repository;


import Electricity.Management.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {

    List<AuditLog> findByInvoice_InvoiceId(Integer invoiceId);
    List<AuditLog> findByInvoice_Provider_ProviderId(Integer providerId);
    List<AuditLog> findByInvoice_InvoiceNumberAndInvoice_Provider_ProviderId(String invoiceNumber, Integer providerId);

}
