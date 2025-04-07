package spring._3alemliveback.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring._3alemliveback.dto.formation.FormationRequest;
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
    public ResponseEntity<List<Formation>> getAllPendingFormations() {
        List<Formation> formations = formationService.getAllPendingFormations();
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
}