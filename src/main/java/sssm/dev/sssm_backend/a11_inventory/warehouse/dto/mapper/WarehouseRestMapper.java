package sssm.dev.sssm_backend.a11_inventory.warehouse.dto.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a02_organisation.branch.dto.BranchResponse;
import sssm.dev.sssm_backend.a02_organisation.branch.dto.mapper.BranchRestMapper;
import sssm.dev.sssm_backend.a02_organisation.branch.dto.rest.BranchRest;
import sssm.dev.sssm_backend.a02_organisation.branch.repository.BranchDAOImpl;
import sssm.dev.sssm_backend.a02_organisation.company.dto.mapper.CompanyRestMapper;
import sssm.dev.sssm_backend.a02_organisation.company.entity.Company;
import sssm.dev.sssm_backend.a02_organisation.company.repository.CompanyDAOImpl;
import sssm.dev.sssm_backend.a11_inventory.warehouse.dto.WarehouseResponse;
import sssm.dev.sssm_backend.a11_inventory.warehouse.dto.rest.WarehouseRest;

@Component
@RequiredArgsConstructor
public class WarehouseRestMapper {
  private final CompanyDAOImpl companyDAOImpl;
  private final CompanyRestMapper companyRestMapper;
  private final BranchDAOImpl branchDAOImpl;
  private final BranchRestMapper branchRestMapper;

  public WarehouseRest toRest(WarehouseResponse rs) {
    Company company =
        companyDAOImpl
            .getCompanyById(rs.getCompany_id())
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Entreprise introuvable",
                        "Aucune entreprise ne correspond à l'identifiant " + rs.getCompany_id() + ".",
                        "NOT_FOUND"));
    BranchRest branch = null;
    if (rs.getBranch_id() != null) {
      BranchResponse branchResponse =
          branchDAOImpl
              .getBranchById(rs.getBranch_id())
              .orElseThrow(
                  () ->
                      new NotFoundException(
                          "Succursale introuvable",
                          "Aucune succursale ne correspond à l'identifiant "
                              + rs.getBranch_id()
                              + ".",
                          "NOT_FOUND"));
      branch = branchRestMapper.toRest(branchResponse);
    }
    return new WarehouseRest(
        rs.getId(),
        companyRestMapper.toRest(company),
        branch,
        rs.getName(),
        rs.getAddress(),
        rs.getActive(),
        rs.getCreated_at(),
        rs.getUpdated_at(),
        rs.getArchived_at());
  }
}
