package kr.osci.container.config;

import kr.osci.container.entity.ContainerTemplate;
import kr.osci.container.repository.ContainerTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final ContainerTemplateRepository templateRepository;

    @Override
    public void run(String... args) {
        log.info("Initializing container templates...");

        // VS Code (code-server) - 이미지명만 저장 (registry는 서비스에서 추가)
        templateRepository.save(ContainerTemplate.builder()
                .name("VS Code")
                .image("code-server:latest")
                .port(8080)
                .icon("vscode")
                .description("웹 기반 VS Code 개발 환경")
                .envVariables("{\"PASSWORD\": \"password123\"}")
                .enabled(true)
                .build());

        // Jupyter Notebook
        templateRepository.save(ContainerTemplate.builder()
                .name("Jupyter Notebook")
                .image("jupyter:latest")
                .port(8888)
                .icon("jupyter")
                .description("데이터 분석 및 Python 노트북")
                .envVariables("{\"JUPYTER_TOKEN\": \"token123\"}")
                .enabled(true)
                .build());

        // Jenkins
        templateRepository.save(ContainerTemplate.builder()
                .name("Jenkins")
                .image("jenkins/jenkins:lts")
                .port(8080)
                .icon("jenkins")
                .description("CI/CD 파이프라인 도구")
                .envVariables("{}")
                .enabled(true)
                .build());

        // Python 개발환경
        templateRepository.save(ContainerTemplate.builder()
                .name("Python Dev")
                .image("python-base:3.11-offline")
                .port(8000)
                .icon("python")
                .description("Python 개발 환경 (numpy, pandas 포함)")
                .envVariables("{}")
                .enabled(true)
                .build());

        log.info("Container templates initialized: {} templates", templateRepository.count());
    }
}