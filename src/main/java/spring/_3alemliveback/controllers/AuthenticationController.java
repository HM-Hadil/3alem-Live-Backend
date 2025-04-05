package spring._3alemliveback.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import spring._3alemliveback.dto.register.AuthenticationRequest;
import spring._3alemliveback.dto.register.AuthenticationResponse;
import spring._3alemliveback.dto.register.RegisterRequest;
import spring._3alemliveback.services.AuthenticationService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

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

    @PutMapping("/validate-expert/{expertId}")
   // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> validateExpert(@PathVariable Long expertId) {
        authenticationService.validateExpertAccount(expertId);
        return ResponseEntity.ok("Le compte expert a été validé avec succès.");
    }
}
