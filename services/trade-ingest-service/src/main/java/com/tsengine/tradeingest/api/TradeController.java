package com.tsengine.tradeingest.api;

import com.tsengine.common.ApiResponse;
import com.tsengine.common.TradeDTO;
import com.tsengine.common.TradeStatus;
import com.tsengine.tradeingest.application.TradeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trades")
@Validated
public class TradeController {

    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @PostMapping
    public ApiResponse<TradeDTO> ingestTrade(@Valid @RequestBody TradeRequest request) {
        return ApiResponse.success(tradeService.ingestTrade(request));
    }

    @PostMapping("/batch")
    public ApiResponse<List<TradeDTO>> ingestBatch(
            @RequestBody @Size(max = 100) List<@Valid TradeRequest> requests
    ) {
        return ApiResponse.success(tradeService.ingestBatch(requests));
    }

    @GetMapping
    public ApiResponse<Page<TradeDTO>> listTrades(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) TradeStatus status
    ) {
        return ApiResponse.success(tradeService.listTrades(page, size, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<TradeDTO> getTrade(@PathVariable UUID id) {
        return ApiResponse.success(tradeService.getTrade(id));
    }
}
