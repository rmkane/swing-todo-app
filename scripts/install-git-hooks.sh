#!/usr/bin/env bash
# Install project git hooks into .git/hooks (symlinks so updates apply automatically).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/scripts/git-hooks"
DEST="$ROOT/.git/hooks"

if [[ ! -d "$ROOT/.git" ]]; then
	echo "install-git-hooks: not a git repository ($ROOT)" >&2
	exit 1
fi

if [[ ! -d "$SRC" ]]; then
	echo "install-git-hooks: missing $SRC" >&2
	exit 1
fi

mkdir -p "$DEST"

for hook in "$SRC"/*; do
	[[ -f "$hook" ]] || continue
	name="$(basename "$hook")"
	ln -sf "../../scripts/git-hooks/$name" "$DEST/$name"
	chmod +x "$hook"
	echo "installed: $name"
done

echo "Git hooks installed. Pre-commit runs: mvn spotless:check && mvn compile test-compile"
echo "Bypass once with: git commit --no-verify  or  SKIP_PRE_COMMIT=1 git commit"
