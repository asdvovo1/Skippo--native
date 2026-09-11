#!/usr/bin/env bash
# Convenience wrapper so you can run `bash setup.sh` from the project root.
set -euo pipefail
cd "$(dirname "$0")"
bash .devcontainer/setup.sh
