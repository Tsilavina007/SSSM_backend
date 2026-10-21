package sssm.dev.sssm_backend.a11_inventory.item_assignment.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a01_common.enums.ItemStatus;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemResponse;
import sssm.dev.sssm_backend.a11_inventory.item.repository.ItemDAO;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.ItemAssignmentCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.ItemAssignmentResponse;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.ItemAssignmentUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.mapper.ItemAssignmentRestMapper;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.rest.ItemAssignmentRest;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.repository.ItemAssignmentDAO;

@Service
@RequiredArgsConstructor
public class ItemAssignmentService {
  private final ItemAssignmentDAO itemAssignmentDAO;
  private final ItemDAO itemDAO;
  private final ItemAssignmentRestMapper itemAssignmentRestMapper;

  public List<ItemAssignmentRest> getItemAssignments(
      Optional<String> search, Optional<Integer> page, Optional<Integer> pageSize) {
    int pageNumber = page.orElse(1);
    int pageSizeNumber = pageSize.orElse(10);
    List<ItemAssignmentResponse> assignments =
        search
            .map(
                value ->
                    itemAssignmentDAO.searchItemAssignmentsByName(
                        value, pageNumber, pageSizeNumber))
            .orElseGet(() -> itemAssignmentDAO.getAllItemAssignments(pageNumber, pageSizeNumber));
    return assignments.stream().map(itemAssignmentRestMapper::toRest).toList();
  }

  public List<ItemAssignmentRest> getItemAssignmentsByItem(
      String itemId, int page, int pageSize) {
    return itemAssignmentDAO.getItemAssignmentsByItem(itemId, page, pageSize).stream()
        .map(itemAssignmentRestMapper::toRest)
        .toList();
  }

  public ItemAssignmentRest getItemAssignmentById(String itemAssignmentId) {
    return itemAssignmentDAO
        .getItemAssignmentById(itemAssignmentId)
        .map(itemAssignmentRestMapper::toRest)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Affectation d'article introuvable",
                    "Aucune affectation d'article ne correspond à l'identifiant "
                        + itemAssignmentId
                        + ".",
                    "NOT_FOUND"));
  }

  @Transactional
  public ItemAssignmentRest createItemAssignment(ItemAssignmentCreateRequest itemAssignment) {
    // Vérifier que l'item existe et est disponible
    ItemResponse item =
        itemDAO
            .getItemById(itemAssignment.getItem_id())
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Article introuvable",
                        "Aucun article ne correspond à l'identifiant "
                            + itemAssignment.getItem_id()
                            + ".",
                        "NOT_FOUND"));
    if (item.getStatus() != ItemStatus.AVAILABLE) {
      throw new sssm.dev.sssm_backend.a00_infrastructure.exception.ClientException(
          "Article non disponible",
          "L'article "
              + itemAssignment.getItem_id()
              + " n'est pas disponible pour une affectation (statut: "
              + item.getStatus()
              + ").",
          "CLIENT_ERROR");
    }

    ItemAssignmentRest result =
        itemAssignmentRestMapper.toRest(itemAssignmentDAO.createItemAssignment(itemAssignment));

    // Mettre à jour l'état de l'item
    itemDAO.updateItemStatus(itemAssignment.getItem_id(), ItemStatus.ASSIGNED);
    itemDAO.updateItemLocation(itemAssignment.getItem_id(), null);

    return result;
  }

  @Transactional
  public ItemAssignmentRest updateItemAssignment(
      String itemAssignmentId, ItemAssignmentUpdateRequest updatedItemAssignment) {
    ItemAssignmentResponse existing =
        itemAssignmentDAO
            .getItemAssignmentById(itemAssignmentId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Affectation d'article introuvable",
                        "Aucune affectation d'article ne correspond à l'identifiant "
                            + itemAssignmentId
                            + ".",
                        "NOT_FOUND"));

    ItemAssignmentRest result =
        itemAssignmentRestMapper.toRest(
            itemAssignmentDAO.updateItemAssignment(itemAssignmentId, updatedItemAssignment));

    // Si la restitution est enregistrée, remettre l'item en AVAILABLE
    if (updatedItemAssignment.getReturned_at() != null) {
      itemDAO.updateItemStatus(existing.getItem_id(), ItemStatus.AVAILABLE);
    }

    return result;
  }

  @Transactional
  public void deleteItemAssignment(String itemAssignmentId) {
    itemAssignmentDAO.deleteItemAssignment(itemAssignmentId);
  }
}
