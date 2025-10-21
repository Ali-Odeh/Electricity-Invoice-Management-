package Electricity.Management.service;

import Electricity.Management.Enum.UserRole;
import Electricity.Management.dto.CreateProviderRequest;
import Electricity.Management.dto.CreateUserRequest;
import Electricity.Management.entity.PricingHistory;
import Electricity.Management.entity.Provider;
import Electricity.Management.entity.User;
import Electricity.Management.exception.ResourceNotFoundException;
import Electricity.Management.exception.BadRequestException;
import Electricity.Management.repository.PricingHistoryRepository;
import Electricity.Management.repository.ProviderRepository;
import Electricity.Management.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import Electricity.Management.dto.UpdatePriceRequest;
import java.time.LocalDateTime;
import java.util.List;


@Service
public class AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private PricingHistoryRepository pricingHistoryRepository;


    @Transactional
    public User createCustomer(CreateUserRequest request) {

        logger.info("Creating new customer with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        Provider provider = providerRepository.findById(request.getProviderId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));

        User user = new User();
        user.setProvider(provider);
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setAddress(request.getAddress());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole(UserRole.Customer);
        user.setIsActive(true);

        User savedUser = userRepository.save(user);

        logger.info("Customer created successfully with ID: {}", savedUser.getUserId());

        return savedUser;
    }


    @Transactional
    public Provider createProvider(CreateProviderRequest request, Integer adminUserId) {

        logger.info("Creating new provider: {}", request.getName());

        if (providerRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Provider Email already exists");
        }

        Provider provider = new Provider();
        provider.setName(request.getName());
        provider.setCity(request.getCity());
        provider.setEmail(request.getEmail());
        provider.setPhoneNumber(request.getPhoneNumber());
        provider.setCurrentKwhPrice(request.getCurrentKwhPrice());
        provider.setIsActive(true);

        Provider savedProvider = providerRepository.save(provider);

        // Create initial pricing history
        // Get Admin info
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        // Create a pricing record & see who created it
        PricingHistory pricingHistory = new PricingHistory();
        pricingHistory.setProvider(savedProvider);
        pricingHistory.setChangedByUser(admin);
        pricingHistory.setKwhPrice(request.getCurrentKwhPrice());
        pricingHistory.setValidFrom(LocalDateTime.now());
        pricingHistory.setValidTo(null);
        pricingHistoryRepository.save(pricingHistory);

        logger.info("Provider created successfully with ID: {}", savedProvider.getProviderId());

        return savedProvider;
    }


    @org.springframework.transaction.annotation.Transactional
    public Provider updateProviderPrice(Integer providerId, UpdatePriceRequest request, Integer adminUserId) {
        logger.info("Updating price for provider ID: {}", providerId);

        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        // Close current pricing period
        PricingHistory currentPricing = pricingHistoryRepository.findCurrentPrice(providerId)
                .orElse(null);

        if (currentPricing != null) {
            currentPricing.setValidTo(LocalDateTime.now());
            pricingHistoryRepository.save(currentPricing);
        }

        // Create new pricing history
        PricingHistory newPricing = new PricingHistory();
        newPricing.setProvider(provider);
        newPricing.setChangedByUser(admin);
        newPricing.setKwhPrice(request.getNewKwhPrice());
        newPricing.setValidFrom(LocalDateTime.now());
        newPricing.setValidTo(null);
        pricingHistoryRepository.save(newPricing);

        // Update provider's current price
        provider.setCurrentKwhPrice(request.getNewKwhPrice());
        Provider updatedProvider = providerRepository.save(provider);

        logger.info("Provider price updated successfully to: {}", request.getNewKwhPrice());
        return updatedProvider;
    }


    public Provider getProviderById(Integer providerId) {
        logger.info("Fetching provider with ID: {}", providerId);
        return providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
    }



    @Transactional
    public void deleteUser(Integer userId) {
        logger.info("Deleting user with ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        userRepository.delete(user);
        logger.info("User deleted successfully");
    }


}
