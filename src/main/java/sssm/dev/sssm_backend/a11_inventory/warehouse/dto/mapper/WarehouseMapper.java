package sssm.dev.sssm_backend.a11_inventory.warehouse.dto.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import sssm.dev.sssm_backend.a02_organisation.branch.dto.BranchResponse;
import sssm.dev.sssm_backend.a02_organisation.branch.dto.mapper.BranchRestMapper;
import sssm.dev.sssm_backend.a02_organisation.branch.repository.BranchDAOImpl;
import sssm.dev.sssm_backend.a02_organisation.company.dto.mapper.CompanyRestMapper;
import sssm.dev.sssm_backend.a02_organisation.company.entity.Company;
import sssm.dev.sssm_backend.a02_organisation.company.repository.CompanyDAOImpl;
import sssm.dev.sssm_backend.a11_inventory.warehouse.dto.WarehouseResponse;
import sssm.dev.sssm_backend.a11_inventory.warehouse.entity.Warehouse;

@Component
public class WarehouseMapper {
  @Autowired private CompanyDAOImpl companyDAOImpl;
  @Autowired private CompanyRestMapper companyRestMapper;
  @Autowired private BranchDAOImpl branchDAOImpl;
  @Autowired private BranchRestMapper branchRestMapper;

  public Warehouse toRest(WarehouseResponse rs) {
    Company company = companyDAOImpl.getCompanyById(rs.getCompany_id()).get();
    BranchResponse branch = branchDAOImpl.getBranchById(rs.getBranch_id()).get();
    return (new Warehouse(
        rs.getId(),
        companyRestMapper.toRest(company),
        branchRestMapper.toRest(branch),
        rs.getName(),
        rs.getAddress(),
        rs.getActive(),
        rs.getCreated_at(),
        rs.getUpdated_at(),
        rs.getArchived_at()));
  }
}
