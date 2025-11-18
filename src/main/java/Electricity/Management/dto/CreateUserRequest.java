package Electricity.Management.dto;


import Electricity.Management.Enum.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotNull(message = "Provider ID is required")
    private Integer providerId;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    private String address;
    private String phoneNumber;

    // Roles to assign (Admin selects these)
    private List<Role> roles;


}
