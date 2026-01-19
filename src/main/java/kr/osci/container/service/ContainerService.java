package kr.osci.container.service;

import kr.osci.container.entity.ContainerInstance;
import kr.osci.container.entity.ContainerTemplate;
import kr.osci.container.repository.ContainerInstanceRepository;
import kr.osci.container.repository.ContainerTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // 템플릿 목록 조회
    public List<ContainerTemplate> getAvailableTemplates() {
        return templateRepository.findByEnabledTrue();
    }

    // 템플릿 추가
    @Transactional
    public ContainerTemplate addTemplate(ContainerTemplate template) {
        template.setEnabled(true);
        return templateRepository.save(template);
    }

    // 컨테이너 인스턴스 생성
    @Transactional
    public ContainerInstance createInstance(Long templateId, String userId) {
        ContainerTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));

        // 랜덤 이름 및 URL 생성
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8);
        String podName = template.getName().toLowerCase().replaceAll(" ", "-") + "-" + randomSuffix;
        String accessUrl = "http://" + randomSuffix + ".localhost:8080";

        ContainerInstance instance = ContainerInstance.builder()
                .template(template)
                .userId(userId)
                .podName(podName)
                .namespace("user-containers")
                .accessUrl(accessUrl)
                .status(ContainerInstance.ContainerStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        instance = instanceRepository.save(instance);

        // TODO: 실제 K8s Pod 생성 로직 (이후 단계에서 구현)
        log.info("Container instance created: {}", podName);

        // 임시로 RUNNING 상태로 변경
        instance.setStatus(ContainerInstance.ContainerStatus.RUNNING);

        return instanceRepository.save(instance);
    }

    // 사용자의 인스턴스 목록 조회
    public List<ContainerInstance> getUserInstances(String userId) {
        return instanceRepository.findByUserId(userId);
    }

    // 인스턴스 중지
    @Transactional
    public void stopInstance(Long instanceId) {
        ContainerInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new RuntimeException("Instance not found: " + instanceId));

        // TODO: 실제 K8s Pod 삭제 로직
        log.info("Stopping container: {}", instance.getPodName());

        instance.setStatus(ContainerInstance.ContainerStatus.STOPPED);
        instance.setStoppedAt(LocalDateTime.now());
        instanceRepository.save(instance);
    }
}