package sssm.dev.sssm_backend.a02_organisation.branch.controller;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a00_infrastructure.exception.ClientException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a01_common.errors.mapper.ErrorMapper;
import sssm.dev.sssm_backend.a02_organisation.branch.dto.BranchCreateRequest;
import sssm.dev.sssm_backend.a02_organisation.branch.dto.BranchUpdateRequest;
import sssm.dev.sssm_backend.a02_organisation.branch.entity.Branch;
import sssm.dev.sssm_backend.a02_organisation.branch.service.BranchService;

@RestController
@RequestMapping("/api/branch")
public class BranchController {
  @Autowired private BranchService branchService;

  @GetMapping
  public ResponseEntity<Object> getBranches(
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize,
      @RequestParam Optional<String> search) {
    try {
      List<Branch> branches = branchService.getBranches(search, page, pageSize);
      return ResponseEntity.ok().body(branches);
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(Map.of("errors", ErrorMapper.toItemError(e)));
    } catch (NotFoundException e) {
      return ResponseEntity.status(NOT_FOUND).body(Map.of("errors", ErrorMapper.toItemError(e)));
    } catch (ServerException e) {
      return ResponseEntity.internalServerError()
          .body(Map.of("errors", ErrorMapper.toItemError(e)));
    }
  }

  @GetMapping("/{branchId}")
  public ResponseEntity<Object> getBranchById(@PathVariable String branchId) {
    try {
      Optional<Branch> branches = branchService.getBranchById(branchId);
      return ResponseEntity.ok().body(branches);
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(Map.of("errors", ErrorMapper.toItemError(e)));
    } catch (NotFoundException e) {
      return ResponseEntity.status(NOT_FOUND).body(Map.of("errors", ErrorMapper.toItemError(e)));
    } catch (ServerException e) {
      return ResponseEntity.internalServerError()
          .body(Map.of("errors", ErrorMapper.toItemError(e)));
    }
  }

  @PostMapping
  public ResponseEntity<Object> createBranch(@RequestBody BranchCreateRequest branch) {
    try {
      Branch branchRes = branchService.createBranch(branch);
      return ResponseEntity.ok().body(branchRes);
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(Map.of("errors", ErrorMapper.toItemError(e)));
    } catch (NotFoundException e) {
      return ResponseEntity.status(NOT_FOUND).body(Map.of("errors", ErrorMapper.toItemError(e)));
    } catch (ServerException e) {
      return ResponseEntity.internalServerError()
          .body(Map.of("errors", ErrorMapper.toItemError(e)));
    }
  }

  @PatchMapping("/{branchId}")
  public ResponseEntity<Object> updateBranch(
      @PathVariable String branchId, @RequestBody BranchUpdateRequest branch) {
    try {
      Branch branchRes = branchService.updateBranch(branchId, branch);
      return ResponseEntity.ok().body(branchRes);
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(Map.of("errors", ErrorMapper.toItemError(e)));
    } catch (NotFoundException e) {
      return ResponseEntity.status(NOT_FOUND).body(Map.of("errors", ErrorMapper.toItemError(e)));
    } catch (ServerException e) {
      return ResponseEntity.internalServerError()
          .body(Map.of("errors", ErrorMapper.toItemError(e)));
    }
  }

  @DeleteMapping("/{branchId}")
  public ResponseEntity<Object> deleteBranch(@PathVariable String branchId) {
    try {
      branchService.deleteBranch(branchId);
      return ResponseEntity.ok().body("Company deleted : " + branchId);
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(Map.of("errors", ErrorMapper.toItemError(e)));
    } catch (NotFoundException e) {
      return ResponseEntity.status(NOT_FOUND).body(Map.of("errors", ErrorMapper.toItemError(e)));
    } catch (ServerException e) {
      return ResponseEntity.internalServerError()
          .body(Map.of("errors", ErrorMapper.toItemError(e)));
    }
  }
}
