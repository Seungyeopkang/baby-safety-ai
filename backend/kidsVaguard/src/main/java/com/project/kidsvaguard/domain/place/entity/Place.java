package com.project.kidsvaguard.domain.place.entity;

import com.project.kidsvaguard.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.sql.Timestamp;

@Entity
@Table(name = "place")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Place {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long placeId;

    @Column(nullable = false, length = 100)
    private String placeName;

    //cctv 주소
    private String cctvAddress;

    @Enumerated(EnumType.STRING)
    private Action action;

    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp createdAt;

    @ManyToOne
    @JoinColumn(name = "user_pk", nullable = false)
    private User user;

    public enum Action { START, STOP }
}