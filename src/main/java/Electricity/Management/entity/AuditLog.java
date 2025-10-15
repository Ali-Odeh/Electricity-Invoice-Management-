package Electricity.Management.entity;


import Electricity.Management.Enum.AuditAction;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "Audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor


public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Integer auditId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "invoice_id")
  //  @JsonIgnoreProperties({"customer", "provider", "createdByUser", "pricing"})
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "performed_by_user_id")
   // @JsonIgnoreProperties({"password", "provider", "createdAt", "updatedAt"})
    private User performedByUser;

    @Enumerated(EnumType.STRING)
    private AuditAction action;

    @Column(name = "old_value", columnDefinition = "JSON")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "JSON")
    private String newValue;

    @CreationTimestamp
    @Column(name = "performed_at")
    private LocalDateTime performedAt;
}

