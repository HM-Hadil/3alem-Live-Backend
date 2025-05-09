package spring._3alemliveback.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import spring._3alemliveback.dto.register.*;
import spring._3alemliveback.entities.User;
import spring._3alemliveback.mapper.UserMapper;
import spring._3alemliveback.services.AuthenticationService;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class AuthenticationController {
    @Autowired
    private UserMapper userMapper;
    private final AuthenticationService authenticationService;

    @PostMapping("/register/expert")
    public ResponseEntity<AuthenticationResponse> registerExpert(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authenticationService.registerExpert(request));
    }

    @PostMapping("/register/apprenant")
    public ResponseEntity<AuthenticationResponse> registerApprenant(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authenticationService.registerApprenant(request));
    }



    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody AuthenticationRequest request
    ) {
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }

    @PutMapping("/{expertId}")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> validateExpert(@PathVariable Long expertId) {
        authenticationService.validateExpertAccount(expertId);
        return ResponseEntity.ok("Le compte expert a été validé avec succès.");
    }

    /**
     * Endpoint pour récupérer la liste des experts qui ont vérifié leur email mais n'ont pas encore été validés par l'admin
     * @return Liste des experts en attente de validation
     */
    @GetMapping("/pending-experts")
    //@PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getPendingExperts() {
        List<User> pendingExperts = authenticationService.getPendingExperts();
        List<UserDto> pendingExpertDtos = userMapper.toDtoList(pendingExperts);
        return ResponseEntity.ok(pendingExpertDtos);
    }
    /**
     * Endpoint pour récupérer tous les formateurs/experts actifs
     * @return Liste de tous les experts actifs
     */
    @GetMapping("/users/experts")
    //@PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getAllExperts() {
        List<User> activeExperts = authenticationService.getAllExperts();
        List<UserDto> activeExpertDtos = userMapper.toDtoList(activeExperts);
        return ResponseEntity.ok(activeExpertDtos);
    }
    @GetMapping("/users/formateur")
    //@PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getActiveExpert() {
        List<User> activeExperts = authenticationService.getActivateExperts();
        List<UserDto> activeExpertDtos = userMapper.toDtoList(activeExperts);
        return ResponseEntity.ok(activeExpertDtos);
    }
    @GetMapping("/user/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        Optional<User> userOptional = authenticationService.getUserById(id);

        if (userOptional.isPresent()) {
            UserDto userDto = userMapper.toDto(userOptional.get());
            return ResponseEntity.ok(userDto);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/profile")
    // Only authenticated users can update their profile
    public ResponseEntity<UserDto> updateUserProfile(
            @RequestBody UserProfileUpdateRequest updateRequest,
            Principal connectedUser // Inject the authenticated user's principal
    ) {
        User updatedUser = authenticationService.updateUserProfile(connectedUser.getName(), updateRequest);
        UserDto updatedUserDto = userMapper.toDto(updatedUser);
        return ResponseEntity.ok(updatedUserDto);
    }

    @GetMapping("/user/by-email")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> getUserByEmail(Principal connectedUser) {
        String email = connectedUser.getName();
        Optional<User> userOptional = authenticationService.getUserByEmail(email);

        if (userOptional.isPresent()) {
            UserDto userDto = userMapper.toDto(userOptional.get());
            return ResponseEntity.ok(userDto);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> getUserProfile(Principal connectedUser) {
        String email = connectedUser.getName();
        Optional<User> userOptional = authenticationService.getUserByEmail(email);

        if (userOptional.isPresent()) {
            UserDto userDto = userMapper.toDto(userOptional.get());
            return ResponseEntity.ok(userDto);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verifyAccount(@RequestParam String token) {
        try {
            authenticationService.verifyAccount(token);
            return ResponseEntity.ok("<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "    <title>Vérification réussie</title>\n" +
                    "    <style>\n" +
                    "        body {\n" +
                    "            font-family: Arial, sans-serif;\n" +
                    "            max-width: 600px;\n" +
                    "            margin: 0 auto;\n" +
                    "            padding: 20px;\n" +
                    "            text-align: center;\n" +
                    "        }\n" +
                    "        .success {\n" +
                    "            color: #28a745;\n" +
                    "            font-size: 24px;\n" +
                    "            margin-bottom: 20px;\n" +
                    "        }\n" +
                    "        .button {\n" +
                    "            display: inline-block;\n" +
                    "            background-color: #007bff;\n" +
                    "            color: white;\n" +
                    "            padding: 10px 20px;\n" +
                    "            text-decoration: none;\n" +
                    "            border-radius: 5px;\n" +
                    "            margin-top: 20px;\n" +
                    "        }\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <h1 class=\"success\">Compte vérifié avec succès!</h1>\n" +
                    "    <p>Votre compte a été activé. Vous pouvez maintenant vous connecter à l'application.</p>\n" +
                    "    <a href=\"http://localhost:4200/login\" class=\"button\">Se connecter</a>\n" +
                    "</body>\n" +
                    "</html>");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("<!DOCTYPE html>\n" +
                            "<html>\n" +
                            "<head>\n" +
                            "    <title>Échec de vérification</title>\n" +
                            "    <style>\n" +
                            "        body {\n" +
                            "            font-family: Arial, sans-serif;\n" +
                            "            max-width: 600px;\n" +
                            "            margin: 0 auto;\n" +
                            "            padding: 20px;\n" +
                            "            text-align: center;\n" +
                            "        }\n" +
                            "        .error {\n" +
                            "            color: #dc3545;\n" +
                            "            font-size: 24px;\n" +
                            "            margin-bottom: 20px;\n" +
                            "        }\n" +
                            "        .error-details {\n" +
                            "            background-color: #f8d7da;\n" +
                            "            border: 1px solid #f5c6cb;\n" +
                            "            border-radius: 5px;\n" +
                            "            padding: 10px;\n" +
                            "            margin: 20px 0;\n" +
                            "        }\n" +
                            "        .button {\n" +
                            "            display: inline-block;\n" +
                            "            background-color: #007bff;\n" +
                            "            color: white;\n" +
                            "            padding: 10px 20px;\n" +
                            "            text-decoration: none;\n" +
                            "            border-radius: 5px;\n" +
                            "            margin-top: 20px;\n" +
                            "        }\n" +
                            "    </style>\n" +
                            "</head>\n" +
                            "<body>\n" +
                            "    <h1 class=\"error\">Échec de vérification</h1>\n" +
                            "    <div class=\"error-details\">\n" +
                            "        <p><strong>Erreur:</strong> " + e.getMessage() + "</p>\n" +
                            "    </div>\n" +
                            "    <p>Veuillez demander un nouveau lien de vérification ou contacter le support.</p>\n" +
                            "    <a href=\"http://localhost:4200/resend-verification\" class=\"button\">Demander un nouveau lien</a>\n" +
                            "</body>\n" +
                            "</html>");
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody String email) {
        try {
            authenticationService.resendVerificationEmail(email);
            return ResponseEntity.ok("Un nouveau lien de vérification a été envoyé à votre adresse email.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }
}