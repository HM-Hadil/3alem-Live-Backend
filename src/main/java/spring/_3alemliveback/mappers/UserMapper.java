package spring._3alemliveback.mappers;

import org.springframework.stereotype.Component;
import spring._3alemliveback.dto.register.UserDto;
import spring._3alemliveback.entities.User;


@Component
public class UserMapper {

    public UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .isActive(user.isActive())
                .isVerified(user.isVerified())
                .certifications(user.getCertifications())
                .domaines(user.getDomaines())
                .profileDescription(user.getProfileDescription())
                .profileImage(user.getProfileImage())
                .niveauEtude(user.getNiveauEtude())
                .experience(user.getExperience())
                .linkedinUrl(user.getLinkedinUrl())
                .portfolioUrl(user.getPortfolioUrl())
                .cvPdf(user.getCvPdf())


                .build();
    }
}