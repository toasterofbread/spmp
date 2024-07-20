import os
import re

STRING_REGEX = r"getString\(\"([^\"]+)\"\)"
STRING_REGEX_REPLACEMENT = r"stringResource(Res.string.\1)"

def formatStringKey(key: str) -> str:
    if "$" in key:
        return f"`{key}`"

    return key

def getImportsForReplacements(replacements: list[re.Match]) -> list[str]:
    imports = [
        "org.jetbrains.compose.resources.stringResource",
        "spmp.shared.generated.resources.Res"
    ]

    for replacement in replacements:
        call = replacement.string[replacement.start() : replacement.end()]
        string_key = call[11:-2]

        imports.append(f"spmp.shared.generated.resources.{formatStringKey(string_key)}")

    return [f"import {imp}" for imp in imports]

def processKotlinFile(path: str):
    f = open(path, "r")
    content = f.read()
    f.close()

    replacements = list(re.finditer(STRING_REGEX, content))
    if len(replacements) == 0:
        return

    content = re.sub(STRING_REGEX, STRING_REGEX_REPLACEMENT, content)

    imports_start_line = None
    package_line = None

    lines = content.split("\n")

    for index, line in enumerate(lines):
        line = line.strip()
        if line.startswith("package "):
            package_line = index
        elif line.startswith("import "):
            imports_start_line = index

    if imports_start_line is None:
        imports_start_line = package_line + 1
    else:
        imports_start_line += 1

    for index, import_line in enumerate(getImportsForReplacements(replacements)):
        lines.insert(imports_start_line + index, import_line)

    f = open(path, "w")
    f.write("\n".join(lines))
    f.close()

def main():
    for root, dirs, files in os.walk("."):
        for file in files:
            if file.endswith(".kt"):
                processKotlinFile(os.path.join(root, file))

if __name__ == "__main__":
    main()
