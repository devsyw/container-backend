package kr.osci.container.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "container_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContainerTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String image;

    @Column(nullable = false)
    private Integer port;

    private String icon;

    private String description;

    @Column(columnDefinition = "TEXT")
    private String envVariables;

    private Boolean enabled;
}
