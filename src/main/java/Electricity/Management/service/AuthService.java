package Electricity.Management.service;

import Electricity.Management.dto.LoginRequest;
import Electricity.Management.dto.LoginResponse;
import Electricity.Management.entity.User;
import Electricity.Management.exception.BadRequestException;
import Electricity.Management.repository.UserRepository;
import Electricity.Management.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest loginRequest) {
        logger.info("Login attempt for user: {}", loginRequest.getEmail());

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        // Simple password check (no encryption for now)
        if (!user.getPassword().equals(loginRequest.getPassword())) {
            throw new BadRequestException("Invalid email or password");
        }


        // Generate JWT token
        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole().name(),
                user.getUserId() );


        Integer providerId;
        if (user.getProvider() != null) {
            providerId = user.getProvider().getProviderId();
        } else { providerId = null; }

        String providerName = user.getProvider() != null ? user.getProvider().getName() : null;


        logger.info("User {} logged in successfully with role {}", user.getEmail(), user.getRole());

        return new LoginResponse(token, user.getUserId(), user.getName(), user.getEmail(),
                user.getRole().name(), providerId, providerName);
    }
}

