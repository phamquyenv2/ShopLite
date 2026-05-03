package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.Menu;
import com.quyen.shoplite.domain.Permission;
import com.quyen.shoplite.domain.Role;
import com.quyen.shoplite.domain.response.ResMenuDTO;
import com.quyen.shoplite.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;

    @Transactional(readOnly = true)
    public List<ResMenuDTO> getVisibleMenus(Role role) {
        Set<Long> rolePermissionIds = new HashSet<>();
        if (role != null && role.getPermissions() != null) {
            role.getPermissions().stream()
                    .map(Permission::getId)
                    .forEach(rolePermissionIds::add);
        }

        return menuRepository.findAllByActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc().stream()
                .filter(menu -> canSee(menu, rolePermissionIds))
                .map(this::toDto)
                .toList();
    }

    private boolean canSee(Menu menu, Set<Long> rolePermissionIds) {
        if (menu.getPermissions() == null || menu.getPermissions().isEmpty()) {
            return true;
        }
        return menu.getPermissions().stream()
                .map(Permission::getId)
                .anyMatch(rolePermissionIds::contains);
    }

    private ResMenuDTO toDto(Menu menu) {
        return ResMenuDTO.builder()
                .id(menu.getId())
                .code(menu.getCode())
                .title(menu.getTitle())
                .route(menu.getRoute())
                .icon(menu.getIcon())
                .menuType(menu.getMenuType())
                .parentId(menu.getParent() != null ? menu.getParent().getId() : null)
                .sortOrder(menu.getSortOrder())
                .build();
    }
}
