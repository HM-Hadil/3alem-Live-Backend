package spring._3alemliveback.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    // Si tu veux filtrer certaines propriétés de User mais garder la relation
    @JsonIgnoreProperties({"formations", "inscriptions", "password", "authorities",
            "accountNonExpired", "accountNonLocked", "credentialsNonExpired"})
    private User formateur;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "formation_participants",
            joinColumns = @JoinColumn(name = "formation_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    // On ignore complètement cette collection lors de la sérialisation
    @JsonIgnore
    private List<User> participants = new ArrayList<>();

    @OneToMany(mappedBy = "formation", cascade = CascadeType.ALL, orphanRemoval = true)
    // On ignore la référence retour vers formation dans chaque avis
    @JsonIgnoreProperties("formation")
    private List<Avis> avis = new ArrayList<>();
}