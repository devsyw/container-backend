package kr.osci.container.repository;

import kr.osci.container.entity.ContainerInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContainerInstanceRepository extends JpaRepository<ContainerInstance, Long> {
    List<ContainerInstance> findByUserId(String userId);
    List<ContainerInstance> findByUserIdAndStatus(String userId, ContainerInstance.ContainerStatus status);
}