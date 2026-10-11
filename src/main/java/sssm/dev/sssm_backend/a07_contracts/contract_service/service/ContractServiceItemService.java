package sssm.dev.sssm_backend.a07_contracts.contract_service.service;

import java.util.List;
import java.util.Optional;
import sssm.dev.sssm_backend.a07_contracts.contract_service.entity.ContractServiceItem;
import sssm.dev.sssm_backend.a07_contracts.contract_service.repository.ContractServiceDAO;
import sssm.dev.sssm_backend.a07_contracts.contract_service.repository.ContractServiceDAOImpl;

public class ContractServiceItemService {
  private final ContractServiceDAO dao = new ContractServiceDAOImpl();

  public List<ContractServiceItem> findByContractId(String contractId) {
    return dao.findByContractId(contractId);
  }

  public Optional<ContractServiceItem> findById(String id) {
    return dao.findById(id);
  }

  public ContractServiceItem create(ContractServiceItem item) {
    return dao.create(item);
  }

  public ContractServiceItem update(String id, ContractServiceItem item) {
    if (dao.findById(id).isEmpty())
      throw new IllegalArgumentException("Contract service not found with ID: " + id);
    item.setId(id);
    return dao.update(item);
  }

  public void delete(String id) {
    dao.delete(id);
  }
}
