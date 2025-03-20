package com.prashanthvangari.email_writer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prashanthvangari.email_writer.dto.EmailRequestDTO;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Data
@Service
public class EmailGeneratorService {

    private final WebClient webClient;

    public EmailGeneratorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;
    public String generateEmailReply(EmailRequestDTO emailRequestDTO) {
        //Build the prompt
        String prompt = buildPrompt(emailRequestDTO);
        Map<String,Object> requestBody = Map.of(
                "contents",new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text",prompt)
                        })
                }
        );

        String response = webClient
                                .post()
                                .uri(geminiApiUrl+geminiApiKey)
                                .header("Content-Type", "application/json")
                                .body(BodyInserters.fromValue(requestBody))
                                .retrieve()
                                .bodyToMono(String.class)
                .block();
        return extactResponseContent(response);

    }

    private String extactResponseContent(String response) {
        try{
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response);
            return rootNode
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text")
                    .asText();
        }
        catch(Exception e){
            return "Error processing response "+e.getMessage();
        }
    }

    private String buildPrompt(EmailRequestDTO emailRequestDTO) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a proffsional email reply for the following email content. Please don't generate a subject line");
        //craft a request
        if(emailRequestDTO.getTone() !=null && !emailRequestDTO.getTone().isEmpty()) {
            prompt.append(" Use a ").append(emailRequestDTO.getTone()).append(" tone.");
        }
        prompt.append("\n Orignal email content:\n").append(emailRequestDTO.getEmailContent());
        return prompt.toString();
    }
}
