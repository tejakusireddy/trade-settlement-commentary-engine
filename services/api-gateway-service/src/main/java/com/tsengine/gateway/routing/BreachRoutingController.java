package com.tsengine.gateway.routing;

import com.tsengine.gateway.config.ServiceUrlsProperties;
import com.tsengine.gateway.infrastructure.DownstreamHttpClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/breaches")
public class BreachRoutingController {

    private final DownstreamHttpClient downstreamHttpClient;
    private final ServiceUrlsProperties serviceUrls;

    public BreachRoutingController(DownstreamHttpClient downstreamHttpClient, ServiceUrlsProperties serviceUrls) {
        this.downstreamHttpClient = downstreamHttpClient;
        this.serviceUrls = serviceUrls;
    }

    @GetMapping
    public ResponseEntity<String> getBreaches(HttpServletRequest request) {
        return downstreamHttpClient.forward(request, serviceUrls.getBreachDetectorUrl() + request.getRequestURI());
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> getBreachById(@PathVariable String id, HttpServletRequest request) {
        return downstreamHttpClient.forward(request, serviceUrls.getBreachDetectorUrl() + request.getRequestURI());
    }
}
