package Electricity.Management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import Electricity.Management.Enum.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInvoiceRequest {
    private BigDecimal kwhConsumed;
    private LocalDate dueDate;
    private PaymentStatus paymentStatus;
    private LocalDate paymentDate;
}
