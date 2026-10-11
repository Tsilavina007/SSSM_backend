package sssm.dev.sssm_backend.authentication.permission.service;

import java.util.*;
import sssm.dev.sssm_backend.authentication.permission.entity.Permission;
import sssm.dev.sssm_backend.authentication.permission.repository.*;

public class PermissionService {
    private final PermissionDAO dao=new PermissionDAOImpl();
    public List<Permission> findAll(Optional<Integer> page,Optional<Integer> size){return dao.findAll(page.orElse(1),size.orElse(10));}
    public Optional<Permission> findById(String id){return dao.findById(id);}
    public Permission create(Permission permission){return dao.create(permission);}
    public Permission update(String id,Permission permission){if(dao.findById(id).isEmpty())throw new IllegalArgumentException("Permission not found with ID: "+id);permission.setId(id);return dao.update(permission);}
    public void delete(String id){dao.delete(id);}
}
