import glob, re, os

BASE = "backend/shoplite/src/test/java/com/quyen/shoplite/integration/"
files = [f for f in glob.glob(BASE + "*.java") if "IntegrationTestBase" not in f]

for fpath in files:
    with open(fpath, encoding="utf-8") as f:
        content = f.read()
    
    original = content
    
    # Split by mockMvc.perform(
    parts = content.split("mockMvc.perform(")
    if len(parts) > 1:
        new_content = parts[0]
        for i in range(1, len(parts)):
            part = parts[i]
            # find the first closing parenthesis of the HTTP method call.
            # Usually it's something like `get("/api/v1/employees/" + emp.getId())`
            # or `post("/api/v1/employees")`
            # We can track parentheses to find the closing one.
            
            paren_count = 1 # We start after `perform(`
            method_end_idx = -1
            
            for j, char in enumerate(part):
                if char == '(':
                    paren_count += 1
                elif char == ')':
                    paren_count -= 1
                    if paren_count == 0:
                        method_end_idx = j
                        break
                        
            if method_end_idx != -1:
                # check if there's already a header
                method_call = part[:method_end_idx+1]
                rest = part[method_end_idx+1:]
                
                # Check if rest starts with .header("X-Store-Id", testStore.getId())
                # Or if method_call already has it (though it shouldn't be inside the method call)
                
                # To be safe, look at the next 50 chars
                if '.header("X-Store-Id"' not in rest[:150] and '.header("X-Store-Id"' not in method_call:
                    part = part[:method_end_idx+1] + '.header("X-Store-Id", testStore.getId())' + part[method_end_idx+1:]
            
            new_content += "mockMvc.perform(" + part
            
        if new_content != original:
            with open(fpath, "w", encoding="utf-8", newline="\n") as f:
                f.write(new_content)
            print(f"Added headers to {os.path.basename(fpath)}")

print("Done fixing missing headers.")
