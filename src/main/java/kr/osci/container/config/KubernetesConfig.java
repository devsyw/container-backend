package kr.osci.container.config;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.util.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileReader;
import java.io.IOException;

@Configuration
public class KubernetesConfig {

    @Value("${kubernetes.config.path:#{null}}")
    private String kubeConfigPath;

    @Bean
    public ApiClient apiClient() throws IOException {
        // 클러스터 내부에서 자동으로 ServiceAccount 인증 사용
        ApiClient client = Config.defaultClient();
        io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client);
        return client;
    }

    @Bean
    public CoreV1Api coreV1Api(ApiClient apiClient) {
        return new CoreV1Api(apiClient);
    }

    @Bean
    public AppsV1Api appsV1Api(ApiClient apiClient) {
        return new AppsV1Api(apiClient);
    }

    @Bean
    public NetworkingV1Api networkingV1Api(ApiClient apiClient) {
        return new NetworkingV1Api(apiClient);
    }
}