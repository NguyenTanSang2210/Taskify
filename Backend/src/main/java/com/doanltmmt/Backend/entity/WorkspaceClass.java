package com.doanltmmt.Backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "workspace_classes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "class_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class WorkspaceClass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "class_id", nullable = false)
    private AcademicClass academicClass;

    @Column(nullable = false)
    private boolean active = true;
}

