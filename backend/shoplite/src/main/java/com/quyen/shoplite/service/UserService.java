package com.quyen.shoplite.service;

import com.quyen.shoplite.repository.RoleRepository;
import com.quyen.shoplite.repository.UserRepository;
import com.quyen.shoplite.util.DTOMapper;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;

import com.quyen.shoplite.domain.Role;
import com.quyen.shoplite.domain.User;
import com.quyen.shoplite.domain.request.ReqUserDTO;
import com.quyen.shoplite.domain.response.ResUserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public ResUserDTO create(ReqUserDTO req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new BadRequestException("Username '" + req.getUsername() + "' Ä‘Ã£ tá»“n táº¡i");
        }
        User user = User.builder()
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .isActive(req.isActive())
                .build();
        return DTOMapper.toResUserDTO(userRepository.save(user));
    }

    public ResUserDTO findById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("KhÃ´ng tÃ¬m tháº¥y User vá»›i id=" + id));
        return DTOMapper.toResUserDTO(user);
    }

    public List<ResUserDTO> findAll() {
        return userRepository.findAll().stream()
                .map(DTOMapper::toResUserDTO)
                .toList();
    }

    public ResUserDTO update(Integer id, ReqUserDTO req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("KhÃ´ng tÃ¬m tháº¥y User vá»›i id=" + id));


        user.setActive(req.isActive());
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        return DTOMapper.toResUserDTO(userRepository.save(user));
    }

    public void delete(Integer id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("KhÃ´ng tÃ¬m tháº¥y User vá»›i id=" + id);
        }
        userRepository.deleteById(id);
    }

    public User findEntityByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("KhÃ´ng tÃ¬m tháº¥y User: " + username));
    }

    private Role resolveRole(Long roleId) {
        if (roleId == null) {
            return null;
        }
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("KhÃ´ng tÃ¬m tháº¥y Role id=" + roleId));
    }
}
