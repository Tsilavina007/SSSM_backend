package sssm.dev.sssm_backend.a02_organisation.branch.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a02_organisation.agency.entity.Agency;
import sssm.dev.sssm_backend.a02_organisation.agency.service.AgencyService;
import sssm.dev.sssm_backend.a02_organisation.branch.dto.BranchCreateRequest;
import sssm.dev.sssm_backend.a02_organisation.branch.dto.BranchResponse;
import sssm.dev.sssm_backend.a02_organisation.branch.dto.BranchUpdateRequest;
import sssm.dev.sssm_backend.a02_organisation.branch.entity.Branch;
import sssm.dev.sssm_backend.a02_organisation.branch.repository.BranchDAO;
import sssm.dev.sssm_backend.a02_organisation.branch.repository.BranchDAOImpl;

public class BranchService {
  private final BranchDAO branchDAO;
  private final AgencyService agencyService;

  public BranchService() {
    this.branchDAO = new BranchDAOImpl();
    this.agencyService = new AgencyService();
  }
  public List<Branch> getBranches(
      Optional<String> search, Optional<Integer> page, Optional<Integer> pageSize) throws SQLException {
    int p = page.orElse(1), size = pageSize.orElse(10);
    return search.isPresent()
        ? mapperListResponseToBranch(branchDAO.searchBranchesByName(search.get(), p, size))
        : mapperListResponseToBranch(branchDAO.getAllBranches(p, size));
  }

  public Optional<Branch> getBranchById(String id) throws SQLException {
    return Optional.of(mapperResponseToBranch(branchDAO.getBranchById(id).get()));
  }

  public Branch createBranch(BranchCreateRequest branch) throws SQLException {
    return mapperResponseToBranch(branchDAO.createBranch(branch));
  }

  public Branch updateBranch(String id, BranchUpdateRequest branch) throws SQLException {
    if (branchDAO.getBranchById(id).isEmpty())
      throw new IllegalArgumentException("Branch not found with ID: " + id);
    return mapperResponseToBranch(branchDAO.updateBranch(id, branch));
  }

  public void deleteBranch(String id) {
    if (branchDAO.getBranchById(id).isPresent()) branchDAO.deleteBranch(id);
  }

  private Branch mapperResponseToBranch(BranchResponse rs) throws SQLException {
    if (rs == null || rs.getAgence_id() == null) {
      return null;
    }
  Agency c = agencyService.getAgencyById(rs.getAgence_id()).get();
    return (new Branch(rs.getId(), rs.getCompany_id(), c, rs.getName(), rs.getCode(), rs.getAddress(),
                          rs.getCity(), rs.getPhone(), rs.getEmail(), rs.getStatus(), rs.getCreated_at(), rs.getUpdated_at()));
  }

  private List<Branch> mapperListResponseToBranch(List<BranchResponse> items)
      throws SQLException {
    return items.stream()
        .map(
            item -> {
              try {
                return mapperResponseToBranch(item);
              } catch (SQLException e) {
                throw new RuntimeException(e);
              }
            })
        .toList();
  }
}
