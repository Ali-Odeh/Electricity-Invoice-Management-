package Electricity.Management.dto;

import Electricity.Management.Enum.Role;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignRoleRequest {

    @NotNull(message = "User ID is required")
    private Integer userId;

    @NotNull(message = "Role is required")
    private Role role;
}
