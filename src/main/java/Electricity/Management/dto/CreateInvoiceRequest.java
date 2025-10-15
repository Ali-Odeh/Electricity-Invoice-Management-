package Electricity.Management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateInvoiceRequest {
    
    @NotNull(message = "Customer ID is required")
    private Integer customerId;
    
    @NotNull(message = "kWh consumed is required")
    private BigDecimal kwhConsumed;
    
    @NotNull(message = "Issue date is required")
    private LocalDate issueDate;
    
    @NotNull(message = "Due date is required")
    private LocalDate dueDate;
}
