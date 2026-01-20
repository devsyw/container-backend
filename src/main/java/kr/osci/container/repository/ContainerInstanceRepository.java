package kr.osci.container.repository;

import kr.osci.container.entity.ContainerInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContainerInstanceRepository extends JpaRepository<ContainerInstance, Long> {
    List<ContainerInstance> findByUserId(String userId);
    List<ContainerInstance> findByUserIdAndStatusIn(String userId, List<ContainerInstance.ContainerStatus> statuses);
}