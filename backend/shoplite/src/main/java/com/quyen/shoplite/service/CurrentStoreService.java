package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.Store;
import com.quyen.shoplite.domain.StoreMember;
import com.quyen.shoplite.domain.User;
import com.quyen.shoplite.repository.StoreMemberRepository;
import com.quyen.shoplite.repository.StoreRepository;
import com.quyen.shoplite.repository.UserRepository;
import com.quyen.shoplite.util.SecurityUtil;
import com.quyen.shoplite.util.constant.StoreMemberStatus;
import com.quyen.shoplite.util.error.ResourceNotFoundException;
import com.quyen.shoplite.util.error.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CurrentStoreService {

    public static final String STORE_HEADER = "X-Store-Id";

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final StoreMemberRepository storeMemberRepository;

    @Transactional(readOnly = true)
    public Long getCurrentStoreId() {
        return getCurrentStoreMembership().getStore().getId();
    }

    @Transactional(readOnly = true)
    public Store getCurrentStore() {
        return getCurrentStoreMembership().getStore();
    }

    @Transactional(readOnly = true)
    public StoreMember getCurrentStoreMembership() {
        User user = getCurrentUser();
        Long requestedStoreId = readStoreIdHeader();

        List<StoreMember> memberships = storeMemberRepository
                .findAllByUserIdAndStatusFetchStore(user.getId(), StoreMemberStatus.ACTIVE);

        if (memberships.isEmpty()) {
            throw new UnauthorizedException("Current user does not belong to any active store");
        }

        if (requestedStoreId == null) {
            return memberships.get(0);
        }

        return memberships.stream()
                .filter(member -> member.getStore().getId().equals(requestedStoreId))
                .findFirst()
                .orElseThrow(() -> new UnauthorizedException("Current user cannot access store id=" + requestedStoreId));
    }

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        String username = SecurityUtil.requireCurrentUserLogin();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Current user was not found: " + username));
    }

    @Transactional(readOnly = true)
    public Store getStoreReference(Long storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with id=" + storeId));
    }

    private Long readStoreIdHeader() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        String rawStoreId = request.getHeader(STORE_HEADER);
        if (rawStoreId == null || rawStoreId.isBlank()) {
            return null;
        }

        try {
            return Long.valueOf(rawStoreId.trim());
        } catch (NumberFormatException ex) {
            throw new UnauthorizedException("Invalid X-Store-Id header");
        }
    }
}
