package spring._3alemliveback.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import spring._3alemliveback.dto.register.AuthenticationRequest;
import spring._3alemliveback.dto.register.AuthenticationResponse;
import spring._3alemliveback.dto.register.RegisterRequest;
import spring._3alemliveback.dto.register.UserDto;
import spring._3alemliveback.entities.User;
import spring._3alemliveback.mapper.UserMapper;
import spring._3alemliveback.services.AuthenticationService;

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
    @GetMapping("/verify")
    public ResponseEntity<String> verifyAccount(@RequestParam String token) {
        authenticationService.verifyAccount(token);
        return ResponseEntity.ok("Votre compte a été vérifié avec succès !");
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
    @GetMapping("/user/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        Optional<User> userOptional = authenticationService.getUserById(id);

        if (userOptional.isPresent()) {
            UserDto userDto = userMapper.toDto(userOptional.get());
            return ResponseEntity.ok(userDto);
        } else {
            return ResponseEntity.notFound().build();
        }}
}
