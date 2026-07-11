#!/usr/bin/env python3
"""
Generate and insert a Java serialVersionUID using the JDK `serialver` tool.

Workflow:
1. Read a Java source file.
2. Temporarily remove any existing serialVersionUID declaration and its @Serial annotation.
3. Compile the project so the target class is built without an explicit serialVersionUID.
4. Run `serialver` against the compiled class.
5. Restore the original source.
6. Insert or replace:
       import java.io.Serial;

       @Serial
       private static final long serialVersionUID = ...L;

Default assumptions:
- Maven project
- Compiled classes are written to target/classes

Implementation notes:
- This script intentionally uses dependency-free, heuristic text editing.
- Import and field insertion assume conventional Java source formatting.
- Existing `import java.io.Serial;` declarations are left untouched.
- The generated field is formatted with one blank line after the type's
  opening brace and one blank line before the next field or method.

Future direction:
- Convert this standalone script into a uv-managed Python project suitable
  for packaging and publishing to PyPI.
- Replace heuristic Java source editing with an AST-aware implementation.
- Evaluate JavaParser for import management, member insertion, nested types,
  comments, annotations, records, enums, and modern Java syntax.
- Keep compilation and `serialver` orchestration separate from source editing
  so the parser implementation can be replaced without rewriting the build
  integration.

Examples:
    python generate_serial_uid.py src/main/java/org/acme/Foo.java

    python generate_serial_uid.py \
        src/main/java/org/acme/Foo.java \
        --compile-command "./mvnw -q -DskipTests compile"

    python generate_serial_uid.py \
        src/main/java/org/acme/Foo.java \
        --classes-dir build/classes/java/main \
        --compile-command "./gradlew classes"
"""

from __future__ import annotations

import argparse
import os
import re
import shlex
import subprocess
import sys
import tempfile
from pathlib import Path


SERIAL_IMPORT = "import java.io.Serial;"

# Matches an optional @Serial annotation immediately preceding serialVersionUID.
SERIAL_FIELD_RE = re.compile(
    r"""
    (?P<indent>^[ \t]*)
    (?:@Serial[ \t]*\r?\n[ \t]*)?
    (?:
        private|protected|public
    )?
    [ \t]*
    static[ \t]+final[ \t]+long[ \t]+serialVersionUID
    [ \t]*=[ \t]*
    [+-]?\d+[lL]
    [ \t]*;
    [ \t]*(?:\r?\n)?
    """,
    re.MULTILINE | re.VERBOSE,
)

PACKAGE_RE = re.compile(
    r"^[ \t]*package[ \t]+(?P<package>[A-Za-z_]\w*(?:\.[A-Za-z_]\w*)*)[ \t]*;",
    re.MULTILINE,
)

PUBLIC_TYPE_RE = re.compile(
    r"""
    ^[ \t]*
    public
    (?:[ \t]+(?:abstract|final|sealed|non-sealed|strictfp))*
    [ \t]+
    (?:class|record|enum)
    [ \t]+
    (?P<name>[A-Za-z_]\w*)
    """,
    re.MULTILINE | re.VERBOSE,
)

TYPE_RE = re.compile(
    r"""
    ^[ \t]*
    (?:(?:public|protected|private|abstract|final|sealed|non-sealed|strictfp|static)[ \t]+)*
    (?:class|record|enum)
    [ \t]+
    (?P<name>[A-Za-z_]\w*)
    """,
    re.MULTILINE | re.VERBOSE,
)

IMPORT_RE = re.compile(
    r"^[ \t]*import[ \t]+(?:static[ \t]+)?[\w.*]+[ \t]*;[ \t]*(?:\r?\n)?",
    re.MULTILINE,
)


class SerialUidError(RuntimeError):
    """Raised when serialVersionUID generation cannot be completed."""


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Generate a Java serialVersionUID with serialver and update the source."
    )
    parser.add_argument(
        "source",
        type=Path,
        help="Java source file to update.",
    )
    parser.add_argument(
        "--project-dir",
        type=Path,
        default=Path.cwd(),
        help="Project directory where the compile command runs. Default: current directory.",
    )
    parser.add_argument(
        "--classes-dir",
        type=Path,
        default=Path("target/classes"),
        help="Compiled classes directory, relative to project-dir unless absolute.",
    )
    parser.add_argument(
        "--compile-command",
        default="mvn -q -DskipTests compile",
        help='Command used to compile the project. Default: "mvn -q -DskipTests compile".',
    )
    parser.add_argument(
        "--serialver",
        default="serialver",
        help='serialver executable. Default: "serialver".',
    )
    parser.add_argument(
        "--class-name",
        help="Fully qualified class name. Normally inferred from package and source filename.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print the updated source without writing it.",
    )
    return parser.parse_args()


def detect_newline(text: str) -> str:
    return "\r\n" if "\r\n" in text else "\n"


def infer_class_name(source_path: Path, source: str) -> str:
    package_match = PACKAGE_RE.search(source)
    package_name = package_match.group("package") if package_match else ""

    type_match = PUBLIC_TYPE_RE.search(source) or TYPE_RE.search(source)
    type_name = type_match.group("name") if type_match else source_path.stem

    return f"{package_name}.{type_name}" if package_name else type_name


def remove_serial_field(source: str) -> tuple[str, bool]:
    updated, count = SERIAL_FIELD_RE.subn("", source, count=1)
    return updated, count > 0


def remove_unused_serial_import(source: str) -> str:
    """
    Remove java.io.Serial only from the temporary compilation source when no
    @Serial annotation remains. This avoids an unused import concern in stricter builds.
    """
    if "@Serial" in source:
        return source

    pattern = re.compile(
        r"^[ \t]*import[ \t]+java\.io\.Serial[ \t]*;[ \t]*(?:\r?\n)?",
        re.MULTILINE,
    )
    return pattern.sub("", source, count=1)


def run_command(command: str, cwd: Path) -> None:
    args = shlex.split(command)
    if not args:
        raise SerialUidError("Compile command is empty.")

    result = subprocess.run(args, cwd=cwd)
    if result.returncode != 0:
        raise SerialUidError(
            f"Compile command failed with exit code {result.returncode}: {command}"
        )


def detect_maven_command(project_dir: Path) -> list[str]:
    wrapper = project_dir / ("mvnw.cmd" if os.name == "nt" else "mvnw")
    if wrapper.is_file():
        return [str(wrapper)]

    return ["mvn"]


def build_maven_classpath(project_dir: Path, classes_dir: Path) -> str:
    """
    Build the classpath needed by serialver.

    serialver loads the target class reflectively, so referenced dependency
    types must also be available.
    """
    with tempfile.NamedTemporaryFile(
        prefix="serialver-classpath-",
        suffix=".txt",
        delete=False,
    ) as temp:
        output_file = Path(temp.name)

    try:
        command = [
            *detect_maven_command(project_dir),
            "-q",
            "-DincludeScope=compile",
            f"-Dmdep.outputFile={output_file}",
            "dependency:build-classpath",
        ]

        result = subprocess.run(
            command,
            cwd=project_dir,
            capture_output=True,
            text=True,
        )
        if result.returncode != 0:
            details = (result.stderr or result.stdout).strip()
            raise SerialUidError(
                "Could not build the Maven compile classpath"
                + (f": {details}" if details else "")
            )

        dependency_classpath = output_file.read_text(encoding="utf-8").strip()
    finally:
        output_file.unlink(missing_ok=True)

    entries = [str(classes_dir)]
    if dependency_classpath:
        entries.append(dependency_classpath)

    return os.pathsep.join(entries)


def run_serialver(
    executable: str,
    classpath: str,
    class_name: str,
    cwd: Path,
) -> str:
    command = [
        executable,
        "-classpath",
        classpath,
        class_name,
    ]

    result = subprocess.run(
        command,
        cwd=cwd,
        capture_output=True,
        text=True,
    )

    if result.returncode != 0:
        details = (result.stderr or result.stdout).strip()
        raise SerialUidError(
            f"serialver failed for {class_name}: {details or 'unknown error'}"
        )

    output = result.stdout.strip()
    match = re.search(
        r"serialVersionUID\s*=\s*(?P<uid>[+-]?\d+[lL])\s*;",
        output,
    )
    if not match:
        raise SerialUidError(f"Could not parse serialver output: {output}")

    return match.group("uid").upper().replace("LL", "L")


def ensure_serial_import(source: str, newline: str) -> str:
    """
    Add java.io.Serial without disturbing existing imports.

    Placement rules:
    - If java.io.Serial is already imported, leave imports unchanged.
    - Prefer the existing java.* import group.
    - Insert lexicographically within that group.
    - If no java.* group exists, create one before the first import group.
    """
    if re.search(
        r"^[ \t]*import[ \t]+java\.io\.Serial[ \t]*;",
        source,
        re.MULTILINE,
    ):
        return source

    import_line_re = re.compile(
        r"^(?P<indent>[ \t]*)import[ \t]+(?P<static>static[ \t]+)?"
        r"(?P<name>[\w.*]+)[ \t]*;[ \t]*(?P<newline>\r?\n|$)",
        re.MULTILINE,
    )
    imports = list(import_line_re.finditer(source))

    if not imports:
        package_match = PACKAGE_RE.search(source)
        if package_match:
            point = package_match.end()
            return (
                source[:point]
                + newline
                + newline
                + SERIAL_IMPORT
                + newline
                + source[point:].lstrip("\r\n")
            )

        return SERIAL_IMPORT + newline + newline + source

    java_imports = [
        match
        for match in imports
        if match.group("static") is None
        and match.group("name").startswith("java.")
    ]

    if java_imports:
        serial_name = "java.io.Serial"

        for match in java_imports:
            if serial_name < match.group("name"):
                point = match.start()
                return source[:point] + SERIAL_IMPORT + newline + source[point:]

        point = java_imports[-1].end()
        return source[:point] + SERIAL_IMPORT + newline + source[point:]

    first_import = imports[0]
    point = first_import.start()
    return (
        source[:point]
        + SERIAL_IMPORT
        + newline
        + newline
        + source[point:]
    )


def find_type_body_start(source: str) -> int:
    """
    Find the opening brace of the first top-level class/record/enum declaration.
    This is intentionally lightweight and works for normal Java declarations.
    """
    type_match = PUBLIC_TYPE_RE.search(source) or TYPE_RE.search(source)
    if not type_match:
        raise SerialUidError("Could not find a class, record, or enum declaration.")

    brace = source.find("{", type_match.end())
    if brace < 0:
        raise SerialUidError("Could not find the opening brace of the Java type.")

    return brace


def infer_member_indent(source: str, body_start: int) -> str:
    remainder = source[body_start + 1 :]
    member_match = re.search(r"\r?\n(?P<indent>[ \t]+)\S", remainder)
    return member_match.group("indent") if member_match else "    "


def insert_serial_field(source: str, uid: str, newline: str) -> str:
    body_start = find_type_body_start(source)
    indent = infer_member_indent(source, body_start)

    # Consume whitespace immediately inside the type body so insertion always
    # produces exactly one blank line after "{" and one blank line before the
    # first existing member.
    content_start = body_start + 1
    remainder = source[content_start:]
    leading_blank_lines = re.match(r"(?:[ \t]*\r?\n)*", remainder)
    first_member_start = (
        content_start + leading_blank_lines.end()
        if leading_blank_lines
        else content_start
    )

    declaration = (
        newline
        + newline
        + indent
        + "@Serial"
        + newline
        + indent
        + f"private static final long serialVersionUID = {uid};"
        + newline
        + newline
    )

    return (
        source[:content_start]
        + declaration
        + source[first_member_start:]
    )


def replace_or_insert_serial_field(source: str, uid: str, newline: str) -> str:
    replacement = (
        r"\g<indent>@Serial"
        + newline
        + r"\g<indent>private static final long serialVersionUID = "
        + uid
        + ";"
        + newline
    )

    updated, count = SERIAL_FIELD_RE.subn(replacement, source, count=1)
    if count:
        return updated

    return insert_serial_field(source, uid, newline)


def main() -> int:
    args = parse_args()

    project_dir = args.project_dir.expanduser().resolve()
    source_path = args.source.expanduser()
    if not source_path.is_absolute():
        source_path = (Path.cwd() / source_path).resolve()

    classes_dir = args.classes_dir.expanduser()
    if not classes_dir.is_absolute():
        classes_dir = (project_dir / classes_dir).resolve()

    if not source_path.is_file():
        print(f"error: source file not found: {source_path}", file=sys.stderr)
        return 2

    original_source = source_path.read_text(encoding="utf-8")
    newline = detect_newline(original_source)
    class_name = args.class_name or infer_class_name(source_path, original_source)

    temporary_source, had_existing_uid = remove_serial_field(original_source)
    temporary_source = remove_unused_serial_import(temporary_source)

    try:
        source_path.write_text(temporary_source, encoding="utf-8")

        print(f"Compiling {class_name} without an explicit serialVersionUID...")
        run_command(args.compile_command, project_dir)

        classpath = build_maven_classpath(project_dir, classes_dir)
        print("Running serialver with the Maven compile classpath...")
        uid = run_serialver(
            executable=args.serialver,
            classpath=classpath,
            class_name=class_name,
            cwd=project_dir,
        )
    except (OSError, SerialUidError) as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1
    finally:
        # Always restore the user's original source before applying the final patch.
        source_path.write_text(original_source, encoding="utf-8")

    updated_source = ensure_serial_import(original_source, newline)
    updated_source = replace_or_insert_serial_field(updated_source, uid, newline)

    action = "Replaced" if had_existing_uid else "Added"
    print(f"{action} serialVersionUID for {class_name}: {uid}")

    if args.dry_run:
        print(updated_source, end="")
    else:
        source_path.write_text(updated_source, encoding="utf-8")
        print(f"Updated {source_path}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())