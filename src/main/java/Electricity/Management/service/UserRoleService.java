package Electricity.Management.service;

import Electricity.Management.Enum.Role;
import Electricity.Management.entity.User;
import Electricity.Management.entity.UserRole;
import Electricity.Management.exception.BadRequestException;
import Electricity.Management.exception.ResourceNotFoundException;
import Electricity.Management.repository.UserRepository;
import Electricity.Management.repository.UserRoleRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserRoleService {

    private static final Logger logger = LoggerFactory.getLogger(UserRoleService.class);

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public UserRole assignRole(Integer userId, Role role) {
        logger.info("Assigning role {} to user ID: {}", role, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if user already has this role
        if (userRoleRepository.existsByUser_UserIdAndRole(userId, role)) {
            throw new BadRequestException("User already has this role");
        }

        UserRole userRole = new UserRole(user, role);
        UserRole saved = userRoleRepository.save(userRole);

        logger.info("Role {} assigned successfully to user ID: {}", role, userId);
        return saved;
    }

    @Transactional
    public void removeRole(Integer userId, Role role) {
        logger.info("Removing role {} from user ID: {}", role, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if user has this role
        if (!userRoleRepository.existsByUser_UserIdAndRole(userId, role)) {
            throw new BadRequestException("User doesn't have this role");
        }

        // Prevent removing last role
        List<Role> userRoles = userRoleRepository.findRolesByUserId(userId);
        if (userRoles.size() <= 1) {
            throw new BadRequestException("Cannot remove the last role. User must have at least one role.");
        }

        userRoleRepository.deleteByUser_UserIdAndRole(userId, role);
        logger.info("Role {} removed successfully from user ID: {}", role, userId);
    }

    public List<Role> getUserRoles(Integer userId) {
        logger.info("Fetching roles for user ID: {}", userId);
        return userRoleRepository.findRolesByUserId(userId);
    }

    public List<UserRole> getAllUserRoles(Integer userId) {
        logger.info("Fetching all user roles for user ID: {}", userId);
        return userRoleRepository.findByUser_UserId(userId);
    }
}
