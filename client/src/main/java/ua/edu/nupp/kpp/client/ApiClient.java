package ua.edu.nupp.kpp.client;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public final class ApiClient {
    private final String base = System.getenv().getOrDefault("APPROVAL_API_URL",
            "http://localhost:8080/approval-server").replaceAll("/+$", "");
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private String encode(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    static final class RejectedRow extends IOException {
        RejectedRow(String reason) { super(reason); }
    }

    private JsonNode send(HttpRequest request) throws IOException, InterruptedException {
        var response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (!response.headers().firstValue("Content-Type").orElse("").startsWith("application/json"))
            throw new IOException("HTTP " + response.statusCode() + ". Перевірте адресу WAR у Tomcat.");
        JsonNode body = json.readTree(response.body());
        if (response.statusCode() == 400)
            throw new RejectedRow(body.path("error").asText("Сервер відхилив дані"));
        if (response.statusCode() >= 400)
            throw new IOException(body.path("error").asText("Помилка HTTP " + response.statusCode()));
        return body;
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(base + "/" + path)).timeout(Duration.ofSeconds(15));
    }

    public <T> List<T> list(String route, Class<T> type) throws Exception {
        JsonNode rows = send(request(route).GET().build());
        return json.convertValue(rows, json.getTypeFactory().constructCollectionType(List.class, type));
    }

    public void post(String route, Map<String,String> fields) throws Exception {
        // Кодуємо кожне значення окремо: у назві документа може бути навіть знак &.
        String form = fields.entrySet().stream().map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
        send(request(route).header("Content-Type","application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build());
    }

    public void addDocument(ua.edu.nupp.kpp.client.model.Document doc) throws Exception {
        post("documents", Map.of("action","add","registrationNumber",doc.registrationNumber(),
                "title",doc.title(),"authorId",String.valueOf(doc.authorId()),
                "criticalityScore",String.valueOf(doc.criticalityScore()),
                "dependentProcessCount",String.valueOf(doc.dependentProcessCount())));
    }

    public record Score(int f1, int f2, int f3, double index) {}
    public Score priority(int id, String day) throws Exception {
        return json.treeToValue(send(request("requests?action=priority&id=" + id + "&onDate=" + encode(day))
                .GET().build()), Score.class);
    }
}
