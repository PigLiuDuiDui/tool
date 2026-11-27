package com.lighttoolbox.modules.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lighttoolbox.common.BusinessException;
import com.lighttoolbox.common.ErrorCode;
import com.lighttoolbox.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

/**
 * 调用 photo-worker / doc-worker。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkerClient {

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public WorkerResult callPhoto(String action, Path input, Map<String, String> params, Path outputHint) {
        return post(appProperties.getPhoto().getWorkerUrl() + "/process", action, input, params, outputHint);
    }

    public WorkerResult callDoc(String action, Path input, Map<String, String> params, Path outputHint) {
        return post(appProperties.getDoc().getWorkerUrl() + "/process", action, input, params, outputHint);
    }

    public WorkerResult callDocEstimate(String action, Path input, Map<String, String> params) {
        return post(appProperties.getDoc().getWorkerUrl() + "/estimate", action, input, params, null);
    }

    private WorkerResult post(String url, String action, Path input, Map<String, String> params, Path outputHint) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("action", action);
            body.add("file", new FileSystemResource(input));
            if (params != null) {
                params.forEach((k, v) -> {
                    if (v != null) {
                        body.add(k, v);
                    }
                });
            }
            if (outputHint != null) {
                body.add("output_path", outputHint.toString());
            }

            JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
            factory.setReadTimeout(Duration.ofMinutes(3));

            String raw = RestClient.builder()
                    .requestFactory(factory)
                    .build()
                    .post()
                    .uri(url)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode node = objectMapper.readTree(raw);
            if (node.path("ok").asBoolean(false)) {
                return new WorkerResult(
                        true,
                        node.path("output_path").asText(null),
                        node.path("output_name").asText(null),
                        node.path("output_size").asLong(0),
                        node.path("estimated_size").asLong(0),
                        null
                );
            }
            return new WorkerResult(false, null, null, 0, 0, node.path("message").asText("处理失败"));
        } catch (Exception e) {
            log.error("调用 worker 失败: {}", url, e);
            throw new BusinessException(ErrorCode.INTERNAL, "处理服务暂不可用：" + e.getMessage());
        }
    }

    public record WorkerResult(boolean ok, String outputPath, String outputName, long outputSize,
                               long estimatedSize, String message) {
    }
}
