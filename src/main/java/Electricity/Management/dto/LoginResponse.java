package Electricity.Management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String type = "Bearer";
    private Integer userId;
    private String name;
    private String email;
    private String role;
    private Integer providerId;
    private String providerName;


    public LoginResponse(String token, Integer userId, String name, String email, String role, Integer providerId, String providerName) {
        this.token = token;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.providerId = providerId;
        this.providerName = providerName;
    }
}
