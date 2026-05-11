"""
Final fix:
1. Add .store(testStore) to Category.builder() and Customer.builder() saves that are missing store
2. Fix mockMvc.perform() calls where URL contains + variable and missing X-Store-Id header
"""
import re, glob, os

BASE = "backend/shoplite/src/test/java/com/quyen/shoplite/integration/"
files = [f for f in glob.glob(BASE + "*.java") if "IntegrationTestBase" not in f]

def fix(fpath):
    name = os.path.basename(fpath)
    with open(fpath, encoding="utf-8") as f:
        content = f.read()
    original = content

    HTTP_METHODS = r'(?:get|post|put|delete|patch|multipart)'

    # Fix 1: Add X-Store-Id to performs with dynamic URL (containing + variable)
    # Pattern: METHOD("/api/.." + var))\r\n  .andExpect  -- closing )) means perform(METHOD(...))
    # Need to insert header before the last ) of METHOD call
    # post("/url/" + id) → post("/url/" + id).header("X-Store-Id", testStore.getId())
    
    # Case: METHOD("/url" + var)\r\n  .contentType(...)
    content = re.sub(
        rf'({HTTP_METHODS}\("[^"]*"\s*\+\s*[^)]+\))\r?\n(\s*\.(?:contentType|content|param|accept))',
        r'\1.header("X-Store-Id", testStore.getId())\r\n\2',
        content
    )
    # Case: METHOD("/url" + var))\r\n  .andExpect  -- ) is perform close
    content = re.sub(
        rf'({HTTP_METHODS}\("[^"]*"\s*\+\s*[^)]+\))\)\r?\n(\s*\.andExpect)',
        r'\1.header("X-Store-Id", testStore.getId()))\r\n\2',
        content
    )
    # Case: simple METHOD("/url" + var)) on same line as andExpect  
    content = re.sub(
        rf'({HTTP_METHODS}\("[^"]*"\s*\+\s*[^)]+\))\)\.andExpect',
        r'\1.header("X-Store-Id", testStore.getId())).andExpect',
        content
    )
    # Case: .param("key", ...) or )\r\n    .andReturn
    content = re.sub(
        rf'({HTTP_METHODS}\("[^"]*"\s*\+\s*[^)]+\))\)\r?\n(\s*\.andReturn)',
        r'\1.header("X-Store-Id", testStore.getId()))\r\n\2',
        content
    )

    # Fix 2: Add .store(testStore) to entity builders without store
    ENTITIES_NEEDING_STORE = [
        "Category", "Customer", "Supplier", "Product", "Order"
    ]
    for entity in ENTITIES_NEEDING_STORE:
        # Match: EntityName.builder()\n  .name(...)\n  .build()
        # But NOT if .store( already present
        pattern = rf'({re.escape(entity)}\.builder\(\))((?:(?!\.(store|build)\()[\s\S])*?)(\.build\(\))'
        def add_store(m):
            if '.store(' in m.group(0):
                return m.group(0)
            return m.group(1) + m.group(2) + '\n                .store(testStore)' + m.group(4)
        content = re.sub(pattern, add_store, content)

    if content != original:
        with open(fpath, "w", encoding="utf-8", newline="\r\n") as f:
            f.write(content)
        print(f"  Fixed: {name}")
    else:
        print(f"  No change: {name}")

for f in files:
    fix(f)
print("Done.")
