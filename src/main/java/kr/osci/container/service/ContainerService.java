package kr.osci.container.service;

import kr.osci.container.entity.ContainerInstance;
import kr.osci.container.entity.ContainerTemplate;
import kr.osci.container.repository.ContainerInstanceRepository;
import kr.osci.container.repository.ContainerTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContainerService {

    private final ContainerTemplateRepository templateRepository;
    private final ContainerInstanceRepository instanceRepository;

    // Optional: K8s 비활성화 시 null
    @Autowired(required = false)
    private KubernetesService kubernetesService;

    @Value("${kubernetes.enabled:false}")
    private boolean kubernetesEnabled;

    @Value("${kubernetes.domain:localhost}")
    private String domain;

    @Value("${kubernetes.ingress.port:8080}")
    private int ingressPort;

    public List<ContainerTemplate> getAvailableTemplates() {
        return templateRepository.findByEnabledTrue();
    }

    @Transactional
    public ContainerTemplate addTemplate(ContainerTemplate template) {
        template.setEnabled(true);
        return templateRepository.save(template);
    }

    @Transactional
    public ContainerInstance createInstance(Long templateId, String userId) {
        ContainerTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));

        String randomSuffix = UUID.randomUUID().toString().substring(0, 8);
        String podName = template.getName().toLowerCase().replaceAll(" ", "-") + "-" + randomSuffix;
        String accessUrl = "http://" + randomSuffix + "." + domain + ":" + ingressPort;

        ContainerInstance instance = ContainerInstance.builder()
                .template(template)
                .userId(userId)
                .podName(podName)
                .namespace("user-containers")
                .accessUrl(accessUrl)
                .status(ContainerInstance.ContainerStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        // K8s 활성화 시에만 실제 Pod 생성
        if (kubernetesEnabled && kubernetesService != null) {
            kubernetesService.createContainer(template, userId, podName, randomSuffix);
            log.info("Kubernetes Pod created: {}", podName);
            instance.setStatus(ContainerInstance.ContainerStatus.PENDING);
        } else {
            log.info("Kubernetes disabled - Mock instance created: {}", podName);
            instance.setStatus(ContainerInstance.ContainerStatus.RUNNING);
        }

        return instanceRepository.save(instance);
    }

    public List<ContainerInstance> getUserInstances(String userId) {
        // RUNNING 또는 PENDING 상태만 반환 (STOPPED 제외)
        return instanceRepository.findByUserIdAndStatusIn(userId,
                List.of(ContainerInstance.ContainerStatus.RUNNING, ContainerInstance.ContainerStatus.PENDING));
    }

    @Transactional
    public void stopInstance(Long instanceId) {
        ContainerInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new RuntimeException("Instance not found: " + instanceId));

        // K8s 활성화 시에만 실제 Pod 삭제
        if (kubernetesEnabled && kubernetesService != null) {
            kubernetesService.deleteContainer(instance.getPodName());
            log.info("Kubernetes Pod deleted: {}", instance.getPodName());
        } else {
            log.info("Kubernetes disabled - Mock instance stopped: {}", instance.getPodName());
        }

        instance.setStatus(ContainerInstance.ContainerStatus.STOPPED);
        instance.setStoppedAt(LocalDateTime.now());
        instanceRepository.save(instance);
    }

    // Pod 상태 확인 (신규)
    public String getInstanceStatus(Long instanceId) {
        ContainerInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new RuntimeException("Instance not found: " + instanceId));

        if (kubernetesEnabled && kubernetesService != null) {
            String podStatus = kubernetesService.getPodStatus(instance.getPodName());

            // Pod 상태에 따라 Instance 상태 업데이트
            if ("Running".equals(podStatus)) {
                if (instance.getStatus() != ContainerInstance.ContainerStatus.RUNNING) {
                    instance.setStatus(ContainerInstance.ContainerStatus.RUNNING);
                    instanceRepository.save(instance);
                }
                return "READY";
            } else if ("Pending".equals(podStatus)) {
                return "PENDING";
            } else {
                return podStatus;
            }
        }

        return instance.getStatus().name();
    }
}