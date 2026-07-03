package ru.practicum.statsserver.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "hits")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndpointHitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String app;

    @Column(nullable = false)
    private String uri;

    @Column(name = "ip", nullable = false)
    private String ip;

    @Column(name = "hit_timestamp", nullable = false)
    private LocalDateTime timestamp;
}