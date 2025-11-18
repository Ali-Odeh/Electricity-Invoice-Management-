package Electricity.Management.dto;

import Electricity.Management.Enum.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String type = "Bearer";
    private Integer userId;
    private String name;
    private String email;
    private List<Role> roles;  // Changed from single role to list of roles
    private Role selectedRole; // The currently active role (null if not selected yet)
    private Integer providerId;
    private String providerName;
    private boolean requiresRoleSelection; // True if user has multiple roles


    public LoginResponse(String token, Integer userId, String name, String email, List<Role> roles, 
                        Role selectedRole, Integer providerId, String providerName, boolean requiresRoleSelection) {
        this.token = token;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.roles = roles;
        this.selectedRole = selectedRole;
        this.providerId = providerId;
        this.providerName = providerName;
        this.requiresRoleSelection = requiresRoleSelection;
    }
}
