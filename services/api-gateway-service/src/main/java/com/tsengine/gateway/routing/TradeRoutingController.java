package com.tsengine.gateway.routing;

import com.tsengine.gateway.config.ServiceUrlsProperties;
import com.tsengine.gateway.infrastructure.DownstreamHttpClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trades")
public class TradeRoutingController {

    private final DownstreamHttpClient downstreamHttpClient;
    private final ServiceUrlsProperties serviceUrls;

    public TradeRoutingController(DownstreamHttpClient downstreamHttpClient, ServiceUrlsProperties serviceUrls) {
        this.downstreamHttpClient = downstreamHttpClient;
        this.serviceUrls = serviceUrls;
    }

    @GetMapping
    public ResponseEntity<String> getTrades(HttpServletRequest request) {
        return downstreamHttpClient.forward(request, serviceUrls.getTradeIngestUrl() + request.getRequestURI());
    }

    @PostMapping
    public ResponseEntity<String> createTrade(HttpServletRequest request) {
        return downstreamHttpClient.forward(request, serviceUrls.getTradeIngestUrl() + request.getRequestURI());
    }

    @PostMapping("/batch")
    public ResponseEntity<String> createTradeBatch(HttpServletRequest request) {
        return downstreamHttpClient.forward(request, serviceUrls.getTradeIngestUrl() + request.getRequestURI());
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> getTradeById(@PathVariable String id, HttpServletRequest request) {
        return downstreamHttpClient.forward(request, serviceUrls.getTradeIngestUrl() + request.getRequestURI());
    }
}
