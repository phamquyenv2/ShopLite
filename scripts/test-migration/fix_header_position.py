"""
Fix: move .header("X-Store-Id", testStore.getId()) from AFTER perform() 
     to INSIDE perform() (on the request builder, not ResultActions).

Pattern to fix:
  mockMvc.perform(someCall()).header("X-Store-Id", testStore.getId())

Should become:
  mockMvc.perform(someCall().header("X-Store-Id", testStore.getId()))
"""

import glob, re, os

BASE = "backend/shoplite/src/test/java/com/quyen/shoplite/integration/"
files = [f for f in glob.glob(BASE + "*.java") if "IntegrationTestBase" not in f]

PATTERN = re.compile(
    r'(mockMvc\.perform\()(.+?)(\))'     # group1=perform(, group2=inner, group3=)
    r'(\.header\("X-Store-Id",\s*testStore\.getId\(\)\))',  # group4=the misplaced header
    re.DOTALL
)

def fix_header(m):
    # Move the header call inside before the closing paren
    return m.group(1) + m.group(2) + m.group(4) + m.group(3)

total_fixes = 0
for fpath in sorted(files):
    with open(fpath, encoding="utf-8") as f:
        content = f.read()

    new_content, count = PATTERN.subn(fix_header, content)
    if count:
        with open(fpath, "w", encoding="utf-8", newline="\n") as f:
            f.write(new_content)
        print(f"  Fixed {count} occurrence(s) in {os.path.basename(fpath)}")
        total_fixes += count

print(f"\nTotal fixes: {total_fixes}")
