#!/usr/bin/env bash
set -u

TARGETS=(
  "https://chatgpt.com"
  "https://openai.com"
  "https://github.com"
)

TIMEOUT=15
PROXY_URL="${PROXY_URL:-http://127.0.0.1:7890}"

usage() {
  cat <<USAGE
Usage: $(basename "$0") [options]

Options:
  -t, --timeout <seconds>    curl timeout seconds (default: ${TIMEOUT})
  -p, --proxy <url>          proxy url for forced proxy checks (default: ${PROXY_URL})
      --targets <csv>        override targets, e.g. https://a.com,https://b.com
  -h, --help                 show help

Examples:
  $(basename "$0")
  $(basename "$0") --proxy http://127.0.0.1:7890
  $(basename "$0") --targets https://chatgpt.com,https://openai.com
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -t|--timeout)
      TIMEOUT="$2"
      shift 2
      ;;
    -p|--proxy)
      PROXY_URL="$2"
      shift 2
      ;;
    --targets)
      IFS=',' read -r -a TARGETS <<< "$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 2
      ;;
  esac
done

check_one() {
  local mode="$1"
  local url="$2"
  local cmd=(curl -I -L --max-time "$TIMEOUT" -sS -o /dev/null \
    -w 'code=%{http_code} connect=%{time_connect}s tls=%{time_appconnect}s starttransfer=%{time_starttransfer}s total=%{time_total}s')

  case "$mode" in
    direct)
      cmd+=(--noproxy '*')
      ;;
    env)
      :
      ;;
    forced-proxy)
      cmd+=(--proxy "$PROXY_URL")
      ;;
    *)
      echo "invalid mode: $mode" >&2
      return 2
      ;;
  esac

  cmd+=("$url")

  local output rc
  output="$("${cmd[@]}" 2>&1)"
  rc=$?

  printf '  %-12s | rc=%-3s | %s\n' "$mode" "$rc" "$output"

  if [[ $rc -eq 0 ]]; then
    return 0
  fi
  return 1
}

print_env_summary() {
  echo "== Proxy env summary =="
  for k in http_proxy https_proxy HTTP_PROXY HTTPS_PROXY all_proxy ALL_PROXY no_proxy NO_PROXY; do
    if [[ -n "${!k-}" ]]; then
      printf '  %s=%s\n' "$k" "${!k}"
    fi
  done
  echo
}

main() {
  echo "== Network self-check =="
  date '+  time: %F %T %Z'
  echo "  timeout: ${TIMEOUT}s"
  echo "  forced proxy: ${PROXY_URL}"
  echo

  print_env_summary

  local direct_ok=0 env_ok=0 forced_ok=0
  local total=${#TARGETS[@]}

  for url in "${TARGETS[@]}"; do
    echo "Target: ${url}"
    check_one direct "$url" && ((direct_ok+=1)) || true
    check_one env "$url" && ((env_ok+=1)) || true
    check_one forced-proxy "$url" && ((forced_ok+=1)) || true
    echo
  done

  echo "== Summary =="
  echo "  direct success:      ${direct_ok}/${total}"
  echo "  env proxy success:   ${env_ok}/${total}"
  echo "  forced proxy success:${forced_ok}/${total}"
  echo

  if [[ $env_ok -eq $total && $direct_ok -lt $total ]]; then
    cat <<'TIP'
Diagnosis: Direct access is restricted, but proxy access works.
Suggestion:
  1) Keep using your goproxy alias in shell sessions.
  2) For Codex/Desktop apps, ensure system-wide proxy/TUN mode is enabled.
  3) Ensure these domains are forced through proxy rules:
     chatgpt.com, *.chatgpt.com, openai.com, *.openai.com, *.oaistatic.com
TIP
    return 0
  fi

  if [[ $env_ok -lt $total && $forced_ok -lt $total ]]; then
    cat <<'TIP'
Diagnosis: Proxy path is unstable or blocked.
Suggestion:
  1) Verify local proxy service listens on the configured host/port.
  2) Switch proxy node/region and re-test.
  3) Check proxy ACL/rules are not rejecting CONNECT for HTTPS domains.
TIP
    return 1
  fi

  cat <<'TIP'
Diagnosis: Network status is mixed.
Suggestion:
  1) Compare env proxy and forced proxy results above.
  2) If forced proxy works but env does not, check shell export scope.
  3) If direct works and proxy fails, disable proxy for general traffic or fix proxy config.
TIP
}

main
