#!/usr/bin/env python3
"""List upstream changes that CloudStream Skia has to port by hand.

Files in app/src/skia/res replace upstream resources, so upstream edits to those
resources do not reach the app on their own. Given the upstream commit the fork was
last synced with (OLD) and the new upstream commit (NEW), this prints:

  * overridden resource files (layouts, drawables, fonts, ...) that upstream changed;
  * overridden values (colors, dimens, styles, strings) whose upstream definition changed;
  * upstream files carrying a "Skia hook" that upstream changed (merge conflict risk).

Usage:
    python scripts/skia/drift-check.py OLD NEW [--markdown FILE] [--fail-on-drift]
"""

import argparse
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SKIA_RES = Path("app/src/skia/res")
MAIN_RES = Path("app/src/main/res")
VALUE_TAGS = {"color", "dimen", "style", "string", "integer", "bool", "array", "string-array", "plurals"}


def git(*args: str, check: bool = True) -> str:
    result = subprocess.run(
        ["git", *args], cwd=ROOT, capture_output=True, text=True, encoding="utf-8"
    )
    if check and result.returncode != 0:
        sys.exit(f"git {' '.join(args)} failed:\n{result.stderr}")
    return result.stdout


def changed_paths(old: str, new: str) -> set[str]:
    return set(git("diff", "--name-only", old, new).split())


def file_overrides() -> list[Path]:
    out = []
    for path in sorted((ROOT / SKIA_RES).rglob("*")):
        if path.is_file() and not path.parent.name.startswith("values"):
            out.append(path.relative_to(ROOT / SKIA_RES))
    return out


def value_overrides() -> dict[tuple[str, str], Path]:
    """(tag, name) -> skia values file, for every value resource the fork defines."""
    out = {}
    for path in sorted((ROOT / SKIA_RES).glob("values*/*.xml")):
        for element in ET.parse(path).getroot():
            name = element.get("name")
            if element.tag in VALUE_TAGS and name:
                out[(element.tag, name)] = path.relative_to(ROOT)
    return out


def upstream_values(ref: str, values_dir: str) -> dict[tuple[str, str], str]:
    """(tag, name) -> canonical XML of every value resource in app/src/main/res/<values_dir>."""
    out = {}
    listing = git("ls-tree", "--name-only", f"{ref}:{(MAIN_RES / values_dir).as_posix()}", check=False)
    for file_name in listing.split():
        if not file_name.endswith(".xml"):
            continue
        text = git("show", f"{ref}:{(MAIN_RES / values_dir / file_name).as_posix()}")
        try:
            root = ET.fromstring(text)
        except ET.ParseError:
            continue
        for element in root:
            name = element.get("name")
            if element.tag in VALUE_TAGS and name:
                out[(element.tag, name)] = ET.tostring(element, encoding="unicode").strip()
    return out


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("old", help="upstream commit the fork was synced with")
    parser.add_argument("new", help="new upstream commit")
    parser.add_argument("--markdown", help="also write the report to this file")
    parser.add_argument("--fail-on-drift", action="store_true", help="exit with 2 when something needs porting")
    args = parser.parse_args()

    changed = changed_paths(args.old, args.new)
    lines: list[str] = []

    # Resource files replaced as a whole.
    for rel in file_overrides():
        upstream = (MAIN_RES / rel).as_posix()
        if upstream in changed:
            stat = git("diff", "--stat", args.old, args.new, "--", upstream).strip().splitlines()
            summary = stat[-1].strip() if stat else "changed"
            lines.append(f"- `{upstream}` ({summary}); port into `{(SKIA_RES / rel).as_posix()}`")

    # Value resources overridden by name.
    overrides = value_overrides()
    values_dirs = sorted({p.parent.name for p in overrides.values()})
    for values_dir in values_dirs:
        if not any(c.startswith((MAIN_RES / values_dir).as_posix() + "/") for c in changed):
            continue
        before = upstream_values(args.old, values_dir)
        after = upstream_values(args.new, values_dir)
        for (tag, name), skia_file in overrides.items():
            if skia_file.parent.name != values_dir:
                continue
            if before.get((tag, name)) != after.get((tag, name)):
                state = "removed" if (tag, name) not in after else "added" if (tag, name) not in before else "changed"
                lines.append(f"- `<{tag} name=\"{name}\">` {state} upstream ({values_dir}); check `{skia_file.as_posix()}`")

    drift = bool(lines)

    # Upstream files with hooks: informational, the merge itself shows conflicts.
    hooked = sorted({line.split(":", 1)[0] for line in git("grep", "-l", "Skia hook", check=False).splitlines()})
    hook_lines = [f"- `{path}`" for path in hooked if path in changed]

    report = [f"Skia drift check: `{args.old[:10]}..{args.new[:10]}`", ""]
    if drift:
        report += ["Upstream changed resources that the fork overrides:", "", *lines, ""]
    else:
        report += ["No overridden resource changed upstream.", ""]
    if hook_lines:
        report += ["Upstream also changed files that carry a Skia hook (check the hook survived):", "", *hook_lines, ""]

    text = "\n".join(report)
    print(text)
    if args.markdown:
        Path(args.markdown).write_text(text + "\n", encoding="utf-8")
    return 2 if drift and args.fail_on_drift else 0


if __name__ == "__main__":
    sys.exit(main())
