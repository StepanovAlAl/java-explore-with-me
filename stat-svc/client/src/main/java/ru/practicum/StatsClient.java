package ru.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
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

@Slf4j
@Component
public class StatsClient {
    private final RestTemplate restTemplate;

    @Value("${stat-server.url:http://localhost:9090}")
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
            log.info("Successfully saved hit for URI: {}, IP: {}, App: {}",
                    request.getRequestURI(), getClientIp(request), appName);
        } catch (HttpStatusCodeException e) {
            log.error("Failed to save hit to stats service: {}", e.getStatusCode());
        } catch (Exception e) {
            log.error("Failed to save hit to stats service: {}", e.getMessage());
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
            log.info("Successfully retrieved stats for URIs: {}, found {} records",
                    uris, statsArray != null ? statsArray.length : 0);
            return statsArray != null ? Arrays.asList(statsArray) : List.of();
        } catch (HttpStatusCodeException e) {
            log.error("Failed to get stats from stats service: {}", e.getStatusCode());
            return List.of();
        } catch (Exception e) {
            log.error("Failed to get stats from stats service: {}", e.getMessage());
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