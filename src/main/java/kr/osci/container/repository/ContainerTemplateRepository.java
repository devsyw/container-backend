package kr.osci.container.repository;

import kr.osci.container.entity.ContainerTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContainerTemplateRepository extends JpaRepository<ContainerTemplate, Long> {
    List<ContainerTemplate> findByEnabledTrue();
}