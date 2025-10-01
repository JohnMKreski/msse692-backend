package com.arkvalleyevents.msse692_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
public class Artist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String genre;
    private String biography;
    private String website;

    @ManyToMany(mappedBy = "artists")
    private Set<ConcertEvent> concertEvents = new HashSet<>();
}
