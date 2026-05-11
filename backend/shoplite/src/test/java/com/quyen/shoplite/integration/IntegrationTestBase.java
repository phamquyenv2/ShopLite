package com.quyen.shoplite.integration;

import com.quyen.shoplite.domain.Store;
import com.quyen.shoplite.domain.StoreMember;
import com.quyen.shoplite.domain.User;
import com.quyen.shoplite.repository.StoreMemberRepository;
import com.quyen.shoplite.repository.StoreRepository;
import com.quyen.shoplite.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for all Controller integration tests.
 *
 * Seeds a Store + User (matching @WithMockUser username) + StoreMember
 * so that CurrentStoreService.getCurrentStore() resolves correctly.
 *
 * Every subclass MUST use withStore() helper or add X-Store-Id header
 * to all requests that invoke CurrentStoreService.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = IntegrationTestBase.TEST_USERNAME)
public abstract class IntegrationTestBase {

    /** Username used by @WithMockUser — must match what's seeded in DB */
    public static final String TEST_USERNAME = "it_user";

    @Autowired protected MockMvc mockMvc;

    @Autowired private UserRepository userRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private StoreMemberRepository storeMemberRepository;

    protected Store testStore;
    protected User testUser;
    protected StoreMember testStoreMember;

    @BeforeEach
    void setUpStoreContext() {
        // Create test user matching @WithMockUser username
        testUser = userRepository.findByUsername(TEST_USERNAME).orElseGet(() ->
            userRepository.save(User.builder()
                .username(TEST_USERNAME)
                .password("$2a$10$NopH3V6EZgPBRPmyoFtSdeWcR4AoS0Vb0xIAV8MYb6qO0PvXKlZAO")
                .isActive(true)
                .build())
        );

        // Create test store owned by test user
        testStore = storeRepository.save(Store.builder()
            .name("IT Store")
            .owner(testUser)
            .build());

        // Link user to store as active member
        testStoreMember = storeMemberRepository.save(StoreMember.builder()
            .store(testStore)
            .user(testUser)
            .status(com.quyen.shoplite.util.constant.StoreMemberStatus.ACTIVE)
            .build());
    }

    /**
     * Add X-Store-Id header to a MockMvc request builder.
     * Call this on every request so CurrentStoreService can resolve the store.
     */
    protected MockHttpServletRequestBuilder withStore(MockHttpServletRequestBuilder builder) {
        return builder.header("X-Store-Id", testStore.getId());
    }
}
