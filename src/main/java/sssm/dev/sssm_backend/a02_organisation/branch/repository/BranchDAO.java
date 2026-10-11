package sssm.dev.sssm_backend.a02_organisation.branch.repository;

import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a02_organisation.branch.entity.Branch;

public interface BranchDAO {
    List<Branch> getAllBranches(int page, int pageSize);
    List<Branch> searchBranchesByName(String searchName, int page, int pageSize);
    Optional<Branch> getBranchById(String branchId);
    Branch createBranch(Branch branch);
    Branch updateBranch(Branch branch);
    void deleteBranch(String branchId);
}
