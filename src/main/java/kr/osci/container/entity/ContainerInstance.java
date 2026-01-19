package kr.osci.container.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "container_instances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContainerInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ContainerTemplate template;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String podName;

    @Column(nullable = false)
    private String namespace;

    private String accessUrl;

    @Enumerated(EnumType.STRING)
    private ContainerStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime stoppedAt;

    public enum ContainerStatus {
        PENDING, RUNNING, STOPPED, FAILED
    }
}