package tn.esprit.rh.achat.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.rh.achat.entities.*;
import tn.esprit.rh.achat.repositories.*;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class FactureServiceImpl implements IFactureService {

    private final FactureRepository factureRepository;
    private final OperateurRepository operateurRepository;
    private final DetailFactureRepository detailFactureRepository;
    private final FournisseurRepository fournisseurRepository;
    private final ProduitRepository produitRepository;
    private final ReglementServiceImpl reglementService;

    @Override
    public List<Facture> retrieveAllFactures() {
        List<Facture> factures = factureRepository.findAll();
        factures.forEach(f -> log.info("facture : {}", f));
        return factures;
    }

    @Override
    public Facture addFacture(Facture f) {
        return factureRepository.save(f);
    }

    /**
     * Calcul des montants d'une facture
     */
    private Facture addDetailsFacture(Facture f, Set<DetailFacture> detailsFacture) {
        float montantFacture = 0;
        float montantRemise = 0;

        for (DetailFacture detail : detailsFacture) {

            Produit produit = produitRepository.findById(
                    detail.getProduit().getIdProduit()
            ).orElseThrow(() -> new EntityNotFoundException("Produit not found"));

            float prixTotal = detail.getQteCommandee() * produit.getPrix();
            float remise = (prixTotal * detail.getPourcentageRemise()) / 100;
            float totalApresRemise = prixTotal - remise;

            detail.setMontantRemise(remise);
            detail.setPrixTotalDetail(totalApresRemise);

            montantFacture += totalApresRemise;
            montantRemise += remise;

            detailFactureRepository.save(detail);
        }

        f.setMontantFacture(montantFacture);
        f.setMontantRemise(montantRemise);

        return f;
    }

    @Override
    public void cancelFacture(Long factureId) {
        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new EntityNotFoundException("Facture not found"));

        facture.setArchivee(true);
        factureRepository.save(facture);

        // Optionnel (JPQL)
        factureRepository.updateFacture(factureId);
    }

    @Override
    public Facture retrieveFacture(Long factureId) {
        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new EntityNotFoundException("Facture not found"));

        log.info("facture : {}", facture);
        return facture;
    }

    @Override
    public List<Facture> getFacturesByFournisseur(Long idFournisseur) {
        Fournisseur fournisseur = fournisseurRepository.findById(idFournisseur)
                .orElseThrow(() -> new EntityNotFoundException("Fournisseur not found"));

        return (List<Facture>) fournisseur.getFactures();
    }

    @Override
    public void assignOperateurToFacture(Long idOperateur, Long idFacture) {

        Facture facture = factureRepository.findById(idFacture)
                .orElseThrow(() -> new EntityNotFoundException("Facture not found"));

        Operateur operateur = operateurRepository.findById(idOperateur)
                .orElseThrow(() -> new EntityNotFoundException("Operateur not found"));

        operateur.getFactures().add(facture);
        operateurRepository.save(operateur);
    }

    @Override
    public float pourcentageRecouvrement(Date startDate, Date endDate) {

        float totalFactures = factureRepository
                .getTotalFacturesEntreDeuxDates(startDate, endDate);

        float totalRecouvrement = reglementService
                .getChiffreAffaireEntreDeuxDate(startDate, endDate);

        if (totalFactures == 0) {
            return 0; // éviter division par zéro
        }

        return (totalRecouvrement / totalFactures) * 100;
    }
}