package ru.practicum.statsservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.statsservice.dto.EndpointHit;
import ru.practicum.statsservice.dto.ViewStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsClientImpl implements StatsClient {

    private final RestTemplate restTemplate;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${stats-service.url:http://localhost:9090}")
    private String statsServerUrl;

    @Override
    public void saveHit(EndpointHit hit) {
        log.info("Sending hit to stats service: {}", hit);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<EndpointHit> requestEntity = new HttpEntity<>(hit, headers);

        try {
            restTemplate.postForEntity(statsServerUrl + "/hit", requestEntity, Void.class);
            log.debug("Hit sent successfully");
        } catch (Exception e) {
            log.error("Failed to send hit to stats service: {}", e.getMessage(), e);
        }
    }

    @Override
    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        log.info("Getting stats from {} to {}, uris={}, unique={}", start, end, uris, unique);

        String url = UriComponentsBuilder.fromHttpUrl(statsServerUrl + "/stats")
                .queryParam("start", start.format(FORMATTER))
                .queryParam("end", end.format(FORMATTER))
                .queryParam("uris", uris != null ? String.join(",", uris) : null)
                .queryParam("unique", unique)
                .toUriString();

        ResponseEntity<List<ViewStats>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        log.debug("Received {} stats records", response.getBody() != null ? response.getBody().size() : 0);
        return response.getBody();
    }
}