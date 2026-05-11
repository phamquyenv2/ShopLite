package com.quyen.shoplite.service;

import com.quyen.shoplite.repository.CustomerRepository;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;

import com.quyen.shoplite.domain.Customer;
import com.quyen.shoplite.domain.Store;
import com.quyen.shoplite.domain.request.ReqCustomerUpsertDTO;
import com.quyen.shoplite.domain.response.ResCustomerDTO;

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
class CustomerServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private CurrentStoreService currentStoreService;

    @InjectMocks
    private CustomerService customerService;

    private Store testStore() {
        Store store = new Store();
        store.setId(1L);
        return store;
    }

    @Test
    void create_ShouldReturnCustomer_WhenPhoneIsUnique() {
        when(currentStoreService.getCurrentStore()).thenReturn(testStore());
        when(customerRepository.existsByStoreIdAndPhone(1L, "0987654321")).thenReturn(false);

        ReqCustomerUpsertDTO req = new ReqCustomerUpsertDTO();
        req.setName("  John Doe  ");
        req.setPhone("0987654321");

        Customer savedCustomer = Customer.builder()
                .id(1).name("John Doe").phone("0987654321").points(0).build();
        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);

        ResCustomerDTO result = customerService.create(req);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("John Doe", result.getName());
        assertEquals("0987654321", result.getPhone());
        assertEquals(0, result.getPoints());
        verify(customerRepository).save(argThat(c ->
            c.getName().equals("John Doe") && c.getPhone().equals("0987654321")));
    }

    @Test
    void create_ShouldThrowBadRequest_WhenPhoneExists() {
        when(currentStoreService.getCurrentStore()).thenReturn(testStore());
        when(customerRepository.existsByStoreIdAndPhone(1L, "0987654321")).thenReturn(true);

        ReqCustomerUpsertDTO req = new ReqCustomerUpsertDTO();
        req.setName("John");
        req.setPhone("0987654321");

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            customerService.create(req));
        assertEquals("Phone already exists: 0987654321", exception.getMessage());
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void update_ShouldReturnCustomer_WhenIdExistsAndPhoneIsUnique() {
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Integer id = 1;
        ReqCustomerUpsertDTO req = new ReqCustomerUpsertDTO();
        req.setName("Updated John");
        req.setPhone("0999999999");

        Customer existingCustomer = Customer.builder().id(id).name("Old Name").phone("Old Phone").build();
        when(customerRepository.findByIdAndStoreId(id, 1L)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.existsByStoreIdAndPhoneAndIdNot(1L, "0999999999", id)).thenReturn(false);

        Customer savedCustomer = Customer.builder().id(id).name("Updated John").phone("0999999999").build();
        when(customerRepository.save(existingCustomer)).thenReturn(savedCustomer);

        ResCustomerDTO result = customerService.update(id, req);

        assertNotNull(result);
        assertEquals("Updated John", result.getName());
        assertEquals("0999999999", result.getPhone());
        verify(customerRepository).save(existingCustomer);
    }

    @Test
    void update_ShouldThrowResourceNotFound_WhenIdDoesNotExist() {
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Integer id = 99;
        ReqCustomerUpsertDTO req = new ReqCustomerUpsertDTO();
        req.setName("Updated John");

        when(customerRepository.findByIdAndStoreId(id, 1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
            customerService.update(id, req));
        assertEquals("Customer not found with id=99", exception.getMessage());
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void update_ShouldThrowBadRequest_WhenPhoneExistsForAnotherId() {
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Integer id = 1;
        ReqCustomerUpsertDTO req = new ReqCustomerUpsertDTO();
        req.setName("John");
        req.setPhone("0987654321");

        Customer existingCustomer = Customer.builder().id(id).name("Old Name").build();
        when(customerRepository.findByIdAndStoreId(id, 1L)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.existsByStoreIdAndPhoneAndIdNot(1L, "0987654321", id)).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            customerService.update(id, req));
        assertEquals("Phone already exists: 0987654321", exception.getMessage());
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void delete_ShouldCallRepositoryDelete_WhenIdExists() {
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Integer id = 1;
        Customer existingCustomer = Customer.builder().id(id).name("John").build();
        when(customerRepository.findByIdAndStoreId(id, 1L)).thenReturn(Optional.of(existingCustomer));

        customerService.delete(id);

        verify(customerRepository).delete(existingCustomer);
    }

    @Test
    void delete_ShouldThrowResourceNotFound_WhenIdDoesNotExist() {
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Integer id = 99;
        when(customerRepository.findByIdAndStoreId(id, 1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
            customerService.delete(id));
        assertEquals("Customer not found with id=99", exception.getMessage());
        verify(customerRepository, never()).delete(any(Customer.class));
    }
}
