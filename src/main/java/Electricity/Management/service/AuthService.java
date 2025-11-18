package Electricity.Management.service;

import Electricity.Management.Enum.Role;
import Electricity.Management.dto.LoginRequest;
import Electricity.Management.dto.LoginResponse;
import Electricity.Management.dto.RoleSelectionRequest;
import Electricity.Management.entity.User;
import Electricity.Management.entity.UserRole;
import Electricity.Management.exception.BadRequestException;
import Electricity.Management.exception.UnauthorizedException;
import Electricity.Management.repository.UserRepository;
import Electricity.Management.repository.UserRoleRepository;
import Electricity.Management.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest loginRequest) {

        logger.info("Login attempt for user: {}", loginRequest.getEmail());

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        logger.info("User found: ID={}, Name={}", user.getUserId(), user.getName());

        // Simple password check (no encryption for now)
        if (!user.getPassword().equals(loginRequest.getPassword())) {
            throw new BadRequestException("Invalid email or password");
        }

        if (!user.getIsActive()) {
            throw new BadRequestException("User account is inactive");
        }

        // Get all roles for this user
        logger.info("Fetching roles for user ID: {}", user.getUserId());
        List<Role> userRoles;
        try {
            userRoles = userRoleRepository.findRolesByUserId(user.getUserId());
            logger.info("Found {} roles for user: {}", userRoles != null ? userRoles.size() : 0, userRoles);
        } catch (Exception e) {
            logger.error("Error fetching roles for user {}: {}", user.getUserId(), e.getMessage(), e);
            throw new BadRequestException("Error loading user roles: " + e.getMessage());
        }

        if (userRoles == null || userRoles.isEmpty()) {
            throw new BadRequestException("User has no assigned roles. Please contact administrator.");
        }

        Integer providerId = user.getProvider() != null ? user.getProvider().getProviderId() : null;
        String providerName = user.getProvider() != null ? user.getProvider().getName() : null;

        // If user has only one role, auto-select it and generate token
        if (userRoles.size() == 1) {
            Role singleRole = userRoles.get(0);
            String token = jwtUtil.generateToken(user.getEmail(), singleRole.name(), user.getUserId());
            
            logger.info("User {} logged in successfully with single role {}", user.getEmail(), singleRole);
            
            return new LoginResponse(token, user.getUserId(), user.getName(), user.getEmail(),
                    userRoles, singleRole, providerId, providerName, false);
        }

        // If user has multiple roles, return without token (requires role selection)
        logger.info("User {} has multiple roles: {}. Requires role selection.", user.getEmail(), userRoles);
        
        return new LoginResponse(null, user.getUserId(), user.getName(), user.getEmail(),
                userRoles, null, providerId, providerName, true);
    }

    public LoginResponse selectRole(Integer userId, RoleSelectionRequest request) {
        logger.info("Role selection for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        // Verify user has this role
        boolean hasRole = userRoleRepository.existsByUser_UserIdAndRole(userId, request.getSelectedRole());
        if (!hasRole) {
            throw new UnauthorizedException("You don't have permission for this role");
        }

        // Generate token with selected role
        String token = jwtUtil.generateToken(user.getEmail(), request.getSelectedRole().name(), userId);

        List<Role> userRoles = userRoleRepository.findRolesByUserId(userId);
        Integer providerId = user.getProvider() != null ? user.getProvider().getProviderId() : null;
        String providerName = user.getProvider() != null ? user.getProvider().getName() : null;

        logger.info("User {} selected role: {}", user.getEmail(), request.getSelectedRole());

        return new LoginResponse(token, user.getUserId(), user.getName(), user.getEmail(),
                userRoles, request.getSelectedRole(), providerId, providerName, false);
    }
}

