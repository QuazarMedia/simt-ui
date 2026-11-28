#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

python_bin="${PYTHON_BIN:-}"
if [[ -z "$python_bin" ]]; then
  for candidate in python3.13 python3.12 python3.11 python3.10; do
    if command -v "$candidate" >/dev/null 2>&1; then
      python_bin="$candidate"
      break
    fi
  done
fi
if [[ -z "$python_bin" ]] && command -v uv >/dev/null 2>&1; then
  python_bin="$(uv python find 3.12)"
fi
python_bin="${python_bin:-python3}"
"$python_bin" -c 'import sys; sys.exit("Python 3.10+ is required") if sys.version_info < (3, 10) else None'

"$python_bin" -m venv .venv
.venv/bin/pip install --quiet -r requirements.txt
.venv/bin/python sources/build.py
.venv/bin/python sources/export_ufo.py
.venv/bin/python scripts/check_ufo_sources.py
