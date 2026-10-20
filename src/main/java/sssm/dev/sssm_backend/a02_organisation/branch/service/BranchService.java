package sssm.dev.sssm_backend.a02_organisation.branch.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a02_organisation.branch.dto.BranchCreateRequest;
import sssm.dev.sssm_backend.a02_organisation.branch.dto.BranchResponse;
import sssm.dev.sssm_backend.a02_organisation.branch.dto.BranchUpdateRequest;
import sssm.dev.sssm_backend.a02_organisation.branch.dto.mapper.BranchRestMapper;
import sssm.dev.sssm_backend.a02_organisation.branch.dto.rest.BranchRest;
import sssm.dev.sssm_backend.a02_organisation.branch.repository.BranchDAOImpl;

@Service
@RequiredArgsConstructor
public class BranchService {
  private final BranchDAOImpl branchDAO;
  private final BranchRestMapper branchRestMapper;

  public List<BranchRest> getBranches(
      Optional<String> search, Optional<Integer> page, Optional<Integer> pageSize) {
    int p = page.orElse(1), size = pageSize.orElse(10);
    List<BranchResponse> branches =
        search
            .map(value -> branchDAO.searchBranchesByName(value, p, size))
            .orElseGet(() -> branchDAO.getAllBranches(p, size));
    return branches.stream().map(branchRestMapper::toRest).toList();
  }

  public BranchRest getBranchById(String id) {
    return branchDAO
        .getBranchById(id)
        .map(branchRestMapper::toRest)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Succursale introuvable",
                    "Aucune succursale ne correspond à l'identifiant " + id + ".",
                    "NOT_FOUND"));
  }

  public BranchRest createBranch(BranchCreateRequest branch) {
    return branchRestMapper.toRest(branchDAO.createBranch(branch));
  }

  public BranchRest updateBranch(String id, BranchUpdateRequest branch) {
    if (branchDAO.getBranchById(id).isEmpty())
      throw new NotFoundException(
          "Succursale introuvable",
          "Aucune succursale ne correspond à l'identifiant " + id + ".",
          "NOT_FOUND");
    return branchRestMapper.toRest(branchDAO.updateBranch(id, branch));
  }

  public void deleteBranch(String id) {
    if (branchDAO.getBranchById(id).isPresent()) branchDAO.deleteBranch(id);
  }
}
