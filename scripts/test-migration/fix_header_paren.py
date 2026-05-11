import glob, re, os

BASE = "backend/shoplite/src/test/java/com/quyen/shoplite/integration/"
files = [f for f in glob.glob(BASE + "*.java") if "IntegrationTestBase" not in f]

# We want to replace:
# .header("X-Store-Id", testStore.getId()))
# with
# ).header("X-Store-Id", testStore.getId())

PATTERN = re.compile(r'\.header\("X-Store-Id",\s*testStore\.getId\(\)\)\)')

def fix_header(m):
    return ').header("X-Store-Id", testStore.getId())'

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
