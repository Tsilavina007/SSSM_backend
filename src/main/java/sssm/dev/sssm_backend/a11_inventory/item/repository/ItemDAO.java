package sssm.dev.sssm_backend.a11_inventory.item.repository;
import java.util.*;
import sssm.dev.sssm_backend.a11_inventory.item.entity.Item;
public interface ItemDAO { List<Item> findAll(int page,int pageSize); List<Item> search(String value,int page,int pageSize); Optional<Item> findById(String id); Item create(Item value); Item update(Item value); void delete(String id); }
