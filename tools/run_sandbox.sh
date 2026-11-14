#!/usr/bin/env bash
# Packages the plugin and runs it in a Consulo sandbox (platform and consulo.java from the hub, the version of
# consulo.java matches the downloaded platform build).
# Runs on an isolated X display (Xvfb + openbox), never on the user's display; other sessions use their own displays.
# usage: tools/run_sandbox.sh [display-number]   (default 81)
set -e
REPO=$(cd "$(dirname "$0")/.." && pwd)
DISPLAY_NUM=${1:-81}
MODULE=$REPO/plugin
PLATFORM=${PLATFORM_VERSION:-$(curl -s "https://api.consulo.io/repository/info?id=consulo.dist.linux64&platformVersion=SNAPSHOT&version=SNAPSHOT&channel=nightly" | python3 -c 'import json,sys; print(json.load(sys.stdin)["version"])')}

# required plugins (and their required dependencies) for exactly that platform build
DEPS=$MODULE/target/consulo-plugin-dependencies
mkdir -p "$DEPS"
fetch_plugin() {
  local id=$1
  if [ "$(cat "$DEPS/$id.platform" 2>/dev/null)" != "$PLATFORM" ]; then
    rm -rf "${DEPS:?}/$id"
    curl -sfL -o "$DEPS/$id.zip" "https://api.consulo.io/repository/download?id=$id&platformVersion=$PLATFORM&version=SNAPSHOT&channel=nightly&platformBuildSelect=true" || { echo "no build of $id for $PLATFORM"; exit 1; }
    unzip -q -o "$DEPS/$id.zip" -d "$DEPS" && rm "$DEPS/$id.zip"
    echo "$PLATFORM" > "$DEPS/$id.platform"
    echo "$PLATFORM" > "$DEPS/$id.version"
  fi
  local dep
  for dep in $(for jar in "$DEPS/$id"/lib/*.jar; do unzip -p "$jar" META-INF/plugin.xml 2>/dev/null; done | grep -v 'optional="true"' | sed -nE 's#.*<depends>([^<]+)</depends>.*#\1#p' | sort -u); do
    if [ ! -d "$DEPS/$dep" ] || [ "$(cat "$DEPS/$dep.platform" 2>/dev/null)" != "$PLATFORM" ]; then
      fetch_plugin "$dep"
    fi
  done
}
fetch_plugin consulo.java
fetch_plugin org.jetbrains.fernflower
fetch_plugin com.intellij.regexp

# overlay: consulo.java modules changed locally for the port and not yet published (JvmLanguage in java-language-api)
for module in java.language.api java.compiler.impl; do
  local_jar=$(ls ~/.m2/repository/consulo/plugin/consulo.java-$module/3-SNAPSHOT/consulo.java-$module-3-SNAPSHOT.jar)
  cp "$local_jar" "$DEPS/consulo.java/lib/consulo.java-$module-3-SNAPSHOT.jar"
  python3 "$REPO/tools/reindex_plugin_jar.py" "$DEPS/consulo.java" "consulo.java-$module-3-SNAPSHOT.jar"
done

(cd "$REPO" && mvn -q install -pl plugin -am -DskipTests -Dmaven.build.cache.enabled=false)

if ! pgrep -f "Xvfb :$DISPLAY_NUM" >/dev/null; then
  Xvfb ":$DISPLAY_NUM" -screen 0 1600x1000x24 >/dev/null 2>&1 &
  sleep 1
  DISPLAY=":$DISPLAY_NUM" openbox >/dev/null 2>&1 &
fi
cd "$MODULE" && DISPLAY=":$DISPLAY_NUM" mvn consulo:run-desktop-awt-fork
