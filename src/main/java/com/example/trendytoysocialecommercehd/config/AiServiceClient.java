package com.example.trendytoysocialecommercehd.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 调用独立 Python ai-service 的 HTTP 客户端
 * Python服务负责：智谱ChatGLM对话、RAG增强、混合推荐算法
 * Java侧若调用失败，会回退到本地逻辑（FAQ/简单推荐），保证可用性
 */
@Slf4j
@Component
public class AiServiceClient {

    @Value("${ai.service.base-url:http://localhost:8089}")
    private String baseUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * POST 请求并返回 data 字段
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> postForData(String path, Object body) throws Exception {
        String json = mapper.writeValueAsString(body);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("ai-service HTTP " + resp.statusCode() + ": " + resp.body());
        }
        JsonNode root = mapper.readTree(resp.body());
        if (root.path("code").asInt() != 200) {
            throw new RuntimeException("ai-service 业务错误: " + root.path("message").asText());
        }
        JsonNode data = root.get("data");
        if (data == null || data.isNull()) {
            return null;
        }
        return mapper.treeToValue(data, Map.class);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> postForList(String path, Object body) throws Exception {
        String json = mapper.writeValueAsString(body);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("ai-service HTTP " + resp.statusCode() + ": " + resp.body());
        }
        JsonNode root = mapper.readTree(resp.body());
        if (root.path("code").asInt() != 200) {
            throw new RuntimeException("ai-service 业务错误: " + root.path("message").asText());
        }
        JsonNode data = root.get("data");
        if (data == null || !data.isArray()) {
            return List.of();
        }
        return mapper.treeToValue(data, List.class);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getForList(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("ai-service HTTP " + resp.statusCode() + ": " + resp.body());
        }
        JsonNode root = mapper.readTree(resp.body());
        if (root.path("code").asInt() != 200) {
            throw new RuntimeException("ai-service 业务错误: " + root.path("message").asText());
        }
        JsonNode data = root.get("data");
        if (data == null || !data.isArray()) {
            return List.of();
        }
        return mapper.treeToValue(data, List.class);
    }

    @SuppressWarnings("unchecked")
    private List<String> getForStringList(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("ai-service HTTP " + resp.statusCode() + ": " + resp.body());
        }
        JsonNode root = mapper.readTree(resp.body());
        if (root.path("code").asInt() != 200) {
            throw new RuntimeException("ai-service 业务错误: " + root.path("message").asText());
        }
        JsonNode data = root.get("data");
        if (data == null || !data.isArray()) {
            return List.of();
        }
        return mapper.treeToValue(data, List.class);
    }

    /**
     * 智能客服对话
     */
    public Map<String, Object> chat(String userId, String sessionId, String message) throws Exception {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("userId", userId);
        body.put("sessionId", sessionId);
        body.put("message", message);
        log.info("[AiServiceClient] chat 转发 body = {}", mapper.writeValueAsString(body));
        return postForData("/ai/customer-service/chat", body);
    }

    /**
     * 获取会话历史
     */
    public List<Map<String, Object>> getChatHistory(String userId, String sessionId) throws Exception {
        String path = "/ai/customer-service/history?userId=" + userId + "&sessionId=" + sessionId;
        // 复用getForList
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("ai-service HTTP " + resp.statusCode());
        }
        JsonNode root = mapper.readTree(resp.body());
        JsonNode data = root.get("data");
        if (data == null || !data.isArray()) return List.of();
        return mapper.treeToValue(data, List.class);
    }

    /**
     * 获取会话列表
     */
    public List<String> getSessions(String userId) throws Exception {
        return getForStringList("/ai/customer-service/sessions?userId=" + userId);
    }

    /**
     * 个性化推荐
     */
    public List<Map<String, Object>> personalizedRecommend(String userId, int limit) throws Exception {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("userId", userId);
        body.put("limit", limit);
        return postForList("/ai/recommend/personalized", body);
    }

    /**
     * 相似推荐
     */
    public List<Map<String, Object>> similarRecommend(String seriesId, int limit) throws Exception {
        return getForList("/ai/recommend/similar/" + seriesId + "?limit=" + limit);
    }

    /**
     * 热门推荐
     */
    public List<Map<String, Object>> hotRecommend(int limit) throws Exception {
        return getForList("/ai/recommend/hot?limit=" + limit);
    }

    /**
     * 上报用户行为
     */
    public void reportBehavior(String userId, String behaviorType, String targetType, String targetId, int weight) {
        try {
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("userId", userId);
            body.put("behaviorType", behaviorType);
            body.put("targetType", targetType);
            body.put("targetId", targetId);
            body.put("weight", weight);
            String json = mapper.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/ai/behavior"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            log.warn("上报行为失败(可忽略): {}", e.getMessage());
        }
    }

    /**
     * 健康检查
     */
    public boolean isAvailable() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/health"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
