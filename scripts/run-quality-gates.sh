#!/usr/bin/env sh

set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
repo_root=$(CDPATH= cd -- "$script_dir/.." && pwd)
ui_root="$repo_root/spring-boot-iot-ui"
log_dir="$repo_root/logs"
log_file="$log_dir/quality-gates.log"
mvn_settings="$repo_root/.mvn/settings.xml"

mkdir -p "$log_dir"

resolve_executable() {
    description="$1"
    shift

    for candidate in "$@"; do
        if command -v "$candidate" >/dev/null 2>&1; then
            command -v "$candidate"
            return 0
        fi
    done

    echo "$description executable was not found in PATH." >&2
    return 1
}

write_log() {
    message="$1"
    printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$message" | tee -a "$log_file"
}

run_step() {
    step="$1"
    workdir="$2"
    shift 2

    tmp_output=$(mktemp)
    write_log "START $step"

    set +e
    (
        cd "$workdir" &&
        "$@"
    ) >"$tmp_output" 2>&1
    status=$?
    set -e

    if [ "$status" -eq 0 ]; then
        cat "$tmp_output" | tee -a "$log_file"
        rm -f "$tmp_output"
        write_log "PASS $step"
        return 0
    fi

    cat "$tmp_output" | tee -a "$log_file"
    rm -f "$tmp_output"
    write_log "FAIL $step (exit $status)"
    return "$status"
}

mvn_cmd=$(resolve_executable "Maven" mvn)
npm_cmd=$(resolve_executable "npm" npm)
node_cmd=$(resolve_executable "Node" node)
python_cmd=$(resolve_executable "Python" python3 python)

export npm_config_cache="$ui_root/.npm-cache"

write_log "Running local minimum quality gates"
if [ -f "$mvn_settings" ]; then
    write_log "Detected Maven settings: $mvn_settings"
    run_step "maven clean package -DskipTests" "$repo_root" "$mvn_cmd" -s "$mvn_settings" clean package -DskipTests
else
    write_log ".mvn/settings.xml not found, fallback to plain mvn"
    run_step "maven clean package -DskipTests" "$repo_root" "$mvn_cmd" clean package -DskipTests
fi

run_step "frontend build" "$ui_root" "$npm_cmd" run build
run_step "frontend component guard" "$ui_root" "$npm_cmd" run component:guard
run_step "frontend list guard" "$ui_root" "$npm_cmd" run list:guard
run_step "frontend style guard" "$ui_root" "$npm_cmd" run style:guard
run_step "schema baseline guard" "$repo_root" "$python_cmd" -m unittest scripts/test_risk_point_pending_promotion_schema.py -v
run_step "docs topology check" "$repo_root" "$node_cmd" scripts/docs/check-topology.mjs
write_log "All local minimum quality gates passed"
