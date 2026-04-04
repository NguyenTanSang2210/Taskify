package com.doanltmmt.Backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "privileges")
@Getter
@Setter
@NoArgsConstructor
public class Privilege {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name; // e.g., USER_VIEW, USER_CREATE, TOPIC_APPROVE

    @Column(length = 255)
    private String description;

    public Privilege(String name) {
        this.name = name;
    }

    public Privilege(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
