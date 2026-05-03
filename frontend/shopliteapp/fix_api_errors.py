import os
import re

src_dir = r"d:\ShopLite_v1\ShopLite\frontend\shopliteapp\src"

for root, dirs, files in os.walk(src_dir):
    for file in files:
        if file.endswith(".ts") or file.endswith(".tsx"):
            filepath = os.path.join(root, file)
            with open(filepath, "r", encoding="utf-8") as f:
                content = f.read()

            new_content = content
            # Fix authApis().get( -> authApis().get<any>(
            new_content = re.sub(r'authApis\(\)\.(get|post|put|patch|delete)\(', r'authApis().\1<any>(', new_content)
            
            # Fix throw new ApiError(message) -> throw new ApiError(message, error.response || { status: 500, data: null, headers: new Headers() })
            # Matches one-arg constructor.
            new_content = re.sub(r'throw new ApiError\(([^,]+)\);', r'throw new ApiError(\1, error.response || { status: 500, data: null, headers: new Headers() });', new_content)
            
            if new_content != content:
                with open(filepath, "w", encoding="utf-8") as f:
                    f.write(new_content)
                print(f"Fixed {filepath}")
