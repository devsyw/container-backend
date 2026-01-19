package kr.osci.container.controller;

import kr.osci.container.entity.ContainerInstance;
import kr.osci.container.entity.ContainerTemplate;
import kr.osci.container.service.ContainerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/containers")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class ContainerController {

    private final ContainerService containerService;

    // 사용 가능한 템플릿 목록 조회
    @GetMapping("/templates")
    public ResponseEntity<List<ContainerTemplate>> getTemplates() {
        return ResponseEntity.ok(containerService.getAvailableTemplates());
    }

    // 템플릿 추가
    @PostMapping("/templates")
    public ResponseEntity<ContainerTemplate> addTemplate(@RequestBody ContainerTemplate template) {
        return ResponseEntity.ok(containerService.addTemplate(template));
    }

    // 컨테이너 인스턴스 생성 (실행)
    @PostMapping("/instances")
    public ResponseEntity<ContainerInstance> createInstance(
            @RequestParam Long templateId,
            @RequestParam(defaultValue = "default-user") String userId) {
        return ResponseEntity.ok(containerService.createInstance(templateId, userId));
    }

    // 사용자의 인스턴스 목록 조회
    @GetMapping("/instances")
    public ResponseEntity<List<ContainerInstance>> getUserInstances(
            @RequestParam(defaultValue = "default-user") String userId) {
        return ResponseEntity.ok(containerService.getUserInstances(userId));
    }

    // 인스턴스 중지
    @DeleteMapping("/instances/{instanceId}")
    public ResponseEntity<Void> stopInstance(@PathVariable Long instanceId) {
        containerService.stopInstance(instanceId);
        return ResponseEntity.ok().build();
    }
}