package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.Customer;
import com.quyen.shoplite.domain.request.ReqCustomerUpsertDTO;
import com.quyen.shoplite.domain.response.ResCustomerDTO;
import com.quyen.shoplite.repository.CustomerRepository;
import com.quyen.shoplite.util.DTOMapper;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public ResCustomerDTO create(ReqCustomerUpsertDTO req) {
        String phone = normalize(req.getPhone());
        if (customerRepository.existsByPhone(phone)) {
            throw new BadRequestException("Phone already exists: " + phone);
        }

        Customer customer = Customer.builder()
                .name(req.getName().trim())
                .phone(phone)
                .build();
        if (customer.getPoints() == null) {
            customer.setPoints(0);
        }
        return DTOMapper.toResCustomerDTO(customerRepository.save(customer));
    }

    public ResCustomerDTO findById(Integer id) {
        return DTOMapper.toResCustomerDTO(findEntityById(id));
    }

    public List<ResCustomerDTO> findAll() {
        return customerRepository.findAll().stream()
                .map(DTOMapper::toResCustomerDTO)
                .toList();
    }

    @Transactional
    public ResCustomerDTO update(Integer id, ReqCustomerUpsertDTO req) {
        Customer customer = findEntityById(id);
        if (req.getVersion() != null && !req.getVersion().equals(customer.getVersion())) {
            throw new BadRequestException("Customer has been modified by another user. Please refresh and try again.");
        }
        String phone = normalize(req.getPhone());
        if (customerRepository.existsByPhoneAndIdNot(phone, id)) {
            throw new BadRequestException("Phone already exists: " + phone);
        }

        customer.setName(req.getName().trim());
        customer.setPhone(phone);
        return DTOMapper.toResCustomerDTO(customerRepository.save(customer));
    }

    @Transactional
    public void delete(Integer id) {
        Customer customer = findEntityById(id);
        customerRepository.delete(customer);
    }

    private Customer findEntityById(Integer id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id=" + id));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /**
     * POS quick search — find customers by partial phone number.
     */
    public List<ResCustomerDTO> searchByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return List.of();
        }
        return customerRepository.findByPhoneContaining(phone.trim()).stream()
                .map(DTOMapper::toResCustomerDTO)
                .toList();
    }
}
