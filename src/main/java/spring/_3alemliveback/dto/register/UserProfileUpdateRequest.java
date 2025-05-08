// spring._3alemliveback.dto.register.UserProfileUpdateRequest.java
package spring._3alemliveback.dto.register;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateRequest {
    // Include fields the user is allowed to update
    private String nom;
    private String prenom;
    private String phone;
    private String profileDescription;
    private String niveauEtude;
    private String experience;
    private String linkedinUrl;
    private String portfolioUrl;

    // Base64 strings for file updates
    private String profileImage; // Frontend sends Base64 string (without prefix)
    private String cvPdf;        // Frontend sends Base64 string (without prefix)

    // List of certification text strings
    private List<String> certifications;

    // List of domain text strings
    private List<String> domaines;

    // Exclude sensitive fields like email, password, role, active, verified, tokens, formations
}