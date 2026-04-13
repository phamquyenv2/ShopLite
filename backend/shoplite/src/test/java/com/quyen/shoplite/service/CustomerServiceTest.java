package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.Customer;
import com.quyen.shoplite.domain.request.ReqCustomerUpsertDTO;
import com.quyen.shoplite.domain.response.ResCustomerDTO;
import com.quyen.shoplite.repository.CustomerRepository;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;
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

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void create_ShouldReturnCustomer_WhenPhoneIsUnique() {
        // Arrange
        ReqCustomerUpsertDTO req = new ReqCustomerUpsertDTO();
        req.setName("  John Doe  ");
        req.setPhone("0987654321");

        when(customerRepository.existsByPhone("0987654321")).thenReturn(false);

        Customer savedCustomer = Customer.builder()
                .id(1)
                .name("John Doe")
                .phone("0987654321")
                .points(0)
                .build();
        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);

        // Act
        ResCustomerDTO result = customerService.create(req);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("John Doe", result.getName());
        assertEquals("0987654321", result.getPhone());
        assertEquals(0, result.getPoints());
        verify(customerRepository).save(argThat(c -> c.getName().equals("John Doe") && c.getPhone().equals("0987654321")));
    }

    @Test
    void create_ShouldThrowBadRequest_WhenPhoneExists() {
        // Arrange
        ReqCustomerUpsertDTO req = new ReqCustomerUpsertDTO();
        req.setName("John");
        req.setPhone("0987654321");

        when(customerRepository.existsByPhone("0987654321")).thenReturn(true);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> 
            customerService.create(req)
        );
        assertEquals("Phone already exists: 0987654321", exception.getMessage());
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void update_ShouldReturnCustomer_WhenIdExistsAndPhoneIsUnique() {
        // Arrange
        Integer id = 1;
        ReqCustomerUpsertDTO req = new ReqCustomerUpsertDTO();
        req.setName("Updated John");
        req.setPhone("0999999999");

        Customer existingCustomer = Customer.builder().id(id).name("Old Name").phone("Old Phone").build();
        when(customerRepository.findById(id)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.existsByPhoneAndIdNot("0999999999", id)).thenReturn(false);

        Customer savedCustomer = Customer.builder().id(id).name("Updated John").phone("0999999999").build();
        when(customerRepository.save(existingCustomer)).thenReturn(savedCustomer);

        // Act
        ResCustomerDTO result = customerService.update(id, req);

        // Assert
        assertNotNull(result);
        assertEquals("Updated John", result.getName());
        assertEquals("0999999999", result.getPhone());
        verify(customerRepository).save(existingCustomer);
    }

    @Test
    void update_ShouldThrowResourceNotFound_WhenIdDoesNotExist() {
        // Arrange
        Integer id = 99;
        ReqCustomerUpsertDTO req = new ReqCustomerUpsertDTO();
        req.setName("Updated John");

        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> 
            customerService.update(id, req)
        );
        assertEquals("Customer not found with id=99", exception.getMessage());
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void update_ShouldThrowBadRequest_WhenPhoneExistsForAnotherId() {
        // Arrange
        Integer id = 1;
        ReqCustomerUpsertDTO req = new ReqCustomerUpsertDTO();
        req.setName("John");
        req.setPhone("0987654321");

        Customer existingCustomer = Customer.builder().id(id).name("Old Name").build();
        when(customerRepository.findById(id)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.existsByPhoneAndIdNot("0987654321", id)).thenReturn(true);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> 
            customerService.update(id, req)
        );
        assertEquals("Phone already exists: 0987654321", exception.getMessage());
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void delete_ShouldCallRepositoryDelete_WhenIdExists() {
        // Arrange
        Integer id = 1;
        Customer existingCustomer = Customer.builder().id(id).name("John").build();
        when(customerRepository.findById(id)).thenReturn(Optional.of(existingCustomer));

        // Act
        customerService.delete(id);

        // Assert
        verify(customerRepository).delete(existingCustomer);
    }

    @Test
    void delete_ShouldThrowResourceNotFound_WhenIdDoesNotExist() {
        // Arrange
        Integer id = 99;
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> 
            customerService.delete(id)
        );
        assertEquals("Customer not found with id=99", exception.getMessage());
        verify(customerRepository, never()).delete(any(Customer.class));
    }
}
