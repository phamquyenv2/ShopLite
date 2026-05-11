import glob

files = glob.glob('src/test/java/com/quyen/shoplite/integration/*IntegrationTest.java')

for filepath in sorted(files):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    lines = content.split('\n')
    new_lines = []
    i = 0
    changes = 0

    while i < len(lines):
        line = lines[i]

        if 'mockMvc.perform(' in line and 'withStore' not in line:
            mp_idx = line.index('mockMvc.perform(')
            before = line[:mp_idx]
            after_perform = line[mp_idx + len('mockMvc.perform('):]

            # Collect all lines that are part of this perform() call
            # We're inside perform(, so start depth at 1
            collected = []
            depth = 1
            j = i

            while j < len(lines):
                if j == i:
                    text = after_perform
                else:
                    text = lines[j]

                for ch in text:
                    if ch == '(':
                        depth += 1
                    elif ch == ')':
                        depth -= 1

                collected.append(text)

                if depth <= 0:
                    break
                j += 1

            if depth > 0:
                new_lines.append(line)
                i += 1
                continue

            # collected has all lines from after perform( to its closing )
            # Wrap with withStore: add 'withStore(' at start, extra ')' before closing
            new_collected = list(collected)
            new_collected[0] = 'withStore(' + new_collected[0]

            # Add extra ) before the last ) on last line
            last = new_collected[-1]
            last_close_idx = last.rfind(')')
            if last_close_idx >= 0:
                new_collected[-1] = last[:last_close_idx] + ')' + last[last_close_idx:]

            # Rebuild lines
            result = [before + 'mockMvc.perform(' + new_collected[0]]
            result.extend(new_collected[1:])
            new_lines.extend(result)
            changes += 1
            i = j + 1
        else:
            new_lines.append(line)
            i += 1

    if changes > 0:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write('\n'.join(new_lines))
        print(f'{filepath}: {changes} changes')
    else:
        print(f'{filepath}: no changes needed')
