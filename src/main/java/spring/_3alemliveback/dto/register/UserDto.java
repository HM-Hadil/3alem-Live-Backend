package spring._3alemliveback.dto.register;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import spring._3alemliveback.enums.Role;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String phone;
    private Role role;
    private boolean isActive;
    private boolean isVerified;
}
