package sssm.dev.sssm_backend.a07_contracts.service.service;

import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a07_contracts.service.entity.Service;
import sssm.dev.sssm_backend.a07_contracts.service.repository.ServiceDAO;
import sssm.dev.sssm_backend.a07_contracts.service.repository.ServiceDAOImpl;

public class ServiceService {
  private final ServiceDAO dao = new ServiceDAOImpl();

  public List<Service> findAll(Optional<Integer> page, Optional<Integer> size) {
    return dao.findAll(page.orElse(1), size.orElse(10));
  }

  public List<Service> findByCompanyId(String companyId) {
    return dao.findByCompanyId(companyId);
  }

  public Optional<Service> findById(String id) {
    return dao.findById(id);
  }

  public Service create(Service service) {
    return dao.create(service);
  }

  public Service update(String id, Service service) {
    if (dao.findById(id).isEmpty())
      throw new IllegalArgumentException("Service not found with ID: " + id);
    service.setId(id);
    return dao.update(service);
  }

  public void delete(String id) {
    dao.delete(id);
  }
}
