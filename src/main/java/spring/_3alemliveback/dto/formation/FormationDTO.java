package spring._3alemliveback.dto.formation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import spring._3alemliveback.entities.Avis;
import spring._3alemliveback.entities.Formation;
import spring._3alemliveback.entities.User;
import spring._3alemliveback.enums.FormationCategory;
import spring._3alemliveback.enums.FormationStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormationDTO {
    private Long id;
    private String titre;
    private String description;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private Integer duree;
    private Integer nombreMaxParticipants;
    private Double prix;
    private String urlMeet;
    private String imageFormation; // Stockée comme chaîne Base64
    private FormationCategory categorie;
    private FormationStatus statut;
    private FormateurDTO formateur;
    private List<AvisDTO> avis;
    private int nombreParticipants;
    private List<ParticipantDTO> participantsDetails; // Optionnel, utiliser seulement si nécessaire

    /**
     * Convertit une entité Formation en FormationDTO
     * @param formation L'entité Formation à convertir
     * @return Le DTO correspondant
     */
    public static FormationDTO fromEntity(Formation formation) {
        if (formation == null) {
            return null;
        }

        FormationDTOBuilder builder = FormationDTO.builder()
                .id(formation.getId())
                .titre(formation.getTitre())
                .description(formation.getDescription())
                .dateDebut(formation.getDateDebut())
                .dateFin(formation.getDateFin())
                .duree(formation.getDuree())
                .nombreMaxParticipants(formation.getNombreMaxParticipants())
                .prix(formation.getPrix())
                .urlMeet(formation.getUrlMeet())
                .categorie(formation.getCategorie())
                .statut(formation.getStatut())
                .nombreParticipants(formation.getParticipants() != null ? formation.getParticipants().size() : 0);

        // Conversion de l'image en Base64 (si elle existe)
        if (formation.getImageFormation() != null && formation.getImageFormation().length > 0) {
            builder.imageFormation(Base64.getEncoder().encodeToString(formation.getImageFormation()));
        }

        // Conversion du formateur
        if (formation.getFormateur() != null) {
            builder.formateur(FormateurDTO.fromEntity(formation.getFormateur()));
        }

        // Conversion des avis
        if (formation.getAvis() != null && !formation.getAvis().isEmpty()) {
            builder.avis(formation.getAvis().stream()
                    .map(AvisDTO::fromEntity)
                    .collect(Collectors.toList()));
        } else {
            builder.avis(new ArrayList<>());
        }

        // Si nécessaire, ajouter la liste des participants (utiliser avec précaution)
        /*
        if (formation.getParticipants() != null && !formation.getParticipants().isEmpty()) {
            builder.participantsDetails(formation.getParticipants().stream()
                    .map(ParticipantDTO::fromEntity)
                    .collect(Collectors.toList()));
        }
        */

        return builder.build();
    }

    /**
     * Convertit une liste d'entités Formation en liste de FormationDTO
     * @param formations Liste d'entités Formation à convertir
     * @return Liste de DTOs correspondants
     */
    public static List<FormationDTO> fromEntities(List<Formation> formations) {
        if (formations == null) {
            return new ArrayList<>();
        }
        return formations.stream()
                .map(FormationDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * DTO pour représenter un formateur de manière simplifiée
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FormateurDTO {
        private Long id;
        private String nom;
        private String prenom;
        private String email;
        private String phone;
        private String profileDescription;
        private String imageProfile; // Encodée en Base64
        private List<String> domaines;
        private List<String> certifications;
        private String niveauEtude;
        private String experience;
        private String linkedinUrl;
        private String portfolioUrl;

        public static FormateurDTO fromEntity(User user) {
            if (user == null) {
                return null;
            }

            FormateurDTOBuilder builder = FormateurDTO.builder()
                    .id(user.getId())
                    .nom(user.getNom())
                    .prenom(user.getPrenom())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .profileDescription(user.getProfileDescription())
                    .domaines(user.getDomaines())
                    .certifications(user.getCertifications())
                    .niveauEtude(user.getNiveauEtude())
                    .experience(user.getExperience())
                    .linkedinUrl(user.getLinkedinUrl())
                    .portfolioUrl(user.getPortfolioUrl());

            // Conversion de l'image de profil en Base64 (si elle existe)
            if (user.getProfileImage() != null && user.getProfileImage().length > 0) {
                builder.imageProfile(Base64.getEncoder().encodeToString(user.getProfileImage()));
            }

            return builder.build();
        }
    }

    /**
     * DTO pour représenter un avis de manière simplifiée
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AvisDTO {
        private Long id;
        private String commentaire;
        private Integer note;
        private LocalDateTime dateCreation;
        private Long utilisateurId;
        private String utilisateurNom;
        private String utilisateurPrenom;
        private String imageProfile; // Encodée en Base64, optionnelle

        public static AvisDTO fromEntity(Avis avis) {
            if (avis == null) {
                return null;
            }

            AvisDTOBuilder builder = AvisDTO.builder()
                    .id(avis.getId())
                    .commentaire(avis.getCommentaire())
                    .note(avis.getNote())
                    .dateCreation(avis.getDateCreation());

            if (avis.getUtilisateur() != null) {
                builder.utilisateurId(avis.getUtilisateur().getId())
                        .utilisateurNom(avis.getUtilisateur().getNom())
                        .utilisateurPrenom(avis.getUtilisateur().getPrenom());

                // Optionnellement, ajouter l'image de profil
                if (avis.getUtilisateur().getProfileImage() != null &&
                        avis.getUtilisateur().getProfileImage().length > 0) {
                    builder.imageProfile(Base64.getEncoder().encodeToString(
                            avis.getUtilisateur().getProfileImage()));
                }
            }

            return builder.build();
        }
    }

    /**
     * DTO pour représenter un participant de manière simplifiée
     * Utilisé seulement lorsque nécessaire (attention à la performance)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantDTO {
        private Long id;
        private String nom;
        private String prenom;
        private String email;
        private String imageProfile; // Encodée en Base64

        public static ParticipantDTO fromEntity(User user) {
            if (user == null) {
                return null;
            }

            ParticipantDTOBuilder builder = ParticipantDTO.builder()
                    .id(user.getId())
                    .nom(user.getNom())
                    .prenom(user.getPrenom())
                    .email(user.getEmail());

            // Conversion de l'image de profil en Base64 (si elle existe)
            if (user.getProfileImage() != null && user.getProfileImage().length > 0) {
                builder.imageProfile(Base64.getEncoder().encodeToString(user.getProfileImage()));
            }

            return builder.build();
        }
    }
}