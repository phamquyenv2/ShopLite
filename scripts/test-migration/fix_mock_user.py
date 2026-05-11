import glob, re

BASE = "backend/shoplite/src/test/java/com/quyen/shoplite/integration/"
files = [f for f in glob.glob(BASE + "*.java") if "IntegrationTestBase" not in f]

for fpath in files:
    with open(fpath, encoding="utf-8") as f:
        c = f.read()
    
    # 1. Remove all @WithMockUser annotations
    c = re.sub(r'[ \t]*@WithMockUser[^\n]*\n', '', c)
    
    # 2. Fix the duplicate setup in Attendance
    if "AttendanceControllerIntegrationTest.java" in fpath:
        # We need to replace the setup block that creates User, Store, StoreMember
        # with just reusing testStoreMember.
        # Replace:
        #         User user = new User();
        #         ...
        #         member = storeMemberRepository.save(member);
        old_setup = """        // 3. Setup User matching the @WithMockUser
        User user = new User();
        user.setUsername("attendance_test_user");
        user.setPassword("secret");
        user.setActive(true);
        user = userRepository.save(user);
        Store store = Store.builder()
                .name("Attendance Store " + System.nanoTime())
                .owner(user)
                .build();
        store = storeRepository.save(store);
        StoreMember member = StoreMember.builder()
                .store(store)
                .user(user)
                .role(role)
                .build();
        member = storeMemberRepository.save(member);"""
        
        new_setup = """        // 3. Reuse testStoreMember from IntegrationTestBase
        testStoreMember.setRole(role);
        testStoreMember = storeMemberRepository.save(testStoreMember);
        Store store = testStore; // Map store to testStore for Office and Employee setup"""
        
        c = c.replace(old_setup, new_setup)
        
    if "PayrollControllerIntegrationTest.java" in fpath:
        old_setup = """        // 3. Setup User matching the @WithMockUser
        User user = new User();
        user.setUsername("payroll_test_user");
        user.setPassword("secret");
        user.setActive(true);
        user = userRepository.save(user);
        Store store = Store.builder()
                .name("Payroll Store " + System.nanoTime())
                .owner(user)
                .build();
        store = storeRepository.save(store);
        StoreMember member = StoreMember.builder()
                .store(store)
                .user(user)
                .role(role)
                .build();
        member = storeMemberRepository.save(member);"""
        
        new_setup = """        // 3. Reuse testStoreMember from IntegrationTestBase
        testStoreMember.setRole(role);
        testStoreMember = storeMemberRepository.save(testStoreMember);
        Store store = testStore; // Map store to testStore for Office and Employee setup"""
        
        c = c.replace(old_setup, new_setup)
        
    with open(fpath, "w", encoding="utf-8", newline="\n") as f:
        f.write(c)

print("Done cleaning up mock users and duplicate setups.")
