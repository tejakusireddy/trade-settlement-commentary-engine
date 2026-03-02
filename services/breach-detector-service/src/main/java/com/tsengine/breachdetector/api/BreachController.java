package com.tsengine.breachdetector.api;

import com.tsengine.breachdetector.domain.Breach;
import com.tsengine.breachdetector.domain.BreachRepository;
import com.tsengine.common.ApiResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/breaches")
public class BreachController {

    private final BreachRepository breachRepository;
    private final BreachMapper breachMapper;

    public BreachController(BreachRepository breachRepository, BreachMapper breachMapper) {
        this.breachRepository = breachRepository;
        this.breachMapper = breachMapper;
    }

    @GetMapping
    public ApiResponse<Page<BreachResponse>> listBreaches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "detectedAt"));
        Page<BreachResponse> payload = breachRepository.findAll(pageable)
                .map(breachMapper::toResponse);
        return ApiResponse.success(payload);
    }

    @GetMapping("/{id}")
    public ApiResponse<BreachResponse> getById(@PathVariable UUID id) {
        Breach breach = breachRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Breach not found for id=" + id));
        return ApiResponse.success(breachMapper.toResponse(breach));
    }
}
