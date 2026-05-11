"""
Rewrite all integration tests to:
1. Remove @SpringBootTest etc. class annotations (inherited from IntegrationTestBase)
2. Remove @Autowired MockMvc field (inherited)
3. extends IntegrationTestBase
4. Add withStore() to every mockMvc.perform() call
5. Add .store(testStore) to entity builders that require store
"""
import re, os

BASE = "backend/shoplite/src/test/java/com/quyen/shoplite/integration/"

def read(n): return open(BASE + n, encoding='utf-8').read()
def write(n, c):
    with open(BASE + n, 'w', encoding='utf-8', newline='\r\n') as f: f.write(c)
    print(f"  OK: {n}")

CLASS_ANNOTATIONS = [
    "@SpringBootTest", "@AutoConfigureMockMvc",
    '@ActiveProfiles("test")', "@Transactional", "@WithMockUser",
]

def remove_class_anns(c):
    for a in CLASS_ANNOTATIONS:
        # Remove full annotation line (with possible @WithMockUser(username=...))
        c = re.sub(re.escape(a) + r'[^\r\n]*\r?\n', '', c)
    return c

def remove_mockmvc(c):
    c = re.sub(r'[ \t]*@Autowired\r?\n[ \t]*private MockMvc mockMvc;\r?\n', '', c)
    return c

def extend_base(c, classname):
    c = re.sub(rf'class {classname}\s*\{{', f'class {classname} extends IntegrationTestBase {{', c)
    return c

def clean_imports(c):
    remove = [
        "import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;",
        "import org.springframework.boot.test.context.SpringBootTest;",
        "import org.springframework.test.context.ActiveProfiles;",
        "import org.springframework.transaction.annotation.Transactional;",
        "import org.springframework.security.test.context.support.WithMockUser;",
        "import org.springframework.test.web.servlet.MockMvc;",
    ]
    for r in remove:
        c = c.replace(r + '\r\n', '').replace(r + '\n', '')
    return c

def add_with_store(c):
    """Wrap mockMvc.perform(METHOD( calls with withStore()."""
    # Step 1: wrap the method call
    c = re.sub(
        r'mockMvc\.perform\((get|post|put|delete|patch|multipart)\(',
        r'mockMvc.perform(withStore(\1(',
        c
    )
    # Step 2: close the extra paren before .andExpect / .andReturn
    # Pattern: two or more closing parens at end of argument list before .andExpect/.andReturn
    # We need to turn ))  into )))
    # The perform call chain ends with )).\nandExpect or )).andExpect on same line
    # Simple approach: after adding withStore(), fix the closing:
    # Before: mockMvc.perform(withStore(get("/api")))  -> correct if it was get("/api"))
    # The tricky case is multi-line perform with .contentType()...content()...)
    # We rely on the fact that .andExpect/.andReturn come after the perform block
    c = re.sub(r'\)\)\r?\n(\s*\.andExpect)', r')))\n\1', c)
    c = re.sub(r'\)\)\r?\n(\s*\.andReturn)', r')))\n\1', c)
    # Single-line: .content(x)).andExpect → .content(x))).andExpect
    c = re.sub(r'\)\)\.andExpect', r'))).andExpect', c)
    c = re.sub(r'\)\)\.andReturn', r'))).andReturn', c)
    return c

def add_store_to_builders(c, entities):
    """Add .store(testStore) before .build() for listed entity names."""
    for entity in entities:
        # Match EntityName.builder()...(.build()) where .store( is not already there
        pattern = rf'({re.escape(entity)}\.builder\(\)(?:(?!\.build\(\)).)*?)(\.build\(\))'
        def replacer(m):
            full = m.group(0)
            if '.store(' in full:
                return full  # already has store
            return m.group(1) + '\n                .store(testStore)' + m.group(2)
        c = re.sub(pattern, replacer, c, flags=re.DOTALL)
    return c

def process(filename, entities_needing_store=None):
    classname = filename.replace('.java', '')
    c = read(filename)
    c = remove_class_anns(c)
    c = clean_imports(c)
    c = remove_mockmvc(c)
    c = extend_base(c, classname)
    c = add_with_store(c)
    if entities_needing_store:
        c = add_store_to_builders(c, entities_needing_store)
    write(filename, c)

# Process simple files
print("Processing simple entity tests...")
process("UnitControllerIntegrationTest.java")  # Unit has no store_id requirement
process("PermissionControllerIntegrationTest.java")
process("RoleControllerIntegrationTest.java")
process("UserControllerIntegrationTest.java")

print("Processing entity tests with store requirement...")
process("CustomerControllerIntegrationTest.java", ["Customer"])
process("SupplierControllerIntegrationTest.java", ["Supplier"])
process("OfficeControllerIntegrationTest.java", ["Office"])
process("ProductControllerIntegrationTest.java", ["Product"])
process("OrderControllerIntegrationTest.java", ["Customer", "Category", "Product"])
process("ImportOrderControllerIntegrationTest.java", ["Supplier", "Category", "Product", "ImportOrder"])
process("EmployeeControllerIntegrationTest.java")
process("AttendanceControllerIntegrationTest.java")
process("AuthControllerIntegrationTest.java")

print("\nAll done.")
