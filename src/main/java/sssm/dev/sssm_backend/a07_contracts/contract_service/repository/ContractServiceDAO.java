package sssm.dev.sssm_backend.a07_contracts.contract_service.repository;

import java.util.List;
import java.util.Optional;
import sssm.dev.sssm_backend.a07_contracts.contract_service.entity.ContractServiceItem;

public interface ContractServiceDAO {
  List<ContractServiceItem> findByContractId(String contractId);

  Optional<ContractServiceItem> findById(String id);

  ContractServiceItem create(ContractServiceItem item);

  ContractServiceItem update(ContractServiceItem item);

  void delete(String id);
}
