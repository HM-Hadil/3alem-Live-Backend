package spring._3alemliveback.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring._3alemliveback.dto.formation.AvisRequest;
import spring._3alemliveback.dto.formation.FormationRequest;
import spring._3alemliveback.dto.formation.FormationResponseDTO;
import spring._3alemliveback.entities.Avis;
import spring._3alemliveback.entities.Formation;
import spring._3alemliveback.services.FormationService;

import java.util.List;

@RestController
@RequestMapping("/api/formations")
@RequiredArgsConstructor
public class FormationController {

    private final FormationService formationService;

    @PostMapping("/create")
    public ResponseEntity<Formation> createFormation(@RequestBody FormationRequest formationRequest) {
        Formation formation = formationService.createFormation(formationRequest);
        return new ResponseEntity<>(formation, HttpStatus.CREATED);
    }

    @PutMapping("/approve/{id}")
    public ResponseEntity<Formation> approveFormation(@PathVariable Long id) {
        Formation formation = formationService.approveFormation(id);
        return ResponseEntity.ok(formation);
    }

    @PutMapping("/reject/{id}")
    public ResponseEntity<Formation> rejectFormation(@PathVariable Long id) {
        Formation formation = formationService.rejectFormation(id);
        return ResponseEntity.ok(formation);
    }

    @PostMapping("/inscription/{id}")
    public ResponseEntity<Formation> inscriptionFormation(@PathVariable Long id) {
        Formation formation = formationService.inscriptionFormation(id);
        return ResponseEntity.ok(formation);
    }

    @GetMapping("/approved")
    public ResponseEntity<List<Formation>> getAllApprovedFormations() {
        List<Formation> formations = formationService.getAllApprovedFormations();
        return ResponseEntity.ok(formations);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<FormationResponseDTO>> getAllPendingFormations() {
        List<FormationResponseDTO> formations = formationService.getAllPendingFormations();
        return ResponseEntity.ok(formations);
    }

    @GetMapping("/my-formations")
    public ResponseEntity<List<Formation>> getMyFormations() {
        List<Formation> formations = formationService.getMyFormations();
        return ResponseEntity.ok(formations);
    }

    @GetMapping("/my-inscriptions")
    public ResponseEntity<List<Formation>> getMyInscriptions() {
        List<Formation> formations = formationService.getMyInscriptions();
        return ResponseEntity.ok(formations);
    }

    /**
     * Démarre une formation (change son statut à EN_COURS)
     * Accessible uniquement par le formateur de la formation
     */
    @PutMapping("/demarrer/{id}")
    public ResponseEntity<Formation> demarrerFormation(@PathVariable Long id) {
        Formation formation = formationService.demarrerFormation(id);
        return ResponseEntity.ok(formation);
    }

    /**
     * Termine une formation (change son statut à TERMINEE)
     * Accessible uniquement par le formateur de la formation
     */
    @PutMapping("/terminer/{id}")
    public ResponseEntity<Formation> terminerFormation(@PathVariable Long id) {
        Formation formation = formationService.terminerFormation(id);
        return ResponseEntity.ok(formation);
    }

    /**
     * Ajoute un avis sur une formation terminée
     * Accessible uniquement par les participants de la formation
     */
    @PostMapping("/{id}/avis")
    public ResponseEntity<Avis> ajouterAvis(
            @PathVariable Long id,
            @RequestBody AvisRequest avisRequest) {
        Avis avis = formationService.ajouterAvis(id, avisRequest);
        return new ResponseEntity<>(avis, HttpStatus.CREATED);
    }

    /**
     * Récupère tous les avis d'une formation
     * Accessible par tous
     */
    @GetMapping("/{id}/avis")
    public ResponseEntity<List<Avis>> getAvisByFormation(@PathVariable Long id) {
        List<Avis> avis = formationService.getAvisByFormation(id);
        return ResponseEntity.ok(avis);
    }
}