#!/usr/bin/env python3
import keyword
import plistlib
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

SPECIFIER = re.compile(r"%(?:(\d+)\$)?([sd])")
INFO_PLIST = {
    "ios_camera_usage": "NSCameraUsageDescription",
    "ios_microphone_usage": "NSMicrophoneUsageDescription",
    "ios_speech_recognition_usage": "NSSpeechRecognitionUsageDescription",
}
SWIFT_KEYWORDS = {"continue", "default", "delete", "done", "repeat", "return", "switch", "case", "class", "struct"}


def unescape(text):
    out, i = [], 0
    while i < len(text):
        c = text[i]
        if c == "\\" and i + 1 < len(text):
            nxt = text[i + 1]
            out.append({"n": "\n", "t": "\t"}.get(nxt, nxt))
            i += 2
            continue
        out.append(c)
        i += 1
    return "".join(out)


def to_ios_format(text):
    return SPECIFIER.sub(lambda m: "%" + (m.group(1) + "$" if m.group(1) else "") + ("@" if m.group(2) == "s" else "ld"), text)


def arguments(text):
    found = {}
    for index, match in enumerate(SPECIFIER.finditer(text), start=1):
        position = int(match.group(1)) if match.group(1) else index
        found[position] = "String" if match.group(2) == "s" else "Int"
    return [found[p] for p in sorted(found)]


def swift_name(key):
    head, *rest = key.split("_")
    name = head + "".join(part[:1].upper() + part[1:] for part in rest)
    return f"`{name}`" if name in SWIFT_KEYWORDS or keyword.iskeyword(name) else name


def quoted(text):
    return '"' + text.replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n").replace("\t", "\\t") + '"'


def read(strings_xml):
    root = ET.parse(strings_xml).getroot()
    strings, plurals = {}, {}
    for element in root:
        name = element.get("name")
        if element.tag == "string":
            strings[name] = unescape("".join(element.itertext()))
        elif element.tag == "plurals":
            plurals[name] = {item.get("quantity"): unescape("".join(item.itertext())) for item in element}
    return strings, plurals


def accessor(key, sample, counted=False):
    params = arguments(sample) or (["Int"] if counted else [])
    name = swift_name(key)
    if not params:
        return f"    static var {name}: String {{ tr({quoted(key)}) }}"
    signature = ", ".join(f"_ p{i}: {kind}" for i, kind in enumerate(params, start=1))
    values = ", ".join(f"p{i}" for i in range(1, len(params) + 1))
    return f"    static func {name}({signature}) -> String {{ tr({quoted(key)}, {values}) }}"


def write_swift(strings, plurals, out):
    lines = ["// Generated from presentation/src/main/res/values/strings.xml. Do not edit, do not commit.", "import Foundation", "", "enum Strings {"]
    for key, value in strings.items():
        if key not in INFO_PLIST:
            lines.append(accessor(key, value))
    for key, forms in plurals.items():
        lines.append(accessor(key, forms.get("other") or next(iter(forms.values())), counted=True))
    lines += [
        "}",
        "",
        "private func tr(_ key: String, _ args: CVarArg...) -> String {",
        "    let format = Bundle.main.localizedString(forKey: key, value: nil, table: nil)",
        "    return args.isEmpty ? format : String(format: format, locale: Locale.current, arguments: args)",
        "}",
        "",
    ]
    content = "\n".join(lines)
    target = Path(out)
    if not target.exists() or target.read_text() != content:
        target.write_text(content)


def write_bundle(strings, plurals, lproj):
    folder = Path(lproj)
    folder.mkdir(parents=True, exist_ok=True)
    body = "".join(f"{quoted(key)} = {quoted(to_ios_format(value))};\n" for key, value in strings.items())
    (folder / "Localizable.strings").write_text(body, encoding="utf-8")
    info = "".join(f"{quoted(INFO_PLIST[key])} = {quoted(value)};\n" for key, value in strings.items() if key in INFO_PLIST)
    (folder / "InfoPlist.strings").write_text(info, encoding="utf-8")
    table = {
        key: {
            "NSStringLocalizedFormatKey": "%#@count@",
            "count": {
                "NSStringFormatSpecTypeKey": "NSStringPluralRuleType",
                "NSStringFormatValueTypeKey": "ld",
                **{quantity: to_ios_format(text) for quantity, text in forms.items()},
            },
        }
        for key, forms in plurals.items()
    }
    with open(folder / "Localizable.stringsdict", "wb") as handle:
        plistlib.dump(table, handle)


def main():
    strings_xml, swift_out = sys.argv[1], sys.argv[2]
    strings, plurals = read(strings_xml)
    write_swift(strings, plurals, swift_out)
    if len(sys.argv) > 3:
        write_bundle(strings, plurals, sys.argv[3])


if __name__ == "__main__":
    main()
