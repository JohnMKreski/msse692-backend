package com.arkvalleyevents.msse692_backend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.URL;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
public class Artist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @NotBlank
    private String genre;

    @Column(length = 2000)
    private String biography;

    @URL
    private String website;

    //A many-to-many relationship in databases means that multiple records in one table can be associated with multiple records in another table
    //mappedBy indicates that the relationship is bidirectional and the ownership is on the other side (ConcertEvent)
    // mappedBy = "artists" means the ownership of the relationship is on the ConcertEvent side. The Artist entity is the inverse side,
    // and the ConcertEvent entity should have a @ManyToMany field named artists that owns the relationship.
    @ManyToMany(mappedBy = "artists")
    private Set<Event> events = new HashSet<>();
}

