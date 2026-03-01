package com.tsengine.breachdetector.infrastructure;

import com.tsengine.breachdetector.application.BreachPersistencePort;
import com.tsengine.breachdetector.domain.Breach;
import com.tsengine.breachdetector.domain.BreachRepository;
import org.springframework.stereotype.Repository;

@Repository
public class BreachJpaRepository implements BreachPersistencePort {

    private final BreachRepository breachRepository;

    public BreachJpaRepository(BreachRepository breachRepository) {
        this.breachRepository = breachRepository;
    }

    @Override
    public Breach save(Breach breach) {
        return breachRepository.save(breach);
    }
}
