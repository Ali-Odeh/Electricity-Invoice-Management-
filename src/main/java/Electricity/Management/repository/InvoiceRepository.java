package Electricity.Management.repository;


import Electricity.Management.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {

    List<Invoice> findByCustomer_UserId(Integer customerId);
    List<Invoice> findByProvider_ProviderId(Integer providerId);
    List<Invoice> findByCreatedByUser_UserId(Integer createdByUserId);
    List<Invoice> findByInvoiceNumberAndProvider_ProviderId(String invoiceNumber, Integer providerId);

}
