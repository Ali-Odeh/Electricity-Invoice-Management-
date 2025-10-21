package Electricity.Management.entity;

import Electricity.Management.Enum.UserRole;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;


@Entity
@Data
@Table(name = "User")
@NoArgsConstructor
@AllArgsConstructor


public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;

    @ManyToOne
    @JoinColumn(name = "provider_id")
    // @JsonIgnoreProperties({"createdAt", "updatedAt", "isActive"})
    private Provider provider;

    private String name;

    private String email;

    @Column(columnDefinition = "TEXT")
    private String password;

    @Column(name = "phone_number")
    private String phoneNumber;

    private String address;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(name = "is_active")
    private Boolean isActive = true;

}
