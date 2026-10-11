package sssm.dev.sssm_backend.a11_inventory.item_assignment.repository;
import java.util.*;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.entity.ItemAssignment;
public interface ItemAssignmentDAO { List<ItemAssignment> findAll(int page,int pageSize); List<ItemAssignment> search(String value,int page,int pageSize); Optional<ItemAssignment> findById(String id); ItemAssignment create(ItemAssignment value); ItemAssignment update(ItemAssignment value); void delete(String id); }
