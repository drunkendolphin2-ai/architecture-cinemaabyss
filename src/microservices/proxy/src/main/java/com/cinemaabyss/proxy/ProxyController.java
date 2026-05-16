package com.cinemaabyss.proxy;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.ThreadLocalRandom;

@RestController
public class ProxyController {

    private static final Logger log = LoggerFactory.getLogger(ProxyController.class);

    private final WebClient webClient;

    @Value("${MONOLITH_URL:http://monolith:8080}")
    private String monolithUrl;

    @Value("${MOVIES_SERVICE_URL:http://movies-service:8081}")
    private String moviesServiceUrl;

    @Value("${EVENTS_SERVICE_URL:http://events-service:8082}")
    private String eventsServiceUrl;

    @Value("${GRADUAL_MIGRATION:true}")
    private boolean gradualMigration;

    @Value("${MOVIES_MIGRATION_PERCENT:50}")
    private int moviesMigrationPercent;

    public ProxyController(WebClient webClient) {
        this.webClient = webClient;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Strangler Fig Proxy is healthy");
    }

    @RequestMapping("/**")
    public ResponseEntity<byte[]> proxy(HttpServletRequest request,
                                        @org.springframework.web.bind.annotation.RequestBody(required = false) byte[] body) {
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String fullPath = query == null ? path : path + "?" + query;

        String targetBase = route(path);
        String targetUrl = targetBase + fullPath;

        log.info("Proxying {} {} -> {}", request.getMethod(), fullPath, targetUrl);

        HttpMethod method = HttpMethod.valueOf(request.getMethod());

        WebClient.RequestBodySpec spec = webClient
                .method(method)
                .uri(targetUrl);

        // Прокидываем заголовки кроме hop-by-hop
        java.util.Collections.list(request.getHeaderNames()).forEach(name -> {
            if (!isHopByHop(name)) {
                spec.header(name, request.getHeader(name));
            }
        });

        WebClient.RequestHeadersSpec<?> finalSpec = (body != null && body.length > 0)
                ? spec.bodyValue(body)
                : spec;

        return finalSpec
                .retrieve()
                .toEntity(byte[].class)
                .block();
    }

    /**
     * Strangler Fig routing.
     * - /api/movies* — между monolith и movies-service по проценту
     * - /api/events* — в events-service
     * - всё остальное (/api/users, /api/payments, /api/subscriptions, /health монолита) — в monolith
     */
    private String route(String path) {
        if (path.startsWith("/api/events")) {
            return eventsServiceUrl;
        }
        if (path.startsWith("/api/movies")) {
            return shouldRouteToMoviesService() ? moviesServiceUrl : monolithUrl;
        }
        return monolithUrl;
    }

    private boolean shouldRouteToMoviesService() {
        if (!gradualMigration) {
            return false;
        }
        int roll = ThreadLocalRandom.current().nextInt(100);
        boolean toMovies = roll < moviesMigrationPercent;
        log.debug("Migration roll: {} < {} = {}", roll, moviesMigrationPercent, toMovies);
        return toMovies;
    }

    private boolean isHopByHop(String name) {
        String lower = name.toLowerCase();
        return lower.equals("host")
                || lower.equals("connection")
                || lower.equals("content-length")
                || lower.equals("transfer-encoding");
    }
}