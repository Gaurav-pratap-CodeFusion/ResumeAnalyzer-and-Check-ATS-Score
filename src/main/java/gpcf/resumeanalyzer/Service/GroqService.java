package gpcf.resumeanalyzer.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class GroqService {

    private final WebClient webClient;

    @Value("${groq.api.key}")
    private String apiKey;

    public GroqService(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    public String chat(String message, String model) {

        if (message.length() > 12000) {
            message = message.substring(0, 12000);
        }

        Map<String, Object> request = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", message
                        )
                )
        );

        return webClient.post()
                .uri("https://api.groq.com/openai/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .map(err -> new RuntimeException("Groq API error: " + err))
                )
                .bodyToMono(Map.class)
                .map(res -> {
                    var choices = (List<Map<String, Object>>) res.get("choices");
                    var msg = (Map<String, Object>) choices.get(0).get("message");
                    return msg.get("content").toString();
                })
                .block();
    }
}
