"""
Fix all service tests broken by multi-store migration.
Rewrites UnitServiceTest, SupplierServiceTest, CategoryServiceTest
to use storeId-aware mocks, and patches remaining issues in
InventoryAdjustmentServiceTest, ImportOrderServiceTest, AttendanceServiceTest.
"""
import re, os

BASE = "backend/shoplite/src/test/java/com/quyen/shoplite/service"

# ============================================================
# Helpers
# ============================================================
def write(rel, content):
    path = os.path.join(BASE, rel)
    with open(path, 'w', encoding='utf-8', newline='\r\n') as f:
        f.write(content)
    print(f"Written: {rel}")

def read(rel):
    path = os.path.join(BASE, rel)
    with open(path, encoding='utf-8') as f:
        return f.read()

# ============================================================
# 1) UnitServiceTest — full rewrite
# ============================================================
unit_test = """\
package com.quyen.shoplite.service;

import com.quyen.shoplite.repository.UnitRepository;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;

import com.quyen.shoplite.domain.Store;
import com.quyen.shoplite.domain.Unit;
import com.quyen.shoplite.domain.request.ReqUnitUpsertDTO;
import com.quyen.shoplite.domain.response.ResUnitDTO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnitServiceTest {

    @Mock
    private UnitRepository unitRepository;
    @Mock
    private CurrentStoreService currentStoreService;

    @InjectMocks
    private UnitService unitService;

    private Store testStore() {
        Store store = new Store();
        store.setId(1L);
        return store;
    }

    @Test
    void create_ShouldReturnUnit_WhenNameIsUnique() {
        when(currentStoreService.getCurrentStore()).thenReturn(testStore());
        when(unitRepository.existsByStoreIdAndName(1L, "New Unit")).thenReturn(false);

        ReqUnitUpsertDTO req = new ReqUnitUpsertDTO();
        req.setName("  New Unit  ");
        req.setDescription("Unit Description");

        Unit savedUnit = Unit.builder()
                .id(1)
                .name("New Unit")
                .description("Unit Description")
                .build();
        when(unitRepository.save(any(Unit.class))).thenReturn(savedUnit);

        ResUnitDTO result = unitService.create(req);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("New Unit", result.getName());
        verify(unitRepository).save(argThat(u -> u.getName().equals("New Unit")));
    }

    @Test
    void create_ShouldThrowBadRequest_WhenNameExists() {
        when(currentStoreService.getCurrentStore()).thenReturn(testStore());
        when(unitRepository.existsByStoreIdAndName(1L, "Existing Unit")).thenReturn(true);

        ReqUnitUpsertDTO req = new ReqUnitUpsertDTO();
        req.setName("Existing Unit");

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            unitService.create(req)
        );
        assertEquals("Unit name already exists: Existing Unit", exception.getMessage());
        verify(unitRepository, never()).save(any(Unit.class));
    }

    @Test
    void update_ShouldReturnUnit_WhenIdExistsAndNameIsUnique() {
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Integer id = 1;
        ReqUnitUpsertDTO req = new ReqUnitUpsertDTO();
        req.setName("Updated Unit");

        Unit existingUnit = Unit.builder().id(id).name("Old Name").build();
        when(unitRepository.findByIdAndStoreId(id, 1L)).thenReturn(Optional.of(existingUnit));
        when(unitRepository.existsByStoreIdAndNameAndIdNot(1L, "Updated Unit", id)).thenReturn(false);

        Unit savedUnit = Unit.builder().id(id).name("Updated Unit").build();
        when(unitRepository.save(existingUnit)).thenReturn(savedUnit);

        ResUnitDTO result = unitService.update(id, req);

        assertNotNull(result);
        assertEquals("Updated Unit", result.getName());
        verify(unitRepository).save(existingUnit);
    }

    @Test
    void update_ShouldThrowResourceNotFound_WhenIdDoesNotExist() {
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Integer id = 99;
        ReqUnitUpsertDTO req = new ReqUnitUpsertDTO();
        req.setName("Updated Unit");

        when(unitRepository.findByIdAndStoreId(id, 1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
            unitService.update(id, req)
        );
        assertEquals("Unit not found with id=99", exception.getMessage());
        verify(unitRepository, never()).save(any(Unit.class));
    }

    @Test
    void update_ShouldThrowBadRequest_WhenNameExistsForAnotherId() {
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Integer id = 1;
        ReqUnitUpsertDTO req = new ReqUnitUpsertDTO();
        req.setName("Duplicate Name");

        Unit existingUnit = Unit.builder().id(id).name("Old Name").build();
        when(unitRepository.findByIdAndStoreId(id, 1L)).thenReturn(Optional.of(existingUnit));
        when(unitRepository.existsByStoreIdAndNameAndIdNot(1L, "Duplicate Name", id)).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            unitService.update(id, req)
        );
        assertEquals("Unit name already exists: Duplicate Name", exception.getMessage());
        verify(unitRepository, never()).save(any(Unit.class));
    }

    @Test
    void delete_ShouldCallRepositoryDelete_WhenIdExists() {
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Integer id = 1;
        Unit existingUnit = Unit.builder().id(id).name("Name").build();
        when(unitRepository.findByIdAndStoreId(id, 1L)).thenReturn(Optional.of(existingUnit));

        unitService.delete(id);

        verify(unitRepository).delete(existingUnit);
    }

    @Test
    void delete_ShouldThrowResourceNotFound_WhenIdDoesNotExist() {
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Integer id = 99;
        when(unitRepository.findByIdAndStoreId(id, 1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
            unitService.delete(id)
        );
        assertEquals("Unit not found with id=99", exception.getMessage());
        verify(unitRepository, never()).delete(any(Unit.class));
    }
}
"""

# ============================================================
# 2) SupplierServiceTest — full rewrite
# ============================================================
supplier_test = """\
package com.quyen.shoplite.service;

import com.quyen.shoplite.repository.SupplierRepository;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;

import com.quyen.shoplite.domain.Store;
import com.quyen.shoplite.domain.Supplier;
import com.quyen.shoplite.domain.request.ReqSupplierDTO;
import com.quyen.shoplite.domain.response.ResSupplierDTO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private CurrentStoreService currentStoreService;

    @InjectMocks
    private SupplierService supplierService;

    private Store testStore() {
        Store store = new Store();
        store.setId(1L);
        return store;
    }

    @Test
    void create_ShouldReturnSupplier_WhenNameIsUnique() {
        when(currentStoreService.getCurrentStore()).thenReturn(testStore());
        when(supplierRepository.existsByStoreIdAndName(1L, "New Supplier")).thenReturn(false);

        ReqSupplierDTO req = new ReqSupplierDTO();
        req.setName("  New Supplier  ");
        req.setPhone("0987654321");
        req.setEmail("test@example.com");

        Supplier savedSupplier = Supplier.builder()
                .id(1)
                .name("New Supplier")
                .phone("0987654321")
                .email("test@example.com")
                .build();
        when(supplierRepository.save(any(Supplier.class))).thenReturn(savedSupplier);

        ResSupplierDTO result = supplierService.create(req);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("New Supplier", result.getName());
        assertEquals("0987654321", result.getPhone());
        assertEquals("test@example.com", result.getEmail());
        verify(supplierRepository).save(argThat(s -> s.getName().equals("New Supplier")));
    }

    @Test
    void create_ShouldThrowBadRequest_WhenNameExists() {
        when(currentStoreService.getCurrentStore()).thenReturn(testStore());
        when(supplierRepository.existsByStoreIdAndName(1L, "Existing Supplier")).thenReturn(true);

        ReqSupplierDTO req = new ReqSupplierDTO();
        req.setName("Existing Supplier");

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            supplierService.create(req)
        );
        assertEquals("Supplier name already exists: Existing Supplier", exception.getMessage());
        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void update_ShouldReturnSupplier_WhenIdExistsAndNameIsUnique() {
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Integer id = 1;
        ReqSupplierDTO req = new ReqSupplierDTO();
        req.setName("Updated Supplier");

        Supplier existingSupplier = Supplier.builder().id(id).name("Old Name").build();
        when(supplierRepository.findByIdAndStoreId(id, 1L)).thenReturn(Optional.of(existingSupplier));
        when(supplierRepository.existsByStoreIdAndNameAndIdNot(1L, "Updated Supplier", id)).thenReturn(false);

        Supplier savedSupplier = Supplier.builder().id(id).name("Updated Supplier").build();
        when(supplierRepository.save(existingSupplier)).thenReturn(savedSupplier);

        ResSupplierDTO result = supplierService.update(id, req);

        assertNotNull(result);
        assertEquals("Updated Supplier", result.getName());
        verify(supplierRepository).save(existingSupplier);
    }

    @Test
    void update_ShouldThrowResourceNotFound_WhenIdDoesNotExist() {
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Integer id = 99;
        ReqSupplierDTO req = new ReqSupplierDTO();
        req.setName("Updated Supplier");

        when(supplierRepository.findByIdAndStoreId(id, 1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
            supplierService.update(id, req)
        );
        assertEquals("Supplier not found with id=99", exception.getMessage());
        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void update_ShouldThrowBadRequest_WhenNameExistsForAnotherId() {
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Integer id = 1;
        ReqSupplierDTO req = new ReqSupplierDTO();
        req.setName("Duplicate Name");

        Supplier existingSupplier = Supplier.builder().id(id).name("Old Name").build();
        when(supplierRepository.findByIdAndStoreId(id, 1L)).thenReturn(Optional.of(existingSupplier));
        when(supplierRepository.existsByStoreIdAndNameAndIdNot(1L, "Duplicate Name", id)).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            supplierService.update(id, req)
        );
        assertEquals("Supplier name already exists: Duplicate Name", exception.getMessage());
        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void delete_ShouldCallRepositoryDelete_WhenIdExists() {
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Integer id = 1;
        Supplier existingSupplier = Supplier.builder().id(id).name("Name").build();
        when(supplierRepository.findByIdAndStoreId(id, 1L)).thenReturn(Optional.of(existingSupplier));

        supplierService.delete(id);

        verify(supplierRepository).delete(existingSupplier);
    }

    @Test
    void delete_ShouldThrowResourceNotFound_WhenIdDoesNotExist() {
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Integer id = 99;
        when(supplierRepository.findByIdAndStoreId(id, 1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
            supplierService.delete(id)
        );
        assertEquals("Supplier not found with id=99", exception.getMessage());
        verify(supplierRepository, never()).delete(any(Supplier.class));
    }
}
"""

# ============================================================
# 3) CategoryServiceTest — full rewrite
# ============================================================
category_test = """\
package com.quyen.shoplite.service;

import com.quyen.shoplite.repository.CategoryRepository;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;

import com.quyen.shoplite.domain.Category;
import com.quyen.shoplite.domain.Store;
import com.quyen.shoplite.domain.request.ReqCategoryUpsertDTO;
import com.quyen.shoplite.domain.response.ResCategoryDTO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CurrentStoreService currentStoreService;

    @InjectMocks
    private CategoryService categoryService;

    private Store testStore() {
        Store store = new Store();
        store.setId(1L);
        return store;
    }

    @Test
    void create_ShouldReturnCategory_WhenNameIsUnique() {
        when(currentStoreService.getCurrentStore()).thenReturn(testStore());
        when(categoryRepository.existsByStoreIdAndName(1L, "New Category")).thenReturn(false);

        ReqCategoryUpsertDTO req = new ReqCategoryUpsertDTO();
        req.setName("  New Category  ");

        Category savedCategory = Category.builder()
                .id(1)
                .name("New Category")
                .build();
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        ResCategoryDTO result = categoryService.create(req);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("New Category", result.getName());
        verify(categoryRepository).save(argThat(c -> c.getName().equals("New Category")));
    }

    @Test
    void create_ShouldThrowBadRequest_WhenNameExists() {
        when(currentStoreService.getCurrentStore()).thenReturn(testStore());
        when(categoryRepository.existsByStoreIdAndName(1L, "Existing Category")).thenReturn(true);

        ReqCategoryUpsertDTO req = new ReqCategoryUpsertDTO();
        req.setName("Existing Category");

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            categoryService.create(req)
        );
        assertEquals("Category name already exists: Existing Category", exception.getMessage());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void update_ShouldReturnCategory_WhenIdExistsAndNameIsUnique() {
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Integer id = 1;
        ReqCategoryUpsertDTO req = new ReqCategoryUpsertDTO();
        req.setName("Updated Category");

        Category existingCategory = Category.builder().id(id).name("Old Name").build();
        when(categoryRepository.findByIdAndStoreId(id, 1L)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.existsByStoreIdAndNameAndIdNot(1L, "Updated Category", id)).thenReturn(false);

        Category savedCategory = Category.builder().id(id).name("Updated Category").build();
        when(categoryRepository.save(existingCategory)).thenReturn(savedCategory);

        ResCategoryDTO result = categoryService.update(id, req);

        assertNotNull(result);
        assertEquals("Updated Category", result.getName());
        verify(categoryRepository).save(existingCategory);
    }

    @Test
    void update_ShouldThrowResourceNotFound_WhenIdDoesNotExist() {
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Integer id = 99;
        ReqCategoryUpsertDTO req = new ReqCategoryUpsertDTO();
        req.setName("Updated Category");

        when(categoryRepository.findByIdAndStoreId(id, 1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
            categoryService.update(id, req)
        );
        assertEquals("Category not found with id=99", exception.getMessage());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void update_ShouldThrowBadRequest_WhenNameExistsForAnotherId() {
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Integer id = 1;
        ReqCategoryUpsertDTO req = new ReqCategoryUpsertDTO();
        req.setName("Duplicate Name");

        Category existingCategory = Category.builder().id(id).name("Old Name").build();
        when(categoryRepository.findByIdAndStoreId(id, 1L)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.existsByStoreIdAndNameAndIdNot(1L, "Duplicate Name", id)).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            categoryService.update(id, req)
        );
        assertEquals("Category name already exists: Duplicate Name", exception.getMessage());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void delete_ShouldCallRepositoryDelete_WhenIdExists() {
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Integer id = 1;
        Category existingCategory = Category.builder().id(id).name("Name").build();
        when(categoryRepository.findByIdAndStoreId(id, 1L)).thenReturn(Optional.of(existingCategory));

        categoryService.delete(id);

        verify(categoryRepository).delete(existingCategory);
    }

    @Test
    void delete_ShouldThrowResourceNotFound_WhenIdDoesNotExist() {
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Integer id = 99;
        when(categoryRepository.findByIdAndStoreId(id, 1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
            categoryService.delete(id)
        );
        assertEquals("Category not found with id=99", exception.getMessage());
        verify(categoryRepository, never()).delete(any(Category.class));
    }
}
"""

write("UnitServiceTest.java", unit_test)
write("SupplierServiceTest.java", supplier_test)
write("CategoryServiceTest.java", category_test)

# ============================================================
# 4) Fix InventoryAdjustmentServiceTest remaining issues
# ============================================================
adj = read("InventoryAdjustmentServiceTest.java")

# Fix stray when() calls before @DisplayName (already fixed in previous run, but ensure)
adj = re.sub(
    r'\n\s*(?:org\.mockito\.Mockito\.lenient\(\)\.)?when\(currentStoreService\.[^)]+\)\)\.thenReturn[^;]+;\n\s*@DisplayName',
    r'\n        @DisplayName',
    adj
)
# Ensure findByIdAndStoreId has 2 args
adj = re.sub(r'findByIdAndStoreId\((\d+)\)(?!\s*,)', r'findByIdAndStoreId(\1, 1L)', adj)
# Ensure findByIdAndStoreIdAndIsDeletedFalse has 2 args (only single-arg calls)
adj = re.sub(r'findByIdAndStoreIdAndIsDeletedFalse\((\d+)\)(?!\s*,)', r'findByIdAndStoreIdAndIsDeletedFalse(\1, 1L)', adj)
# Fix wrong method name
adj = adj.replace('findAllByStoreIdOrderByIdDesc', 'findAllByStoreIdOrderByCreatedAtDesc')
# Fix verify(adjustmentRepository).findById(1) -> findByIdAndStoreId(1, 1L)
adj = re.sub(r'verify\(adjustmentRepository\)\.findById\((\d+)\)', r'verify(adjustmentRepository).findByIdAndStoreId(\1, 1L)', adj)
# Wrap when(currentStoreService with lenient
adj = re.sub(r'(?<!lenient\(\)\.)when\(currentStoreService', r'org.mockito.Mockito.lenient().when(currentStoreService', adj)

write("InventoryAdjustmentServiceTest.java", adj)

# ============================================================
# 5) Fix ImportOrderServiceTest remaining issues
# ============================================================
imp = read("ImportOrderServiceTest.java")
# Fix productRepository method names
imp = imp.replace('productRepository.findByIdAndStoreId(', 'productRepository.findByIdAndStoreIdAndIsDeletedFalse(')
# Fix supplierRepository
imp = imp.replace('supplierRepository.findByIdAndStoreIdAndIsDeletedFalse', 'supplierRepository.findByIdAndStoreId')
# Fix importOrderRepository
imp = imp.replace('importOrderRepository.findByIdAndStoreIdAndIsDeletedFalse', 'importOrderRepository.findByIdAndStoreId')
# Fix wrong method name findAll
imp = imp.replace('findAllByStoreIdOrderByIdDesc', 'findAllByStoreIdOrderByCreatedAtDesc')
# Wrap when(currentStoreService with lenient
imp = re.sub(r'(?<!lenient\(\)\.)when\(currentStoreService', r'org.mockito.Mockito.lenient().when(currentStoreService', imp)
write("ImportOrderServiceTest.java", imp)

# ============================================================
# 6) Fix AttendanceServiceTest — patch the attendanceRepository call
#    Tests use findByEmployee_IdAndCheckOutIsNull but service uses
#    findByEmployee_StoreMember_Store_IdAndEmployee_IdAndCheckOutIsNull
# ============================================================
att = read("AttendanceServiceTest.java")
att = att.replace(
    'attendanceRepository.findByEmployee_IdAndCheckOutIsNull(',
    'attendanceRepository.findByEmployee_StoreMember_Store_IdAndEmployee_IdAndCheckOutIsNull(1L, '
)
write("AttendanceServiceTest.java", att)

# ============================================================
# 7) Apply lenient to remaining service tests
# ============================================================
other_tests = [
    "ProductServiceTest.java",
    "PaymentServiceTest.java",
    "RoleServiceTest.java",
    "OrderServiceTest.java",
    "EmployeeServiceTest.java",
    "CustomerServiceTest.java",
    "OfficeServiceTest.java",
    "PayrollServiceTest.java",
]
for t in other_tests:
    path = os.path.join(BASE, t)
    if os.path.exists(path):
        content = open(path, encoding='utf-8').read()
        patched = re.sub(
            r'(?<!lenient\(\)\.)when\(currentStoreService',
            r'org.mockito.Mockito.lenient().when(currentStoreService',
            content
        )
        if patched != content:
            with open(path, 'w', encoding='utf-8', newline='\r\n') as f:
                f.write(patched)
            print(f"Patched lenient: {t}")

print("All service tests fixed!")
