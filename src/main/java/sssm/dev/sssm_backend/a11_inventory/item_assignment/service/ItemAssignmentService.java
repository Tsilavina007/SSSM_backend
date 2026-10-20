package sssm.dev.sssm_backend.a11_inventory.item_assignment.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
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
  private final ItemAssignmentRestMapper itemAssignmentRestMapper;

  public List<ItemAssignmentRest> getItemAssignments(
      Optional<String> search, Optional<Integer> page, Optional<Integer> pageSize) {
    int pageNumber = page.orElse(1);
    int pageSizeNumber = pageSize.orElse(10);
    return mapperListResponseToRest(
        search.isPresent()
            ? itemAssignmentDAO.searchItemAssignmentsByName(
                search.get(), pageNumber, pageSizeNumber)
            : itemAssignmentDAO.getAllItemAssignments(pageNumber, pageSizeNumber));
  }

  public Optional<ItemAssignmentRest> getItemAssignmentById(String itemAssignmentId) {
    Optional<ItemAssignmentResponse> itemAssignment =
        itemAssignmentDAO.getItemAssignmentById(itemAssignmentId);
    if (itemAssignment.isEmpty()) {
      throw new NotFoundException("Affectation d'article introuvable", "Aucune affectation d'article ne correspond à l'identifiant " + itemAssignmentId + ".", "NOT_FOUND");
    }
    return Optional.of(itemAssignmentRestMapper.toRest(itemAssignment.get()));
  }

  public ItemAssignmentRest createItemAssignment(ItemAssignmentCreateRequest itemAssignment) {
    return itemAssignmentRestMapper.toRest(itemAssignmentDAO.createItemAssignment(itemAssignment));
  }

  public ItemAssignmentRest updateItemAssignment(
      String itemAssignmentId, ItemAssignmentUpdateRequest updatedItemAssignment) {
    if (itemAssignmentDAO.getItemAssignmentById(itemAssignmentId).isEmpty()) {
      throw new NotFoundException("Affectation d'article introuvable", "Aucune affectation d'article ne correspond à l'identifiant " + itemAssignmentId + ".", "NOT_FOUND");
    }
    return itemAssignmentRestMapper.toRest(
        itemAssignmentDAO.updateItemAssignment(itemAssignmentId, updatedItemAssignment));
  }

  public void deleteItemAssignment(String itemAssignmentId) {
    itemAssignmentDAO.deleteItemAssignment(itemAssignmentId);
  }

  private List<ItemAssignmentRest> mapperListResponseToRest(List<ItemAssignmentResponse> items) {
    return items.stream()
        .map(
            item -> {
              return itemAssignmentRestMapper.toRest(item);
            })
        .toList();
  }
}
