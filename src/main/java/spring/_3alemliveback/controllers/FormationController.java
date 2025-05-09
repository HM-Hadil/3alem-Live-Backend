package spring._3alemliveback.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring._3alemliveback.dto.formation.AvisRequest;
import spring._3alemliveback.dto.formation.FormationDTO;
import spring._3alemliveback.dto.formation.FormationRequest;
import spring._3alemliveback.dto.formation.FormationResponseDTO;
import spring._3alemliveback.entities.Avis;
import spring._3alemliveback.entities.Formation;
import spring._3alemliveback.services.FormationService;

import java.util.List;

@RestController
@RequestMapping("/api/formations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FormationController {

    private final FormationService formationService;

    // Pour les requêtes POST/PUT, l'email peut être un @RequestParam
    // ou inclus dans le corps de la requête (nécessiterait d'ajuster les DTOs ou d'utiliser un wrapper DTO)
    // Nous utilisons @RequestParam pour la cohérence et la simplicité ici.

    @PostMapping
    public ResponseEntity<Formation> createFormation(@RequestBody FormationRequest formationRequest, @RequestParam String userEmail) {
        Formation formation = formationService.createFormation(formationRequest, userEmail);
        return new ResponseEntity<>(formation, HttpStatus.CREATED);
    }

    @PutMapping("/approve/{id}")
    public ResponseEntity<Formation> approveFormation(@PathVariable Long id /*, @RequestParam(required = false) String adminEmail */) {
        // Si l'email de l'admin est nécessaire pour la logique métier au-delà de l'autorisation de rôle :
        // Formation formation = formationService.approveFormation(id, adminEmail);
        Formation formation = formationService.approveFormation(id);
        return ResponseEntity.ok(formation);
    }

    @PutMapping("/reject/{id}")
    public ResponseEntity<Formation> rejectFormation(@PathVariable Long id /*, @RequestParam(required = false) String adminEmail */) {
        Formation formation = formationService.rejectFormation(id);
        return ResponseEntity.ok(formation);
    }

    @PostMapping("/inscription/{id}")
    public ResponseEntity<Formation> inscriptionFormation(@PathVariable Long id, @RequestParam String userEmail) {
        Formation formation = formationService.inscriptionFormation(id, userEmail);
        return ResponseEntity.ok(formation);
    }

    @GetMapping("/approved")
    public ResponseEntity<List<FormationDTO>> getAllApprovedFormations() {
        List<FormationDTO> formations = formationService.getAllApprovedFormations();
        return ResponseEntity.ok(formations);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<FormationResponseDTO>> getAllPendingFormations() {
        List<FormationResponseDTO> formations = formationService.getAllPendingFormations();
        return ResponseEntity.ok(formations);
    }

    @GetMapping("/my-formations")
    public ResponseEntity<List<FormationDTO>> getMyFormations(@RequestParam String userEmail) {
        List<FormationDTO> formations = formationService.getMyFormations(userEmail);
        return ResponseEntity.ok(formations);
    }
    @GetMapping("/my-inscriptions")
    public ResponseEntity<List<FormationDTO>> getMyInscriptions(@RequestParam String userEmail) {
        List<FormationDTO> formations = formationService.getMyInscriptions(userEmail);
        return ResponseEntity.ok(formations);
    }

    @PutMapping("/demarrer/{id}")
    public ResponseEntity<Formation> demarrerFormation(@PathVariable Long id, @RequestParam String userEmail) {
        Formation formation = formationService.demarrerFormation(id, userEmail);
        return ResponseEntity.ok(formation);
    }

    @PutMapping("/terminer/{id}")
    public ResponseEntity<Formation> terminerFormation(@PathVariable Long id, @RequestParam String userEmail) {
        Formation formation = formationService.terminerFormation(id, userEmail);
        return ResponseEntity.ok(formation);
    }

    @PostMapping("/{id}/avis")
    public ResponseEntity<Avis> ajouterAvis(
            @PathVariable Long id,
            @RequestBody AvisRequest avisRequest,
            @RequestParam String userEmail) {
        Avis avis = formationService.ajouterAvis(id, avisRequest, userEmail);
        return new ResponseEntity<>(avis, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/avis")
    public ResponseEntity<List<Avis>> getAvisByFormation(@PathVariable Long id) {
        List<Avis> avis = formationService.getAvisByFormation(id);
        return ResponseEntity.ok(avis);
    }
    @GetMapping("/{id}") // Map GET requests to /api/formations/{id}
    public ResponseEntity<FormationDTO> getFormationById(@PathVariable Long id) { // Get the ID from the path variable
        FormationDTO formation = formationService.getFormationById(id); // Call the new service method
        return ResponseEntity.ok(formation); // Return the DTO with OK status
    }
    @DeleteMapping("/{id}") // Map DELETE requests to /api/formations/{id}
    public ResponseEntity<Void> deleteFormation(@PathVariable Long id, @RequestParam String userEmail) {
        formationService.deleteFormation(id, userEmail); // Call the service method
        return ResponseEntity.noContent().build(); // Return 204 No Content on success
    }

    // **NEW ENDPOINT: Update Formation**
    @PutMapping("/{id}") // Map PUT requests to /api/formations/{id} for update
    public ResponseEntity<FormationDTO> updateFormation(@PathVariable Long id,
                                                        @RequestBody FormationRequest updatedFormationRequest,
                                                        @RequestParam String userEmail) {
        FormationDTO updatedFormation = formationService.updateFormation(id, updatedFormationRequest, userEmail); // Call the service method
        return ResponseEntity.ok(updatedFormation); // Return the updated DTO with OK status
    }

}