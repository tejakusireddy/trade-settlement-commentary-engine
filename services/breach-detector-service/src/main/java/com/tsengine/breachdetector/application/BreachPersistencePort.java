package com.tsengine.breachdetector.application;

import com.tsengine.breachdetector.domain.Breach;

public interface BreachPersistencePort {

    Breach save(Breach breach);
}
