#!/usr/bin/env bash
# Compiles a module with scalac directly (all errors, no zinc limit of 100 problems).
# usage: tools/scalac_check.sh <module-dir> [out-log]
set -e
MODULE=$1
LOG=${2:-/tmp/scalac-check.log}
REPO=$(cd "$(dirname "$0")/.." && pwd)
CACHE=$REPO/tools/.cache
mkdir -p "$CACHE"
CP_FILE=$CACHE/$(echo "$MODULE" | tr '/' '_').classpath
if [ ! -f "$CP_FILE" ] || [ "$REPO/$MODULE/pom.xml" -nt "$CP_FILE" ]; then
  (cd "$REPO" && mvn -q dependency:build-classpath -pl "$MODULE" -Dmdep.outputFile="$CP_FILE" -Dmdep.includeScope=compile >/dev/null)
fi
SCALA_CP=$(ls ~/.m2/repository/org/scala-lang/scala3-compiler_3/3.9.0/*.jar ~/.m2/repository/org/scala-lang/scala3-interfaces/3.9.0/*.jar ~/.m2/repository/org/scala-lang/tasty-core_3/3.9.0/tasty-core_3-3.9.0.jar ~/.m2/repository/org/scala-lang/scala-library/3.9.0/scala-library-3.9.0.jar ~/.m2/repository/org/scala-lang/modules/scala-asm/*/scala-asm-*.jar ~/.m2/repository/org/scala-sbt/compiler-interface/1.12.0/*.jar ~/.m2/repository/org/scala-sbt/util-interface/*/*.jar 2>/dev/null | grep -v sources | tr '\n' ':')
OUT=$(mktemp -d)
# upstream compiles only scala-impl with -no-indent (brace syntax); other modules use indentation syntax
INDENT_OPT=""
[ "$MODULE" = "scala/scala-impl" ] && INDENT_OPT="-no-indent"
find "$REPO/$MODULE/src/main/scala" \( -name '*.scala' -o -name '*.java' \) > "$OUT/sources.txt"
java -Xss4m -Xmx5g -cp "$SCALA_CP" dotty.tools.dotc.Main -d "$OUT" -classpath "$(cat "$CP_FILE")" \
  $INDENT_OPT -color:never -nowarn @"$OUT/sources.txt" > "$LOG" 2>&1 || true
rm -rf "$OUT"
grep -cE '^-- .*Error' "$LOG" || true
