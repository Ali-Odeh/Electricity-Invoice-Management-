package Electricity.Management.entity;


import Electricity.Management.Enum.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "Invoice")
@Data
@NoArgsConstructor
@AllArgsConstructor


public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    private Integer invoiceId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id")
  //   @JsonIgnoreProperties({"password", "provider", "createdAt", "updatedAt"})
    private User customer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "provider_id")
     // @JsonIgnoreProperties({"createdAt", "updatedAt", "isActive"})
    private Provider provider;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by_user_id")
 //   @JsonIgnoreProperties({"password", "provider", "createdAt", "updatedAt"})
    private User createdByUser;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pricing_id")
    // @JsonIgnoreProperties({"provider", "changedByUser", "createdAt"})
    private PricingHistory pricing;

    @Column(name = "invoice_number",unique = true)
    private String invoiceNumber;

    @Column(name = "kwh_consumed")
    private BigDecimal kwhConsumed;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus = PaymentStatus.Pending;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

}

