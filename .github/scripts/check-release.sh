#!/usr/bin/env bash
# Usage: check-release.sh <tag>
# Fails (non-zero) unless the repository is ready for a public release. Reads no secrets.
set -euo pipefail
tag="${1:?usage: check-release.sh <tag>}"
root="$(cd "$(dirname "$0")/../.." && pwd)"
fail=0

if [ ! -s "$root/LICENSE" ]; then
  echo "::error::LICENSE is missing or empty. Choose a licence before tagging."
  fail=1
fi

if grep -q 'TODO(maintainer)' "$root/DATA_SOURCE.md"; then
  echo "::error::DATA_SOURCE.md still contains TODO(maintainer): record the data's origin and redistribution terms first."
  fail=1
fi

version="$(sed -n 's/^VERSION_NAME=//p' "$root/gradle.properties" | head -n1)"
if [ "$tag" != "v$version" ]; then
  echo "::error::Tag $tag does not match VERSION_NAME v$version in gradle.properties."
  fail=1
fi

exit "$fail"
