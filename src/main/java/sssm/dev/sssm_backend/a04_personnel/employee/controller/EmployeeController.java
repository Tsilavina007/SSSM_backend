package sssm.dev.sssm_backend.a04_personnel.employee.controller;

import java.util.*;
import org.springframework.web.bind.annotation.*;
import sssm.dev.sssm_backend.a04_personnel.employee.entity.Employee;
import sssm.dev.sssm_backend.a04_personnel.employee.service.EmployeeService;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    private final EmployeeService service = new EmployeeService();
    @GetMapping public List<Employee> findAll(@RequestParam Optional<String> search,@RequestParam Optional<Integer> page,@RequestParam Optional<Integer> pageSize){return service.findAll(search,page,pageSize);}
    @GetMapping("/{id}") public Optional<Employee> findById(@PathVariable String id){return service.findById(id);}
    @PostMapping public Employee create(@RequestBody Employee employee){return service.create(employee);}
    @PatchMapping("/{id}") public Employee update(@PathVariable String id,@RequestBody Employee employee){return service.update(id,employee);}
    @DeleteMapping("/{id}") public void delete(@PathVariable String id){service.delete(id);}
}
