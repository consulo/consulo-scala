#!/usr/bin/env python3
"""Removes stale generated component bindings left by incremental Maven builds.

When a registered (annotated) class is deleted or renamed, an incremental build keeps its generated `<Class>_Binding`
source in target/generated-sources (Java APT: annotations/, Scala: consulo-binding/), the compiled binding classes in
target/classes and the binding entry in target/classes/META-INF/services/consulo.component.bind.* - the plugin then logs
"Failed to build ..._Binding" at startup. A binding is stale when no source file of the module declares the bound class
(class/trait/object/interface/enum/record with that name in the binding's package; the declaring file may have any name).
The bound class may itself be generated from a source class (`<Class>_SearchableTexts` of a configurable) - then it is
stale together with its origin, and the generated class is removed too.

usage: tools/clean_stale_bindings.py [-n|--dry-run] [module dir ...]   (default: every module of the repository)
"""
import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# (generated sources dirs, classes dir, source dirs)
SCOPES = [
    (["generated-sources/annotations", "generated-sources/consulo-binding"], "classes", ["src/main/java", "src/main/scala"]),
    (["generated-test-sources/test-annotations", "generated-test-sources/consulo-binding"], "test-classes", ["src/test/java", "src/test/scala"]),
]

SERVICES_PREFIX = "consulo.component.bind."
BINDING_SUFFIX = "_Binding"

PACKAGE_RE = re.compile(r"^\s*package\s+(?!object\b)([\w.]+)\s*;?\s*(\{)?\s*$")
DECL_RE = re.compile(
    r"^[^\S\n]*(?:(?:@[\w.]+(?:\([^)\n]*\))?|public|protected|private(?:\[\w+\])?|abstract|final|sealed|static|case|implicit|"
    r"open|transparent|inline|strictfp|non-sealed)[^\S\n]+)*(?:class|trait|object|interface|enum|record|@interface)[^\S\n]+([\w$]+)",
    re.MULTILINE,
)
COMMENT_RE = re.compile(r"/\*.*?\*/|//[^\n]*", re.DOTALL)


def file_package(text):
    """Package of a source file; chained Scala package clauses (`package a` / `package b`) are joined."""
    parts = []
    for line in text.splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("//") or stripped.startswith("/*") or stripped.startswith("*"):
            continue
        m = PACKAGE_RE.match(line)
        if not m:
            break
        parts.append(m.group(1))
    return ".".join(parts)


def declared_classes(source_dirs):
    """Set of (package, simple name) declared anywhere in the module sources (nested declarations included - keeping a
    binding is always safe, removing one is not)."""
    declared = set()
    for source_dir in source_dirs:
        for root, _, files in os.walk(source_dir):
            for name in files:
                if not name.endswith((".java", ".scala")):
                    continue
                with open(os.path.join(root, name), encoding="utf-8", errors="replace") as f:
                    text = f.read()
                package = file_package(text)
                for m in DECL_RE.finditer(COMMENT_RE.sub("", text)):
                    declared.add((package, m.group(1)))
    return declared


def bound_class(binding_fqn):
    package, _, simple = binding_fqn.rpartition(".")
    if not simple.endswith(BINDING_SUFFIX) or simple == BINDING_SUFFIX:
        return None
    return package, simple[: -len(BINDING_SUFFIX)]


def is_declared(package, simple, declared):
    if (package, simple) in declared:
        return True
    # a nested class (Outer$Inner) is declared as Inner
    return "$" in simple and (package, simple.rsplit("$", 1)[1]) in declared


def generated_origin(package, simple, generated):
    """Origin class of a generated (non-binding) class: `Class_SearchableTexts` -> `Class`."""
    if (package, simple) not in generated or "_" not in simple:
        return None
    return package, simple.rsplit("_", 1)[0]


def is_stale(binding_fqn, declared, generated):
    bound = bound_class(binding_fqn)
    if bound is None:
        return False
    package, simple = bound
    if is_declared(package, simple, declared):
        return False
    origin = generated_origin(package, simple, generated)
    if origin is not None:
        return not is_declared(*origin, declared)
    return True


def generated_sources(generated_dirs):
    """(bindings, other generated classes): fqn -> generated source paths"""
    bindings, others = {}, {}
    for generated_dir in generated_dirs:
        for root, _, files in os.walk(generated_dir):
            for name in files:
                stem, ext = os.path.splitext(name)
                if ext not in (".java", ".scala"):
                    continue
                package = os.path.relpath(root, generated_dir).replace(os.sep, ".")
                fqn = stem if package == "." else package + "." + stem
                target = bindings if stem.endswith(BINDING_SUFFIX) else others
                target.setdefault(fqn, []).append(os.path.join(root, name))
    return bindings, others


def compiled_binding_files(classes_dir, binding_fqn):
    package, _, simple = binding_fqn.rpartition(".")
    directory = os.path.join(classes_dir, *package.split(".")) if package else classes_dir
    if not os.path.isdir(directory):
        return []
    return [
        os.path.join(directory, name)
        for name in os.listdir(directory)
        if name in (simple + ".class", simple + ".tasty") or name.startswith(simple + "$")
    ]


def clean_scope(module_dir, generated_dirs, classes_dir, source_dirs, dry_run):
    generated_dirs = [d for d in (os.path.join(module_dir, "target", g) for g in generated_dirs) if os.path.isdir(d)]
    classes_dir = os.path.join(module_dir, "target", classes_dir)
    source_dirs = [d for d in (os.path.join(module_dir, s) for s in source_dirs) if os.path.isdir(d)]
    services_dir = os.path.join(classes_dir, "META-INF", "services")
    service_files = (
        [os.path.join(services_dir, f) for f in os.listdir(services_dir) if f.startswith(SERVICES_PREFIX)]
        if os.path.isdir(services_dir)
        else []
    )
    if not source_dirs or (not generated_dirs and not service_files):
        return 0

    declared = declared_classes(source_dirs)
    bindings, others = generated_sources(generated_dirs)
    generated = {tuple(fqn.rpartition(".")[::2]) for fqn in others}

    service_entries = {}
    for service_file in service_files:
        with open(service_file, encoding="utf-8") as f:
            service_entries[service_file] = [line.rstrip("\n") for line in f]

    candidates = set(bindings)
    for lines in service_entries.values():
        candidates.update(line.strip() for line in lines if line.strip() and not line.strip().startswith("#"))

    stale = sorted(fqn for fqn in candidates if is_stale(fqn, declared, generated))
    if not stale:
        return 0

    stale_set = set(stale)
    removed = 0
    for fqn in stale:
        paths = bindings.get(fqn, []) + compiled_binding_files(classes_dir, fqn)
        package, simple = bound_class(fqn)
        bound_fqn = package + "." + simple if package else simple
        if bound_fqn in others:
            paths += others[bound_fqn] + compiled_binding_files(classes_dir, bound_fqn)
        print(f"{os.path.relpath(module_dir, REPO)}: stale binding {fqn}")
        for path in paths:
            print(f"  remove {os.path.relpath(path, REPO)}")
            if not dry_run:
                os.remove(path)
        removed += 1

    for service_file, lines in service_entries.items():
        kept = [line for line in lines if line.strip() not in stale_set]
        if len(kept) != len(lines):
            print(f"  {os.path.relpath(service_file, REPO)}: drop {len(lines) - len(kept)} entr{'y' if len(lines) - len(kept) == 1 else 'ies'}")
            if not dry_run:
                if any(line.strip() and not line.strip().startswith("#") for line in kept):
                    with open(service_file, "w", encoding="utf-8") as f:
                        f.write("".join(line + "\n" for line in kept))
                else:
                    os.remove(service_file)
    return removed


def modules(roots):
    for root in roots:
        for directory, dirs, files in os.walk(root):
            dirs[:] = [d for d in dirs if d not in ("target", "src", "sandbox", ".git", "node_modules") and not d.startswith(".")]
            if "pom.xml" in files and os.path.isdir(os.path.join(directory, "target")):
                yield directory


def main(argv):
    dry_run = any(a in ("-n", "--dry-run") for a in argv)
    roots = [os.path.abspath(a) for a in argv if not a.startswith("-")] or [REPO]
    total = 0
    for module_dir in modules(roots):
        for generated_dirs, classes_dir, source_dirs in SCOPES:
            total += clean_scope(module_dir, generated_dirs, classes_dir, source_dirs, dry_run)
    if total:
        print(f"{'would remove' if dry_run else 'removed'} {total} stale binding(s)")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
