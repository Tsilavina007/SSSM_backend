package sssm.dev.sssm_backend.a02_organisation.branch.service;

import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a02_organisation.branch.entity.Branch;
import sssm.dev.sssm_backend.a02_organisation.branch.repository.BranchDAO;
import sssm.dev.sssm_backend.a02_organisation.branch.repository.BranchDAOImpl;

public class BranchService {
    private final BranchDAO branchDAO = new BranchDAOImpl();

    public List<Branch> getBranches(Optional<String> search, Optional<Integer> page, Optional<Integer> pageSize) {
        int p = page.orElse(1), size = pageSize.orElse(10);
        return search.isPresent() ? branchDAO.searchBranchesByName(search.get(), p, size)
                : branchDAO.getAllBranches(p, size);
    }
    public Optional<Branch> getBranchById(String id) { return branchDAO.getBranchById(id); }
    public Branch createBranch(Branch branch) { return branchDAO.createBranch(branch); }
    public Branch updateBranch(String id, Branch branch) {
        if (branchDAO.getBranchById(id).isEmpty()) throw new IllegalArgumentException("Branch not found with ID: " + id);
        branch.setId(id);
        return branchDAO.updateBranch(branch);
    }
    public void deleteBranch(String id) {
        if (branchDAO.getBranchById(id).isPresent()) branchDAO.deleteBranch(id);
    }
}
