package spring._3alemliveback.mapper;

import org.springframework.stereotype.Component;
import spring._3alemliveback.dto.register.UserDto;
import spring._3alemliveback.entities.User;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    /**
     * Convert User entity to UserDto
     */
    public UserDto toDto(User user) {
        if (user == null) {
            return null;
        }

        return UserDto.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .isActive(user.isActive())
                .isVerified(user.isVerified())
                .profileDescription(user.getProfileDescription())
                .profileImage(user.getProfileImage())
                .domaines(user.getDomaines())
                .certifications(user.getCertifications())
                .niveauEtude(user.getNiveauEtude())
                .experience(user.getExperience())
                .linkedinUrl(user.getLinkedinUrl())
                .portfolioUrl(user.getPortfolioUrl())
                .cvPdf(user.getCvPdf())
                .build();
    }
    /**
     * Convert Optional<User> to UserDto
     */
    public UserDto toDtoOptional(Optional<User> userOptional) {
        return userOptional.map(this::toDto).orElse(null);
    }


    /**
     * Convert a list of User entities to a list of UserDtos
     */
    public List<UserDto> toDtoList(List<User> users) {
        if (users == null) {
            return null;
        }

        return users.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}