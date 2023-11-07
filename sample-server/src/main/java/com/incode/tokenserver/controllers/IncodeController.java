package com.incode.tokenserver.controllers;

import com.incode.tokenserver.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/")
public class IncodeController {

    private final Logger log = LoggerFactory.getLogger(IncodeController.class);

    @Value("${incode.apikey}")
    private String apiKey;

    @Value("${incode.apiurl}")
    private String apiUrl;

    @Value("${incode.configurationId}")
    private String configurationId;

    @Value("${incode.clientId}")
    private String clientId;

    private final WebClient webClient;

    public IncodeController() {
        this.webClient = WebClient.builder().build();
    }

    private Mono<OmniStartResponse> createIncodeSession() {
        String startUrl = apiUrl + "/omni/start";
        log.info("Calling /omni/start");

        return webClient.post()
                .uri(startUrl)
                .header("x-api-key", apiKey)
                .header("api-version", "1.0")
                .bodyValue(Map.of(
                        "configurationId", configurationId,
                        "countryCode", "ALL",
                        "language", "en-US"
                ))
                .retrieve()
                .bodyToMono(OmniStartResponse.class);
    }

    private Mono<FetchOnboardingUrlResponse> getOnboardingUrl(String token) {
        String url = apiUrl + "/omni/onboarding-url?clientId=" + clientId;
        log.info("Calling {}", url);

        return webClient.get()
                .uri(url)
                .header("X-Incode-Hardware-Id", token)
                .header("x-api-key", apiKey)
                .header("api-version", "1.0")
                .retrieve()
                .bodyToMono(FetchOnboardingUrlResponse.class);
    }

    @GetMapping("/start")
    public Mono<Map<String, String>> createSession() {
        return createIncodeSession().map( omniStartResponse -> {
            Map<String, String> response = Map.of(
                    "interviewId", omniStartResponse.interviewId(),
                    "token", omniStartResponse.token()
            );
            return response;
        });
    }

    @GetMapping("/onboarding-url")
    public Mono<Map<String, String>> createSessionWithRedirectUrl() {
        return createIncodeSession()
            .flatMap(omniStartResponse ->
                getOnboardingUrl(omniStartResponse.token())
                    .map(onboardingUrlResponse -> {
                        Map<String, String> response = Map.of(
                                "interviewId", omniStartResponse.interviewId(),
                                "token", omniStartResponse.token(),
                                "url", onboardingUrlResponse.url()
                        );
                        return response;
                    })
            );
    }

    @PostMapping("/webhook")
    public Mono<ResponseEntity<Map<String, Object>>> webhookAction(@RequestBody Mono<WebhookPayload> dataMono) {
        return dataMono.map(data -> {
            String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            Map<String, Object> response = Map.of(
            "timeStamp", timeStamp,
            "data", data
            );

            System.out.println(response);

            return ResponseEntity.ok(response);
        });
    }
}
