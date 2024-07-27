import os
from xmltodict import parse as xmltodict

TODO_COMMENT = "<!--TODO-->"

ASSETS_DIR = "shared/src/commonMain/composeResources/"
BASE_STRINGS_XML_PATH = "shared/src/commonMain/composeResources/values/strings.xml"

UNTRANSLATABLE_MATCH = "translatable=\"false\""
STRING_START = "    <string name=\""
STRING_ARRAY_START = "    <string-array name=\""
STRING_ARRAY_ITEM_START = "        <item>"
STRING_ARRAY_ITEM_END = "</item>"

def isPathInPath(path: str, in_path: str):
    return os.path.realpath(path).startswith(os.path.realpath(in_path) + os.sep)

def organiseAllStrings(assets_dir: str = ASSETS_DIR, base_xml_path: str = BASE_STRINGS_XML_PATH, stage_files: bool = False, move_todos_to_top: bool = False):
    for item in os.listdir(assets_dir):
        item_path = os.path.join(assets_dir, item)

        if not os.path.isdir(item_path) or not item.startswith("values"):
            continue

        # Skip directory containing base_xml_path
        if isPathInPath(base_xml_path, item_path):
            continue

        strings_xml_path = os.path.join(item_path, "strings.xml")
        if not os.path.isfile(strings_xml_path):
            continue

        if stage_files:
            print(f"Staging {item}")
            os.system(f"git add {strings_xml_path}")

        organiseStringsFile(strings_xml_path, base_xml_path, move_todos_to_top = move_todos_to_top)

def organiseStringsFile(xml_path: str, base_xml_path: str = BASE_STRINGS_XML_PATH, out_path: str | None = None, move_todos_to_top: bool = False):
    print(f"Organising {xml_path.removeprefix(ASSETS_DIR)} based on {base_xml_path.removeprefix(ASSETS_DIR)}")

    xml_data = xmltodict(open(xml_path, "r").read())["resources"]
    out_path = out_path or xml_path

    def getString(key: str) -> str | None:
        for item in xml_data["string"]:
            if item["@name"] == key:
                if not "#text" in item:
                    return None
                return item["#text"].replace("\n", "&#xA;")
        return None

    def getStringArray(key: str) -> list[str] | None:
        for array in xml_data["string-array"]:
            if array["@name"] == key:
                if "item" not in array:
                    return None
                return [item.replace("\n", "&#xA;") for item in array["item"]]
        return None

    file_lines = open(base_xml_path, "r").readlines()
    i = 0

    lines = []
    first_added = False

    def addString(line: str):
        key = line.split("\"")[1]

        original_value = line.split(">")[1].split("<")[0]
        if len(original_value) == 0:
            lines.append(line)
            return

        localised = getString(key)
        new_line = line.replace(original_value, localised or "")
        if localised is None:
            new_line = new_line.replace("\n", f" {TODO_COMMENT}\n")

        lines.append(new_line)

    def addStringArray(array: list[str]):
        key = array[0].split("\"")[1]
        localised = getStringArray(key)

        lines.append(array[0])

        if localised is not None:
            for item in localised:
                lines.append(STRING_ARRAY_ITEM_START + item + STRING_ARRAY_ITEM_END + "\n")
        else:
            lines.append(f"    {TODO_COMMENT}\n")

        lines.append(array[-1])

    while i < len(file_lines):
        line = file_lines[i]
        i += 1

        if line.isspace() and not first_added:
            continue

        if UNTRANSLATABLE_MATCH in line:
            continue

        if line.startswith(STRING_START):
            addString(line)
            first_added = True

        elif line.startswith(STRING_ARRAY_START):
            array_lines = [line]

            line = file_lines[i]
            i += 1

            while line.startswith(STRING_ARRAY_ITEM_START):
                array_lines.append(line)
                line = file_lines[i]
                i += 1

            # Array end
            array_lines.append(line)

            addStringArray(array_lines)
            first_added = True

        else:
            lines.append(line)

    open(out_path, "w").writelines(lines)

    if move_todos_to_top:
        moveTodoLinesToTop(out_path)

def moveTodoLinesToTop(xml_path: str, file_lines: list[str] | None = None):
    lines = []

    top_lines_index = None

    for line in file_lines or open(xml_path, "r").readlines():
        if top_lines_index is None and line.startswith(STRING_START) or line.startswith(STRING_ARRAY_START):
            top_lines_index = len(lines)
            lines.append("\n")

        if TODO_COMMENT in line:
            lines.insert(top_lines_index or len(lines), line)
        else:
            lines.append(line)

    open(xml_path, "w").writelines(lines)

def promptYesNo(message: str) -> bool:
    answer = None
    while answer != "y" and answer != "n":
        answer = input(f"{message} ( y / n ) ").lower()
    return answer == "y"

def main():
    stage = promptYesNo("Stage files before modification?")
    move_todos = promptYesNo("Move TODOs to the top of files?")

    organiseAllStrings(stage_files = stage == "y", move_todos_to_top = move_todos)

if __name__ == "__main__":
    main()
