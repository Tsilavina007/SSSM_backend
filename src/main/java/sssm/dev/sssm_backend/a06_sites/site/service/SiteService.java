package sssm.dev.sssm_backend.a06_sites.site.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a06_sites.site.dto.SiteCreateRequest;
import sssm.dev.sssm_backend.a06_sites.site.dto.SiteResponse;
import sssm.dev.sssm_backend.a06_sites.site.dto.SiteUpdateRequest;
import sssm.dev.sssm_backend.a06_sites.site.entity.Site;
import sssm.dev.sssm_backend.a06_sites.site.repository.SiteDAO;

@Service
@RequiredArgsConstructor
public class SiteService {
  private final SiteDAO dao;

  public List<SiteResponse> findAll(
      Optional<String> search, Optional<Integer> page, Optional<Integer> size) {
    int currentPage = page.orElse(1);
    int pageSize = size.orElse(10);
    List<Site> sites =
        search
            .map(value -> dao.search(value, currentPage, pageSize))
            .orElseGet(() -> dao.findAll(currentPage, pageSize));
    return sites.stream().map(this::toResponse).toList();
  }

  public SiteResponse findById(String id) {
    return dao.findById(id)
        .map(this::toResponse)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Site introuvable",
                    "Aucun site ne correspond à l'identifiant " + id + ".",
                    "NOT_FOUND"));
  }

  public SiteResponse create(SiteCreateRequest request) {
    return toResponse(dao.create(toEntity(request)));
  }

  public SiteResponse update(String id, SiteUpdateRequest request) {
    if (dao.findById(id).isEmpty()) {
      throw new NotFoundException(
          "Site introuvable", "Aucun site ne correspond à l'identifiant " + id + ".", "NOT_FOUND");
    }
    Site site = toEntity(request);
    site.setId(id);
    return toResponse(dao.update(site));
  }

  public void delete(String id) {
    dao.delete(id);
  }

  private Site toEntity(SiteCreateRequest request) {
    Site site = new Site();
    site.setCompany_id(request.getCompany_id());
    site.setClient_id(request.getClient_id());
    site.setBranch_id(request.getBranch_id());
    site.setName(request.getName());
    site.setCode(request.getCode());
    site.setAddress(request.getAddress());
    site.setCity(request.getCity());
    site.setLatitude(request.getLatitude());
    site.setLongitude(request.getLongitude());
    site.setStatus(request.getStatus());
    site.setAccess_instructions(request.getAccess_instructions());
    site.setRisk_level(request.getRisk_level());
    return site;
  }

  private Site toEntity(SiteUpdateRequest request) {
    Site site = new Site();
    site.setCompany_id(request.getCompany_id());
    site.setClient_id(request.getClient_id());
    site.setBranch_id(request.getBranch_id());
    site.setName(request.getName());
    site.setCode(request.getCode());
    site.setAddress(request.getAddress());
    site.setCity(request.getCity());
    site.setLatitude(request.getLatitude());
    site.setLongitude(request.getLongitude());
    site.setStatus(request.getStatus());
    site.setAccess_instructions(request.getAccess_instructions());
    site.setRisk_level(request.getRisk_level());
    return site;
  }

  private SiteResponse toResponse(Site site) {
    return new SiteResponse(
        site.getId(),
        site.getCompany_id(),
        site.getClient_id(),
        site.getBranch_id(),
        site.getName(),
        site.getCode(),
        site.getAddress(),
        site.getCity(),
        site.getLatitude(),
        site.getLongitude(),
        site.getStatus(),
        site.getAccess_instructions(),
        site.getRisk_level(),
        site.getCreated_at(),
        site.getUpdated_at());
  }
}
