package com.doanltmmt.Backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "students")
public class Student {

    @Id
    private Long id;  // trùng id user

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private User user;

    private String studentCode;
    private String className;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "class_id")
    private AcademicClass academicClass;
}
