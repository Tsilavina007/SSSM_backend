package sssm.dev.sssm_backend.a11_inventory.item_assignment.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sssm.dev.sssm_backend.a00_infrastructure.exception.ClientException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ConflictException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a01_common.enums.ItemStatus;
import sssm.dev.sssm_backend.a03_authentication.auth.security.CurrentUser;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemResponse;
import sssm.dev.sssm_backend.a11_inventory.item.repository.ItemDAO;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.ItemAssignmentCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.ItemAssignmentResponse;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.ItemAssignmentUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.mapper.ItemAssignmentRestMapper;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.rest.ItemAssignmentRest;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.repository.ItemAssignmentDAO;
import sssm.dev.sssm_backend.a11_inventory.stock_balance.service.StockBalanceService;

@Service
@RequiredArgsConstructor
public class ItemAssignmentService {
  private static final String DEFAULT_ARCHIVE_REASON = "Archivage logique demandé";

  private final ItemAssignmentDAO itemAssignmentDAO;
  private final ItemAssignmentRestMapper itemAssignmentRestMapper;
  private final ItemDAO itemDAO;
  private final StockBalanceService stockBalanceService;
  private final CurrentUser currentUser;

  public List<ItemAssignmentRest> getItemAssignments(
      Optional<String> search,
      Optional<Integer> page,
      Optional<Integer> pageSize,
      boolean activeOnly) {
    int pageNumber = Math.max(1, page.orElse(1));
    int pageSizeNumber = Math.min(500, Math.max(1, pageSize.orElse(10)));
    List<ItemAssignmentResponse> values =
        search
            .filter(value -> value != null && !value.isBlank())
            .map(
                value ->
                    itemAssignmentDAO.searchItemAssignmentsByName(
                        value, pageNumber, pageSizeNumber, activeOnly))
            .orElseGet(
                () ->
                    itemAssignmentDAO.getAllItemAssignments(
                        pageNumber, pageSizeNumber, activeOnly));
    return values.stream().map(itemAssignmentRestMapper::toRest).toList();
  }

  public List<ItemAssignmentRest> getItemAssignmentsByItem(String itemId, int page, int pageSize) {
    return itemAssignmentDAO.getItemAssignmentsByItem(itemId, page, pageSize).stream()
        .map(itemAssignmentRestMapper::toRest)
        .toList();
  }

  public List<ItemAssignmentRest> getItemAssignmentsByEmployee(
      String employeeId, int page, int pageSize) {
    return itemAssignmentDAO.getItemAssignmentsByEmployee(employeeId, page, pageSize).stream()
        .map(itemAssignmentRestMapper::toRest)
        .toList();
  }

  public List<ItemAssignmentRest> getItemAssignmentsByPost(String postId, int page, int pageSize) {
    return itemAssignmentDAO.getItemAssignmentsByPost(postId, page, pageSize).stream()
        .map(itemAssignmentRestMapper::toRest)
        .toList();
  }

  public ItemAssignmentRest getItemAssignmentById(String itemAssignmentId) {
    return itemAssignmentRestMapper.toRest(requireAssignment(itemAssignmentId));
  }

  @Transactional
  public ItemAssignmentRest createItemAssignment(ItemAssignmentCreateRequest value) {
    ItemResponse item = requireItem(value.getItem_id());
    if (item.getArchived_at() != null) {
      throw new ConflictException(
          "Article archivé",
          "L'article " + value.getItem_id() + " est archivé et ne peut pas être affecté.",
          "ITEM_ARCHIVED");
    }

    boolean historical = value.getReturned_at() != null;
    if (historical) {
      validateHistoricalDates(value);
    } else {
      requireAvailableForAssignment(item);
    }

    ItemAssignmentResponse created =
        itemAssignmentDAO.createItemAssignment(
            value,
            item.getCompany_id(),
            historical ? null : item.getCurrent_warehouse_id(),
            currentUserId());

    if (!historical) {
      itemDAO.updateItemStatus(item.getId(), ItemStatus.ASSIGNED);
      itemDAO.updateItemLocation(item.getId(), null);
      ItemResponse after = requireItem(item.getId());
      stockBalanceService.syncItem(item, after);
    }

    return itemAssignmentRestMapper.toRest(created);
  }

  /**
   * Crée l'affectation liée à un mouvement OUT. Le mouvement reste responsable de l'état de
   * l'article et du solde dans sa propre transaction ; cette méthode ne fait que persister le lien
   * d'affectation.
   */
  @Transactional
  public ItemAssignmentResponse createItemAssignmentForMovement(
      String itemId,
      String employeeId,
      String postId,
      String sourceWarehouseId,
      String assignedBy) {
    if (isBlank(employeeId) || isBlank(postId)) {
      throw new ClientException(
          "Employé et poste obligatoires",
          "Une sortie avec affectation doit préciser employee_id et post_id.",
          "CLIENT_ERROR");
    }

    ItemResponse item = requireItem(itemId);
    if (item.getArchived_at() != null) {
      throw new ConflictException(
          "Article archivé",
          "L'article " + itemId + " est archivé et ne peut pas être affecté.",
          "ITEM_ARCHIVED");
    }
    requireAvailableForAssignment(item);

    ItemAssignmentCreateRequest request = new ItemAssignmentCreateRequest();
    request.setItem_id(itemId);
    request.setEmployee_id(employeeId);
    request.setPost_id(postId);
    request.setAssigned_at(Timestamp.from(Instant.now()));

    return itemAssignmentDAO.createItemAssignment(
        request, item.getCompany_id(), sourceWarehouseId, assignedBy);
  }

  @Transactional
  public ItemAssignmentRest returnItemAssignment(
      String itemAssignmentId, ItemAssignmentUpdateRequest value) {
    ItemAssignmentResponse assignment = requireAssignment(itemAssignmentId);
    requireNotVoided(assignment);
    if (assignment.getReturned_at() != null) {
      throw new ConflictException(
          "Affectation déjà retournée",
          "L'affectation " + itemAssignmentId + " est déjà clôturée.",
          "ASSIGNMENT_ALREADY_RETURNED");
    }

    ItemResponse item = requireItem(assignment.getItem_id());
    if (item.getStatus() != ItemStatus.ASSIGNED && item.getStatus() != ItemStatus.AVAILABLE) {
      throw new ConflictException(
          "État d'article incompatible",
          "L'article est dans l'état "
              + item.getStatus()
              + " et ne peut pas être retourné depuis une affectation.",
          "ITEM_STATE_INCOMPATIBLE");
    }

    Timestamp returnedAt = Timestamp.from(Instant.now());
    if (assignment.getAssigned_at() != null && returnedAt.before(assignment.getAssigned_at())) {
      returnedAt = assignment.getAssigned_at();
    }
    int updated =
        itemAssignmentDAO.markReturned(
            itemAssignmentId,
            returnedAt,
            currentUserId(),
            value == null ? null : value.getCondition_on_return());
    if (updated == 0) {
      throw new ConflictException(
          "Affectation déjà clôturée",
          "L'affectation " + itemAssignmentId + " a été clôturée par une autre opération.",
          "ASSIGNMENT_ALREADY_RETURNED");
    }

    String restoreWarehouse = assignment.getSource_warehouse_id();
    if (restoreWarehouse == null) {
      restoreWarehouse = item.getCurrent_warehouse_id();
    }
    itemDAO.updateItemStatus(item.getId(), ItemStatus.AVAILABLE);
    itemDAO.updateItemLocation(item.getId(), restoreWarehouse);
    ItemResponse after = requireItem(item.getId());
    stockBalanceService.syncItem(item, after);

    return itemAssignmentRestMapper.toRest(requireAssignment(itemAssignmentId));
  }

  @Transactional
  public ItemAssignmentRest updateItemAssignment(
      String itemAssignmentId, ItemAssignmentUpdateRequest value) {
    ItemAssignmentResponse existing = requireAssignment(itemAssignmentId);
    requireNotVoided(existing);
    if (value == null) {
      return itemAssignmentRestMapper.toRest(existing);
    }

    if (value.getItem_id() != null
        || value.getEmployee_id() != null
        || value.getPost_id() != null
        || value.getAssigned_at() != null
        || value.getCondition_on_assignment() != null) {
      throw new ClientException(
          "Liens d'affectation immuables",
          "item_id, employee_id, post_id, assigned_at et condition_on_assignment ne peuvent pas "
              + "être modifiés. Utilisez POST /{id}/return pour clôturer l'affectation.",
          "IMMUTABLE_ASSIGNMENT_FIELDS");
    }

    if (value.getReturned_at() != null) {
      return returnItemAssignment(itemAssignmentId, value);
    }

    if (value.getCondition_on_return() != null) {
      throw new ClientException(
          "Retour via la route dédiée",
          "condition_on_return ne peut être renseignée que par POST /{id}/return.",
          "RETURN_ENDPOINT_REQUIRED");
    }

    if (value.getExpected_return_at() != null
        && existing.getAssigned_at() != null
        && value.getExpected_return_at().before(existing.getAssigned_at())) {
      throw new ClientException(
          "Date de retour prévue invalide",
          "expected_return_at doit être postérieure ou égale à assigned_at.",
          "CLIENT_ERROR");
    }

    ItemAssignmentResponse updated =
        itemAssignmentDAO.updateItemAssignmentMetadata(itemAssignmentId, value);
    return itemAssignmentRestMapper.toRest(updated);
  }

  @Transactional
  public ItemAssignmentRest voidItemAssignment(String itemAssignmentId, String reason) {
    String voidReason = requireReason(reason);

    ItemAssignmentResponse assignment = requireAssignment(itemAssignmentId);
    if (assignment.getVoided_at() != null) {
      throw new ConflictException(
          "Affectation déjà voidée",
          "L'affectation " + itemAssignmentId + " est déjà archivée.",
          "ASSIGNMENT_ALREADY_VOIDED");
    }

    Timestamp now = Timestamp.from(Instant.now());
    if (assignment.getAssigned_at() != null && now.before(assignment.getAssigned_at())) {
      now = assignment.getAssigned_at();
    }
    if (assignment.getReturned_at() == null) {
      ItemResponse item = requireItem(assignment.getItem_id());
      if (item.getStatus() != ItemStatus.ASSIGNED && item.getStatus() != ItemStatus.AVAILABLE) {
        throw new ConflictException(
            "État d'article incompatible",
            "L'article est dans l'état "
                + item.getStatus()
                + " et ne peut pas être voidé depuis une affectation.",
            "ITEM_STATE_INCOMPATIBLE");
      }

      int updated =
          itemAssignmentDAO.voidActiveItemAssignment(
              itemAssignmentId, now, currentUserId(), voidReason);
      if (updated == 0) {
        throw new ConflictException(
            "Affectation déjà clôturée",
            "L'affectation " + itemAssignmentId + " a été clôturée par une autre opération.",
            "ASSIGNMENT_ALREADY_RETURNED");
      }

      String restoreWarehouse = assignment.getSource_warehouse_id();
      if (restoreWarehouse == null) {
        restoreWarehouse = item.getCurrent_warehouse_id();
      }
      itemDAO.updateItemStatus(item.getId(), ItemStatus.AVAILABLE);
      itemDAO.updateItemLocation(item.getId(), restoreWarehouse);
      ItemResponse after = requireItem(item.getId());
      stockBalanceService.syncItem(item, after);
    } else {
      int updated =
          itemAssignmentDAO.archiveInactiveItemAssignment(
              itemAssignmentId, now, currentUserId(), voidReason);
      if (updated == 0) {
        throw new ConflictException(
            "Affectation déjà voidée",
            "L'affectation " + itemAssignmentId + " est déjà archivée.",
            "ASSIGNMENT_ALREADY_VOIDED");
      }
    }

    return itemAssignmentRestMapper.toRest(requireAssignment(itemAssignmentId));
  }

  @Transactional
  public void deleteItemAssignment(String itemAssignmentId, String reason) {
    ItemAssignmentResponse assignment = requireAssignment(itemAssignmentId);
    if (assignment.getVoided_at() != null) {
      throw new ConflictException(
          "Affectation déjà voidée",
          "L'affectation " + itemAssignmentId + " est déjà archivée.",
          "ASSIGNMENT_ALREADY_VOIDED");
    }
    if (assignment.getReturned_at() == null) {
      throw new ConflictException(
          "Affectation active",
          "Une affectation active ne peut pas être supprimée. Utilisez POST /{id}/void avec un "
              + "motif ou POST /{id}/return.",
          "ACTIVE_ASSIGNMENT");
    }

    String archiveReason = isBlank(reason) ? DEFAULT_ARCHIVE_REASON : reason.trim();
    int updated =
        itemAssignmentDAO.archiveInactiveItemAssignment(
            itemAssignmentId, Timestamp.from(Instant.now()), currentUserId(), archiveReason);
    if (updated == 0) {
      throw new ConflictException(
          "Affectation déjà voidée",
          "L'affectation " + itemAssignmentId + " est déjà archivée.",
          "ASSIGNMENT_ALREADY_VOIDED");
    }
  }

  @Transactional
  public ItemAssignmentRest restoreItemAssignment(String itemAssignmentId) {
    ItemAssignmentResponse assignment = requireAssignment(itemAssignmentId);
    if (assignment.getVoided_at() == null) {
      throw new ConflictException(
          "Affectation non voidée",
          "L'affectation " + itemAssignmentId + " n'est pas archivée.",
          "ASSIGNMENT_NOT_VOIDED");
    }
    itemAssignmentDAO.restoreItemAssignment(itemAssignmentId);
    return itemAssignmentRestMapper.toRest(requireAssignment(itemAssignmentId));
  }

  public Optional<ItemAssignmentResponse> findActiveAssignmentByItem(String itemId) {
    return itemAssignmentDAO.getActiveItemAssignmentByItem(itemId);
  }

  public boolean hasActiveAssignmentForItem(String itemId) {
    return itemAssignmentDAO.getActiveItemAssignmentByItem(itemId).isPresent();
  }

  /**
   * Clôt l'affectation active d'un article pour un mouvement RETURN. Le mouvement gère déjà
   * l'emplacement, le statut et le solde.
   */
  @Transactional
  public Optional<ItemAssignmentResponse> closeActiveAssignmentForItem(
      String itemId, Timestamp returnedAt, String returnedBy) {
    Optional<ItemAssignmentResponse> active =
        itemAssignmentDAO.getActiveItemAssignmentByItem(itemId);
    if (active.isEmpty()) {
      return Optional.empty();
    }
    int updated =
        itemAssignmentDAO.markReturned(active.get().getId(), returnedAt, returnedBy, null);
    if (updated == 0) {
      throw new ConflictException(
          "Affectation déjà clôturée",
          "L'affectation active de l'article "
              + itemId
              + " a été clôturée par une autre opération.",
          "ASSIGNMENT_ALREADY_RETURNED");
    }
    return itemAssignmentDAO.getItemAssignmentById(active.get().getId());
  }

  private ItemAssignmentResponse requireAssignment(String itemAssignmentId) {
    return itemAssignmentDAO
        .getItemAssignmentById(itemAssignmentId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Affectation d'article introuvable",
                    "Aucune affectation d'article ne correspond à l'identifiant "
                        + itemAssignmentId
                        + ".",
                    "NOT_FOUND"));
  }

  private ItemResponse requireItem(String itemId) {
    return itemDAO
        .getItemById(itemId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Article introuvable",
                    "Aucun article ne correspond à l'identifiant " + itemId + ".",
                    "NOT_FOUND"));
  }

  private void requireAvailableForAssignment(ItemResponse item) {
    if (item.getStatus() != ItemStatus.AVAILABLE) {
      throw new ConflictException(
          "Article non disponible",
          "L'article "
              + item.getId()
              + " n'est pas disponible pour une affectation (statut: "
              + item.getStatus()
              + ").",
          "ITEM_NOT_AVAILABLE");
    }
    if (itemAssignmentDAO.getActiveItemAssignmentByItem(item.getId()).isPresent()) {
      throw new ConflictException(
          "Affectation active existante",
          "L'article " + item.getId() + " possède déjà une affectation active.",
          "ASSIGNMENT_ALREADY_ACTIVE");
    }
  }

  private void requireNotVoided(ItemAssignmentResponse assignment) {
    if (assignment.getVoided_at() != null) {
      throw new ConflictException(
          "Affectation voidée",
          "L'affectation " + assignment.getId() + " est archivée et ne peut pas être modifiée.",
          "ASSIGNMENT_VOIDED");
    }
  }

  private void validateHistoricalDates(ItemAssignmentCreateRequest value) {
    if (value.getAssigned_at() == null) {
      throw new ClientException(
          "Date d'affectation obligatoire",
          "Une affectation historique avec returned_at doit aussi fournir assigned_at.",
          "CLIENT_ERROR");
    }
    if (value.getReturned_at().before(value.getAssigned_at())) {
      throw new ClientException(
          "Dates d'affectation invalides",
          "returned_at doit être postérieure ou égale à assigned_at.",
          "CLIENT_ERROR");
    }
  }

  private String requireReason(String reason) {
    if (isBlank(reason)) {
      throw new ClientException(
          "Motif obligatoire",
          "Un void ou un archivage d'affectation doit fournir un motif.",
          "CLIENT_ERROR");
    }
    return reason.trim();
  }

  private String currentUserId() {
    try {
      String userId = currentUser.require().userId();
      return isUuid(userId) ? userId : null;
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  private static boolean isUuid(String value) {
    if (isBlank(value)) return false;
    try {
      UUID.fromString(value);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
