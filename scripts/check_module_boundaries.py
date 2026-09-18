"""Check StepWise dependency boundaries without Android SDK or third-party packages."""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1] / "android"
ALLOWED = {
    "core": set(),
    "platform-android": {"core"},
    "mode-hardware": {"core", "platform-android"},
    "mode-software": {"core"},
    "software-android": {"core", "mode-software", "platform-android"},
    "app": {"core", "platform-android", "mode-hardware", "mode-software", "software-android"},
}


def check() -> list[str]:
    errors = []
    owners = {}
    sources = []
    for module in ALLOWED:
        for path in (ROOT / module / "src" / "main").rglob("*.kt"):
            source = path.read_text(encoding="utf-8-sig")
            package = re.search(r"^package ([\w.]+)", source, re.MULTILINE)
            if not package:
                continue
            declarations = re.findall(
                r"^(?:(?:data|enum|sealed|fun|value|abstract|open)\s+)*"
                r"(?:class|interface|object)\s+(\w+)", source, re.MULTILINE,
            )
            for name in declarations:
                symbol = f"{package[1]}.{name}"
                if symbol in owners:
                    errors.append(f"Duplicate declaration: {symbol}")
                owners[symbol] = module
            sources.append((module, path, source))

    for module, path, source in sources:
        for imported in re.findall(r"^import ([\w.*]+)", source, re.MULTILINE):
            if module in {"core", "mode-software"} and imported.startswith(("android.", "androidx.")):
                errors.append(f"{path.relative_to(ROOT)}: Android import in JVM module")
            targets = {
                owner for symbol, owner in owners.items()
                if imported == symbol or imported.startswith(symbol + ".")
                or (imported.endswith(".*") and symbol.rsplit(".", 1)[0] == imported[:-2])
            }
            for target in targets:
                if target != module and target not in ALLOWED[module]:
                    errors.append(f"{path.relative_to(ROOT)}: forbidden import {imported} from {target}")

    for module, allowed in ALLOWED.items():
        build = (ROOT / module / "build.gradle.kts").read_text()
        dependencies = set(re.findall(r'project\(":([\w-]+)"\)', build))
        for dependency in dependencies - allowed:
            errors.append(f"{module}: forbidden Gradle dependency on {dependency}")
    return errors


if __name__ == "__main__":
    failures = check()
    if failures:
        print("\n".join(failures), file=sys.stderr)
        sys.exit(1)
    print("Module boundaries OK: " + ", ".join(ALLOWED))
