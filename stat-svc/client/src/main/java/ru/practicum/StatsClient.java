package ru.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class StatsClient {
    private final RestTemplate restTemplate;

    @Value("${stat-server.url:http://stats-server:9090}")
    private String serverUrl;

    @Value("${app.name:ewm-main-service}")
    private String appName;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatsClient() {
        this.restTemplate = new RestTemplate();
    }

    public void hit(HttpServletRequest request) {
        String url = serverUrl + "/hit";

        String clientIp = getClientIp(request);
        String uri = request.getRequestURI();

        log.info("Sending hit to stats service: app={}, uri={}, ip={}", appName, uri, clientIp);

        EndpointHit endpointHit = new EndpointHit();
        endpointHit.setApp(appName);
        endpointHit.setUri(uri);
        endpointHit.setIp(clientIp);
        endpointHit.setTimestamp(LocalDateTime.now());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<EndpointHit> requestEntity = new HttpEntity<>(endpointHit, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            log.info("Stats service hit response: {} - {}", response.getStatusCode(), response.getBody());
        } catch (Exception e) {
            log.error("Failed to save hit to stats service: {}", e.getMessage());
        }
    }

    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        String url = serverUrl + "/stats";

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("start", start.format(formatter))
                .queryParam("end", end.format(formatter))
                .queryParam("unique", unique != null ? unique : false);

        if (uris != null && !uris.isEmpty()) {
            for (String uri : uris) {
                builder.queryParam("uris", uri);
            }
        }

        String finalUrl = builder.build().toUriString();
        log.info("Requesting stats from: {}", finalUrl);

        try {
            ResponseEntity<ViewStats[]> response = restTemplate.getForEntity(finalUrl, ViewStats[].class);
            ViewStats[] statsArray = response.getBody();
            log.info("Received {} stats records", statsArray != null ? statsArray.length : 0);

            if (statsArray != null) {
                for (ViewStats stat : statsArray) {
                    log.debug("Stat: app={}, uri={}, hits={}", stat.getApp(), stat.getUri(), stat.getHits());
                }
            }

            return statsArray != null ? Arrays.asList(statsArray) : List.of();
        } catch (Exception e) {
            log.error("Failed to get stats from stats service: {}", e.getMessage());
            return List.of();
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
