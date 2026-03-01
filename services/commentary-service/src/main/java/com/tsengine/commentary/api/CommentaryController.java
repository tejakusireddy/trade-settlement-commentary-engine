package com.tsengine.commentary.api;

import com.tsengine.commentary.application.CommentaryManagementService;
import com.tsengine.commentary.application.CostTrackingService;
import com.tsengine.commentary.domain.Commentary;
import com.tsengine.common.ApiResponse;
import com.tsengine.common.CommentaryDTO;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CommentaryController {

    private final CommentaryManagementService commentaryManagementService;
    private final CommentaryMapper commentaryMapper;
    private final CostTrackingService costTrackingService;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public CommentaryController(
            CommentaryManagementService commentaryManagementService,
            CommentaryMapper commentaryMapper,
            CostTrackingService costTrackingService,
            CircuitBreakerRegistry circuitBreakerRegistry
    ) {
        this.commentaryManagementService = commentaryManagementService;
        this.commentaryMapper = commentaryMapper;
        this.costTrackingService = costTrackingService;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @GetMapping("/commentaries")
    public ApiResponse<Page<CommentaryDTO>> listCommentaries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<CommentaryDTO> payload = commentaryManagementService
                .listAll(PageRequest.of(page, size))
                .map(commentaryMapper::toDto);
        return ApiResponse.success(payload);
    }

    @GetMapping("/commentaries/{id}")
    public ApiResponse<CommentaryDTO> getById(@PathVariable UUID id) {
        Commentary commentary = commentaryManagementService.getById(id);
        return ApiResponse.success(commentaryMapper.toDto(commentary));
    }

    @GetMapping("/commentaries/breach/{breachId}")
    public ApiResponse<CommentaryDTO> getByBreachId(@PathVariable UUID breachId) {
        Commentary commentary = commentaryManagementService.getByBreachId(breachId);
        return ApiResponse.success(commentaryMapper.toDto(commentary));
    }

    @PostMapping("/commentaries/{id}/approve")
    public ApiResponse<CommentaryDTO> approveCommentary(
            @PathVariable UUID id,
            @Valid @RequestBody ApproveCommentaryRequest request
    ) {
        Commentary updated = commentaryManagementService.approve(id, request.approvedBy());
        return ApiResponse.success(commentaryMapper.toDto(updated));
    }

    @GetMapping("/ai/cost/today")
    public ApiResponse<Map<String, Object>> getDailyCost() {
        BigDecimal cost = costTrackingService.getDailyCost();
        BigDecimal cap = costTrackingService.getDailyCostCap();
        return ApiResponse.success(Map.of(
                "dailyCost", cost,
                "dailyCap", cap,
                "capExceeded", costTrackingService.isCostCapExceeded()
        ));
    }

    @GetMapping("/ai/circuit-breaker")
    public ApiResponse<Map<String, Object>> getCircuitBreakerStatus() {
        CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker("claude-api");
        return ApiResponse.success(Map.of(
                "name", breaker.getName(),
                "state", breaker.getState().name(),
                "failureRate", breaker.getMetrics().getFailureRate(),
                "bufferedCalls", breaker.getMetrics().getNumberOfBufferedCalls()
        ));
    }
}
