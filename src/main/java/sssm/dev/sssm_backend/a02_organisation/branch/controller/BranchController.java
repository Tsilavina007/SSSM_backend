package sssm.dev.sssm_backend.a02_organisation.branch.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a02_organisation.branch.dto.BranchCreateRequest;
import sssm.dev.sssm_backend.a02_organisation.branch.dto.BranchUpdateRequest;
import sssm.dev.sssm_backend.a02_organisation.branch.dto.rest.BranchRest;
import sssm.dev.sssm_backend.a02_organisation.branch.service.BranchService;

@RestController
@RequestMapping("/api/branch")
public class BranchController {
  private final BranchService branchService;

  BranchController(BranchService branchService) {
    this.branchService = branchService;
  }

  @GetMapping
  public List<BranchRest> getBranches(
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize,
      @RequestParam Optional<String> search) {
    return branchService.getBranches(search, page, pageSize);
  }

  @GetMapping("/{branchId}")
  public BranchRest getBranchById(@PathVariable String branchId) {
    return branchService.getBranchById(branchId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public BranchRest createBranch(@Valid @RequestBody BranchCreateRequest branch) {
    return branchService.createBranch(branch);
  }

  @PatchMapping("/{branchId}")
  public BranchRest updateBranch(
      @PathVariable String branchId, @Valid @RequestBody BranchUpdateRequest branch) {
    return branchService.updateBranch(branchId, branch);
  }

  @DeleteMapping("/{branchId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteBranch(@PathVariable String branchId) {
    branchService.deleteBranch(branchId);
  }
}
