package sssm.dev.sssm_backend.a02_organisation.branch.controller;

import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a02_organisation.branch.dto.BranchCreateRequest;
import sssm.dev.sssm_backend.a02_organisation.branch.dto.BranchUpdateRequest;
import sssm.dev.sssm_backend.a02_organisation.branch.entity.Branch;
import sssm.dev.sssm_backend.a02_organisation.branch.service.BranchService;

@RestController
@RequestMapping("/api/branch")
public class BranchController {
  private final BranchService branchService;

  BranchController(BranchService branchService) {
    this.branchService = branchService;
  }

  @GetMapping
  public ResponseEntity<Object> getBranches(
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize,
      @RequestParam Optional<String> search) {
    List<Branch> branches = branchService.getBranches(search, page, pageSize);
    return ResponseEntity.ok().body(branches);
  }

  @GetMapping("/{branchId}")
  public ResponseEntity<Object> getBranchById(@PathVariable String branchId) {
    Optional<Branch> branches = branchService.getBranchById(branchId);
    return ResponseEntity.ok().body(branches);
  }

  @PostMapping
  public ResponseEntity<Object> createBranch(@RequestBody BranchCreateRequest branch) {
    Branch branchRes = branchService.createBranch(branch);
    return ResponseEntity.ok().body(branchRes);
  }

  @PatchMapping("/{branchId}")
  public ResponseEntity<Object> updateBranch(
      @PathVariable String branchId, @RequestBody BranchUpdateRequest branch) {
    Branch branchRes = branchService.updateBranch(branchId, branch);
    return ResponseEntity.ok().body(branchRes);
  }

  @DeleteMapping("/{branchId}")
  public ResponseEntity<Object> deleteBranch(@PathVariable String branchId) {
    branchService.deleteBranch(branchId);
    return ResponseEntity.ok().body("Company deleted : " + branchId);
  }
}
