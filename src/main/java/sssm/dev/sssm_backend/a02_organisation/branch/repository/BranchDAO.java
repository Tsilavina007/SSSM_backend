package sssm.dev.sssm_backend.a02_organisation.branch.repository;

import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a02_organisation.branch.dto.BranchCreateRequest;
import sssm.dev.sssm_backend.a02_organisation.branch.dto.BranchResponse;
import sssm.dev.sssm_backend.a02_organisation.branch.dto.BranchUpdateRequest;

public interface BranchDAO {
  List<BranchResponse> getAllBranches(int page, int pageSize);

  List<BranchResponse> searchBranchesByName(String searchName, int page, int pageSize);

  Optional<BranchResponse> getBranchById(String branchId);

  BranchResponse createBranch(BranchCreateRequest branch);

  BranchResponse updateBranch(String id, BranchUpdateRequest branch);

  void deleteBranch(String branchId);
}
