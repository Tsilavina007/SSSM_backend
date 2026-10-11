package sssm.dev.sssm_backend.a07_contracts.contract.service;

import java.util.List;
import java.util.Optional;
import sssm.dev.sssm_backend.a07_contracts.contract.entity.Contract;
import sssm.dev.sssm_backend.a07_contracts.contract.repository.ContractDAO;
import sssm.dev.sssm_backend.a07_contracts.contract.repository.ContractDAOImpl;

public class ContractService {
  private final ContractDAO dao = new ContractDAOImpl();

  public List<Contract> findAll(Optional<Integer> page, Optional<Integer> size) {
    return dao.findAll(page.orElse(1), size.orElse(10));
  }

  public List<Contract> findByCompanyId(String companyId) {
    return dao.findByCompanyId(companyId);
  }

  public List<Contract> findByClientId(String clientId) {
    return dao.findByClientId(clientId);
  }

  public Optional<Contract> findById(String id) {
    return dao.findById(id);
  }

  public Contract create(Contract contract) {
    return dao.create(contract);
  }

  public Contract update(String id, Contract contract) {
    if (dao.findById(id).isEmpty())
      throw new IllegalArgumentException("Contract not found with ID: " + id);
    contract.setId(id);
    return dao.update(contract);
  }

  public void delete(String id) {
    dao.delete(id);
  }
}
