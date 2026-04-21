package tn.esprit.rh.achat.controllers;

import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh.achat.entities.CategorieProduit;
import tn.esprit.rh.achat.services.ICategorieProduitService;

import java.util.List;

@RestController
@Api(tags = "Gestion des categories Produit")
@RequestMapping("/categorieProduit")
@RequiredArgsConstructor
public class CategorieProduitController {

    private final ICategorieProduitService categorieProduitService;

    // GET ALL
    @GetMapping("/retrieve-all")
    public List<CategorieProduit> getCategorieProduit() {
        return categorieProduitService.retrieveAllCategorieProduits();
    }

    // GET BY ID
    @GetMapping("/retrieve/{id}")
    public CategorieProduit retrieveCategorieProduit(@PathVariable Long id) {
        return categorieProduitService.retrieveCategorieProduit(id);
    }

    // ADD
    @PostMapping("/add")
    public CategorieProduit addCategorieProduit(@RequestBody CategorieProduit cp) {
        return categorieProduitService.addCategorieProduit(cp);
    }

    // DELETE
    @DeleteMapping("/remove/{id}")
    public void removeCategorieProduit(@PathVariable Long id) {
        categorieProduitService.deleteCategorieProduit(id);
    }

    // UPDATE
    @PutMapping("/modify")
    public CategorieProduit modifyCategorieProduit(@RequestBody CategorieProduit categorieProduit) {
        return categorieProduitService.updateCategorieProduit(categorieProduit);
    }
}