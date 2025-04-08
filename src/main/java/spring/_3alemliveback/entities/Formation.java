package spring._3alemliveback.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import spring._3alemliveback.enums.FormationCategory;
import spring._3alemliveback.enums.FormationStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "formations")
public class Formation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titre;

    @Column(length = 2000)
    private String description;

    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private Integer duree; // en heures
    private Integer nombreMaxParticipants;
    private Double prix;

    private String urlMeet;

    @Lob
    private byte[] imageFormation;

    @Enumerated(EnumType.STRING)
    private FormationCategory categorie;

    @Enumerated(EnumType.STRING)
    private FormationStatus statut;

    @ManyToOne
    @JoinColumn(name = "formateur_id")
    private User formateur;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "formation_participants",
            joinColumns = @JoinColumn(name = "formation_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> participants = new ArrayList<>();


}