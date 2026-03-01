package com.tsengine.breachdetector.domain;

import com.tsengine.common.BreachType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BreachRepository extends JpaRepository<Breach, UUID> {

    List<Breach> findByTradeId(UUID tradeId);

    List<Breach> findByStatus(String status);

    List<Breach> findByBreachType(BreachType type);

    boolean existsByTradeIdAndBreachType(UUID tradeId, BreachType type);
}
