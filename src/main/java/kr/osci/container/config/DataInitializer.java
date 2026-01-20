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

        // VS Code (code-server)
        templateRepository.save(ContainerTemplate.builder()
                .name("VS Code")
                .image("code-server:latest")
                .port(8080)
                .icon("vscode")
                .description("웹 기반 VS Code 개발 환경")
                .envVariables("{}")
                .enabled(true)
                .build());

        // Jupyter Notebook
        templateRepository.save(ContainerTemplate.builder()
                .name("Jupyter Notebook")
                .image("jupyter:latest")
                .port(8888)
                .icon("jupyter")
                .description("데이터 분석 및 Python 노트북")
                .envVariables("{}")
                .enabled(true)
                .build());

        // Streamlit WebApp
        templateRepository.save(ContainerTemplate.builder()
                .name("Streamlit")
                .image("streamlit-base:latest")
                .port(8501)
                .icon("streamlit")
                .description("Python 웹앱 대시보드 (데이터 시각화)")
                .envVariables("{}")
                .enabled(true)
                .build());

        log.info("Container templates initialized: {} templates", templateRepository.count());
    }
}