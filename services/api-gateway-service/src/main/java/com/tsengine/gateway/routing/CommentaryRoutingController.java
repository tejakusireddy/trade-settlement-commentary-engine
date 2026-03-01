package com.tsengine.gateway.routing;

import com.tsengine.gateway.config.ServiceUrlsProperties;
import com.tsengine.gateway.infrastructure.DownstreamHttpClient;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class CommentaryRoutingController {

    private final DownstreamHttpClient downstreamHttpClient;
    private final ServiceUrlsProperties serviceUrls;

    public CommentaryRoutingController(DownstreamHttpClient downstreamHttpClient, ServiceUrlsProperties serviceUrls) {
        this.downstreamHttpClient = downstreamHttpClient;
        this.serviceUrls = serviceUrls;
    }

    @GetMapping("/commentaries")
    public ResponseEntity<String> getCommentaries(HttpServletRequest request) {
        return downstreamHttpClient.forward(request, serviceUrls.getCommentaryUrl() + request.getRequestURI());
    }

    @GetMapping("/commentaries/{id}")
    public ResponseEntity<String> getCommentaryById(@PathVariable String id, HttpServletRequest request) {
        return downstreamHttpClient.forward(request, serviceUrls.getCommentaryUrl() + request.getRequestURI());
    }

    @GetMapping("/commentaries/breach/{breachId}")
    public ResponseEntity<String> getCommentaryByBreachId(@PathVariable String breachId, HttpServletRequest request) {
        return downstreamHttpClient.forward(request, serviceUrls.getCommentaryUrl() + request.getRequestURI());
    }

    @PostMapping("/commentaries/{id}/approve")
    public ResponseEntity<String> approveCommentary(@PathVariable String id, HttpServletRequest request) {
        return downstreamHttpClient.forward(request, serviceUrls.getCommentaryUrl() + request.getRequestURI());
    }

    @GetMapping("/ai/cost/today")
    public ResponseEntity<String> getAiCostToday(HttpServletRequest request) {
        return downstreamHttpClient.forward(request, serviceUrls.getCommentaryUrl() + request.getRequestURI());
    }

    @GetMapping("/ai/circuit-breaker")
    public ResponseEntity<String> getCircuitBreaker(HttpServletRequest request) {
        return downstreamHttpClient.forward(request, serviceUrls.getCommentaryUrl() + request.getRequestURI());
    }
}
