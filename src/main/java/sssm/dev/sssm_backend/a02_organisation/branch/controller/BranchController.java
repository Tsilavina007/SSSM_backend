package sssm.dev.sssm_backend.a02_organisation.branch.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a02_organisation.branch.entity.Branch;
import sssm.dev.sssm_backend.a02_organisation.branch.service.BranchService;

@RestController
@RequestMapping("/api/branch")
public class BranchController {
    private final BranchService branchService = new BranchService();

    @GetMapping
    public List<Branch> getBranches(@RequestParam Optional<Integer> page,
            @RequestParam Optional<Integer> pageSize, @RequestParam Optional<String> search) {
        return branchService.getBranches(search, page, pageSize);
    }
    @GetMapping("/{branchId}")
    public Optional<Branch> getBranchById(@PathVariable String branchId) {
        return branchService.getBranchById(branchId);
    }
    @PostMapping
    public Branch createBranch(@RequestBody Branch branch) { return branchService.createBranch(branch); }
    @PatchMapping("/{branchId}")
    public Branch updateBranch(@PathVariable String branchId, @RequestBody Branch branch) {
        return branchService.updateBranch(branchId, branch);
    }
    @DeleteMapping("/{branchId}")
    public void deleteBranch(@PathVariable String branchId) { branchService.deleteBranch(branchId); }
}
