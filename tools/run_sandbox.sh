#!/usr/bin/env bash
# Packages scala-impl and runs it in a Consulo sandbox (platform and consulo.java from the hub, the version of
# consulo.java matches the downloaded platform build).
# Runs on an isolated X display (Xvfb + openbox), never on the user's display; other sessions use their own displays.
# usage: tools/run_sandbox.sh [display-number]   (default 81)
set -e
REPO=$(cd "$(dirname "$0")/.." && pwd)
DISPLAY_NUM=${1:-81}
MODULE=$REPO/scala/scala-impl
PLATFORM=${PLATFORM_VERSION:-$(curl -s "https://api.consulo.io/repository/info?id=consulo.dist.linux64&platformVersion=SNAPSHOT&version=SNAPSHOT&channel=nightly" | python3 -c 'import json,sys; print(json.load(sys.stdin)["version"])')}

# consulo.java for exactly that platform build (the workspace goal asks the hub for the version of the compile classpath)
DEPS=$MODULE/target/consulo-plugin-dependencies
mkdir -p "$DEPS"
if [ "$(cat "$DEPS/consulo.java.platform" 2>/dev/null)" != "$PLATFORM" ]; then
  rm -rf "$DEPS/consulo.java"
  curl -sL -o "$DEPS/consulo.java.zip" "https://api.consulo.io/repository/download?id=consulo.java&platformVersion=$PLATFORM&version=SNAPSHOT&channel=nightly&platformBuildSelect=true"
  unzip -q -o "$DEPS/consulo.java.zip" -d "$DEPS" && rm "$DEPS/consulo.java.zip"
  echo "$PLATFORM" > "$DEPS/consulo.java.platform"
  echo "$PLATFORM" > "$DEPS/consulo.java.version"
fi

# overlay: consulo.java modules changed locally for the port and not yet published (JvmLanguage in java-language-api)
for module in java.language.api; do
  local_jar=$(ls ~/.m2/repository/consulo/plugin/consulo.java-$module/3-SNAPSHOT/consulo.java-$module-3-SNAPSHOT.jar)
  cp "$local_jar" "$DEPS/consulo.java/lib/consulo.java-$module-3-SNAPSHOT.jar"
done

(cd "$REPO" && mvn -o -q install -pl scala/scala-impl -am -DskipTests)

if ! pgrep -f "Xvfb :$DISPLAY_NUM" >/dev/null; then
  Xvfb ":$DISPLAY_NUM" -screen 0 1600x1000x24 >/dev/null 2>&1 &
  sleep 1
  DISPLAY=":$DISPLAY_NUM" openbox >/dev/null 2>&1 &
fi
cd "$MODULE" && DISPLAY=":$DISPLAY_NUM" mvn consulo:run-desktop-awt-fork
