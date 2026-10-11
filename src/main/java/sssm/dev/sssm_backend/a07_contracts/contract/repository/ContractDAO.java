package sssm.dev.sssm_backend.a07_contracts.contract.repository;

import java.util.List;
import java.util.Optional;
import sssm.dev.sssm_backend.a07_contracts.contract.entity.Contract;

public interface ContractDAO {
  List<Contract> findAll(int page, int pageSize);

  List<Contract> findByCompanyId(String companyId);

  List<Contract> findByClientId(String clientId);

  Optional<Contract> findById(String id);

  Contract create(Contract contract);

  Contract update(Contract contract);

  void delete(String id);
}
