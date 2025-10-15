package Electricity.Management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePriceRequest {
    
    @NotNull(message = "New kWh price is required")
    private BigDecimal newKwhPrice;
}
