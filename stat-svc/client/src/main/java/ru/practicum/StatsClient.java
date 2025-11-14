package ru.practicum;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class StatsClient {
    private final RestTemplate restTemplate;

    @Value("${stats.server.url:http://localhost:9090}")
    private String serverUrl;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatsClient() {
        this.restTemplate = new RestTemplate();
    }

    public void hit(HttpServletRequest request, String appName) {
        String url = serverUrl + "/hit";

        EndpointHit endpointHit = new EndpointHit();
        endpointHit.setApp(appName);
        endpointHit.setUri(request.getRequestURI());
        endpointHit.setIp(getClientIp(request));
        endpointHit.setTimestamp(LocalDateTime.now());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<EndpointHit> requestEntity = new HttpEntity<>(endpointHit, headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Void.class);
            System.out.printf("[DEBUG] Successfully saved hit for URI: %s%n", request.getRequestURI());
        } catch (HttpStatusCodeException e) {
            System.out.printf("[WARN] Failed to save hit to stats service: %s%n", e.getStatusCode());
        } catch (Exception e) {
            System.out.printf("[WARN] Failed to save hit to stats service: %s%n", e.getMessage());
        }
    }

    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        String url = serverUrl + "/stats";

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("start", encodeValue(start.format(formatter)))
                .queryParam("end", encodeValue(end.format(formatter)))
                .queryParam("unique", unique);

        if (uris != null && !uris.isEmpty()) {
            builder.queryParam("uris", String.join(",", uris));
        }

        try {
            ResponseEntity<ViewStats[]> response = restTemplate.getForEntity(
                    builder.toUriString(), ViewStats[].class);

            ViewStats[] statsArray = response.getBody();
            System.out.printf("[DEBUG] Successfully retrieved stats, found %d records%n",
                    statsArray != null ? statsArray.length : 0);
            return statsArray != null ? Arrays.asList(statsArray) : List.of();
        } catch (HttpStatusCodeException e) {
            System.out.printf("[WARN] Failed to get stats from stats service: %s%n", e.getStatusCode());
            return List.of();
        } catch (Exception e) {
            System.out.printf("[WARN] Failed to get stats from stats service: %s%n", e.getMessage());
            return List.of();
        }
    }

    private String encodeValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
