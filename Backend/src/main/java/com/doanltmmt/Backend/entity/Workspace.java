package com.doanltmmt.Backend.entity;

import com.doanltmmt.Backend.entity.enums.WorkspaceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "workspaces")
@Getter
@Setter
@NoArgsConstructor
public class Workspace {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 50)
    private String type;

    @Column(length = 50)
    private String semester;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // cột varchar (không dùng kiểu ENUM của MySQL) để thêm giá trị mới không phải ALTER
    @Column(nullable = false, length = 30)
    private WorkspaceStatus status = WorkspaceStatus.DRAFT;

    @Column(nullable = false)
    private boolean active = true;
}
