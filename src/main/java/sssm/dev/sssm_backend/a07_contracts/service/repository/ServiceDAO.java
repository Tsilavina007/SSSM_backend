package sssm.dev.sssm_backend.a07_contracts.service.repository;

import java.util.List;
import java.util.Optional;
import sssm.dev.sssm_backend.a07_contracts.service.entity.Service;

public interface ServiceDAO {
  List<Service> findAll(int page, int pageSize);

  List<Service> findByCompanyId(String companyId);

  Optional<Service> findById(String id);

  Service create(Service service);

  Service update(Service service);

  void delete(String id);
}
