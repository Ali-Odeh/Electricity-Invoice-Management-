package Electricity.Management.repository;


import Electricity.Management.entity.PricingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface PricingHistoryRepository extends JpaRepository<PricingHistory, Integer> {

    List<PricingHistory> findByProvider_ProviderId(Integer providerId);

    @Query("SELECT p FROM PricingHistory p WHERE p.provider.providerId = :providerId " +
            "AND p.validTo IS NULL")
    Optional<PricingHistory> findCurrentPrice(@Param("providerId") Integer providerId);



}
