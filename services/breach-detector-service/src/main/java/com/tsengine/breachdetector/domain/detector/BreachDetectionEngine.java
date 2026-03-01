package com.tsengine.breachdetector.domain.detector;

import com.tsengine.breachdetector.domain.Breach;
import com.tsengine.schema.TradeEvent;
import java.util.Optional;

public interface BreachDetectionEngine {

    Optional<Breach> detect(TradeEvent event);
}
