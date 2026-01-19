package kr.osci.container.service;

import kr.osci.container.entity.ContainerInstance;
import kr.osci.container.entity.ContainerTemplate;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.openapi.models.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "kubernetes.enabled", havingValue = "true")
public class KubernetesService {

    private final CoreV1Api coreV1Api;
    private final AppsV1Api appsV1Api;
    private final NetworkingV1Api networkingV1Api;

    @Value("${kubernetes.namespace:user-containers}")
    private String namespace;

    @Value("${kubernetes.registry:192.168.2.2:32000}")
    private String registry;

    @Value("${kubernetes.domain:192.168.2.2.nip.io}")
    private String domain;

    @Value("${kubernetes.ingress.port:30080}")
    private int ingressPort;

    /**
     * 사용자별 컨테이너 Pod, Service, Ingress 생성
     */
    public ContainerInstance createContainer(ContainerTemplate template, String userId, String podName, String randomSuffix) {
        try {
            String appLabel = podName;
            String imagePath = registry + "/" + template.getImage();
            String host = randomSuffix + "." + domain;
            String accessUrl = "http://" + host + ":" + ingressPort;

            // 1. Deployment 생성
            createDeployment(appLabel, imagePath, template.getPort(), template.getEnvVariables());

            // 2. Service 생성
            createService(appLabel, template.getPort());

            // 3. Ingress 생성
            createIngress(appLabel, host, template.getPort());

            log.info("Created container: {} with URL: {}", podName, accessUrl);

            return ContainerInstance.builder()
                    .podName(podName)
                    .namespace(namespace)
                    .accessUrl(accessUrl)
                    .status(ContainerInstance.ContainerStatus.RUNNING)
                    .build();

        } catch (ApiException e) {
            log.error("Failed to create container: {}", e.getResponseBody(), e);
            throw new RuntimeException("Kubernetes API error: " + e.getResponseBody());
        }
    }

    /**
     * Deployment 생성
     */
    private void createDeployment(String name, String image, int port, String envJson) throws ApiException {
        Map<String, String> labels = new HashMap<>();
        labels.put("app", name);
        labels.put("managed-by", "container-platform");

        // 환경변수 파싱
        List<V1EnvVar> envVars = parseEnvVariables(envJson);

        // 컨테이너 생성
        V1Container container = new V1Container()
                .name(name)
                .image(image)
                .ports(List.of(new V1ContainerPort().containerPort(port)))
                .env(envVars)
                .resources(new V1ResourceRequirements()
                        .requests(Map.of(
                                "memory", new io.kubernetes.client.custom.Quantity("256Mi"),
                                "cpu", new io.kubernetes.client.custom.Quantity("100m")
                        ))
                        .limits(Map.of(
                                "memory", new io.kubernetes.client.custom.Quantity("1Gi"),
                                "cpu", new io.kubernetes.client.custom.Quantity("500m")
                        ))
                );

        // 이미지별 인증 비활성화 설정
        if (image.contains("code-server")) {
            // code-server: 패스워드 비활성화
            container.args(List.of("--auth", "none", "--bind-addr", "0.0.0.0:" + port));
        } else if (image.contains("jupyter")) {
            // Jupyter: 토큰 비활성화
            container.args(List.of(
                    "jupyter", "notebook",
                    "--ip=0.0.0.0",
                    "--port=" + port,
                    "--no-browser",
                    "--allow-root",
                    "--NotebookApp.token=''",
                    "--NotebookApp.password=''"
            ));
        }

        V1Deployment deployment = new V1Deployment()
                .metadata(new V1ObjectMeta().name(name).namespace(namespace).labels(labels))
                .spec(new V1DeploymentSpec()
                        .replicas(1)
                        .selector(new V1LabelSelector().matchLabels(labels))
                        .template(new V1PodTemplateSpec()
                                .metadata(new V1ObjectMeta().labels(labels))
                                .spec(new V1PodSpec()
                                        .containers(List.of(container))
                                )
                        )
                );

        appsV1Api.createNamespacedDeployment(namespace, deployment, null, null, null, null);
        log.info("Deployment created: {}", name);
    }

    /**
     * Service 생성
     */
    private void createService(String name, int port) throws ApiException {
        Map<String, String> labels = new HashMap<>();
        labels.put("app", name);

        V1Service service = new V1Service()
                .metadata(new V1ObjectMeta().name(name).namespace(namespace).labels(labels))
                .spec(new V1ServiceSpec()
                        .selector(labels)
                        .ports(List.of(
                                new V1ServicePort()
                                        .port(port)
                                        .targetPort(new io.kubernetes.client.custom.IntOrString(port))
                        ))
                );

        coreV1Api.createNamespacedService(namespace, service, null, null, null, null);
        log.info("Service created: {}", name);
    }

    /**
     * Ingress 생성
     */
    private void createIngress(String name, String host, int port) throws ApiException {
        Map<String, String> labels = new HashMap<>();
        labels.put("app", name);

        V1Ingress ingress = new V1Ingress()
                .metadata(new V1ObjectMeta()
                        .name(name)
                        .namespace(namespace)
                        .labels(labels)
                        .annotations(Map.of(
                                "nginx.ingress.kubernetes.io/proxy-read-timeout", "3600",
                                "nginx.ingress.kubernetes.io/proxy-send-timeout", "3600"
                        ))
                )
                .spec(new V1IngressSpec()
                        .ingressClassName("nginx")
                        .rules(List.of(
                                new V1IngressRule()
                                        .host(host)
                                        .http(new V1HTTPIngressRuleValue()
                                                .paths(List.of(
                                                        new V1HTTPIngressPath()
                                                                .path("/")
                                                                .pathType("Prefix")
                                                                .backend(new V1IngressBackend()
                                                                        .service(new V1IngressServiceBackend()
                                                                                .name(name)
                                                                                .port(new V1ServiceBackendPort().number(port))
                                                                        )
                                                                )
                                                ))
                                        )
                        ))
                );

        networkingV1Api.createNamespacedIngress(namespace, ingress, null, null, null, null);
        log.info("Ingress created: {} with host: {}", name, host);
    }

    /**
     * 컨테이너 삭제 (Deployment, Service, Ingress)
     */
    public void deleteContainer(String podName) {
        try {
            // Ingress 삭제
            try {
                networkingV1Api.deleteNamespacedIngress(podName, namespace, null, null, null, null, null, null);
                log.info("Ingress deleted: {}", podName);
            } catch (ApiException e) {
                log.warn("Ingress not found or already deleted: {}", podName);
            }

            // Service 삭제
            try {
                coreV1Api.deleteNamespacedService(podName, namespace, null, null, null, null, null, null);
                log.info("Service deleted: {}", podName);
            } catch (ApiException e) {
                log.warn("Service not found or already deleted: {}", podName);
            }

            // Deployment 삭제
            try {
                appsV1Api.deleteNamespacedDeployment(podName, namespace, null, null, null, null, null, null);
                log.info("Deployment deleted: {}", podName);
            } catch (ApiException e) {
                log.warn("Deployment not found or already deleted: {}", podName);
            }

        } catch (Exception e) {
            log.error("Failed to delete container: {}", podName, e);
            throw new RuntimeException("Failed to delete container: " + podName);
        }
    }

    /**
     * Pod 상태 확인
     */
    public String getPodStatus(String podName) {
        try {
            V1PodList podList = coreV1Api.listNamespacedPod(
                    namespace, null, null, null, null,
                    "app=" + podName, null, null, null, null, null);

            if (podList.getItems().isEmpty()) {
                return "NOT_FOUND";
            }

            V1Pod pod = podList.getItems().get(0);
            if (pod.getStatus() != null && pod.getStatus().getPhase() != null) {
                return pod.getStatus().getPhase();
            }
            return "UNKNOWN";

        } catch (ApiException e) {
            log.error("Failed to get pod status: {}", podName, e);
            return "ERROR";
        }
    }

    /**
     * 환경변수 JSON 파싱
     */
    private List<V1EnvVar> parseEnvVariables(String envJson) {
        if (envJson == null || envJson.isEmpty() || envJson.equals("{}")) {
            return List.of();
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, String> envMap = mapper.readValue(envJson, Map.class);

            return envMap.entrySet().stream()
                    .map(entry -> new V1EnvVar().name(entry.getKey()).value(entry.getValue()))
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to parse env variables: {}", envJson, e);
            return List.of();
        }
    }
}