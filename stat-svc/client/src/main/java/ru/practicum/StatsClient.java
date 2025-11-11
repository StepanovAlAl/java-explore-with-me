package ru.practicum;

import org.springframework.http.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class StatsClient {
    private final RestTemplate restTemplate;
    private final String serverUrl;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatsClient(String serverUrl) {
        this.restTemplate = new RestTemplate();
        this.serverUrl = serverUrl;
    }

    public void hit(EndpointHit endpointHit) {
        String url = serverUrl + "/hit";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<EndpointHit> requestEntity = new HttpEntity<>(endpointHit, headers);

        try {
            restTemplate.exchange(url, HttpMethod.POST, requestEntity, Void.class);
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Failed to save hit: " + e.getStatusCode(), e);
        }
    }

    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        String url = serverUrl + "/stats";

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("start", start.format(formatter))
                .queryParam("end", end.format(formatter))
                .queryParam("unique", unique);

        if (uris != null && !uris.isEmpty()) {
            for (String uri : uris) {
                builder.queryParam("uris", uri);
            }
        }

        try {
            ResponseEntity<ViewStats[]> response = restTemplate.getForEntity(
                    builder.toUriString(), ViewStats[].class);

            ViewStats[] statsArray = response.getBody();
            return statsArray != null ? Arrays.asList(statsArray) : List.of();
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Failed to get stats: " + e.getStatusCode(), e);
        }
    }
}