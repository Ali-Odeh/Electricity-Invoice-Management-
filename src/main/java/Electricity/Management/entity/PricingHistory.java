package Electricity.Management.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Pricing_History")
@Data
@NoArgsConstructor
@AllArgsConstructor


public class PricingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pricing_id")
    private Integer pricingId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "provider_id", nullable = false)
    @JsonIgnore
  //  @JsonIgnoreProperties({"createdAt", "updatedAt", "isActive"})
    private Provider provider;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "changed_by_user_id")
    @JsonIgnoreProperties({"password", "provider", "createdAt", "updatedAt"})
    private User changedByUser;

    @Column(name = "kwh_price")
    private BigDecimal kwhPrice;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_to")
    private LocalDateTime validTo;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

