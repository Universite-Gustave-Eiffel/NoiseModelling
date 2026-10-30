#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-only
# Copyright (C) 2026 TAKT contributors
"""Prepare, run or verify a stock-only NoiseModelling tutorial screen family."""

import argparse
import csv
import filecmp
import hashlib
import json
import math
import os
from pathlib import Path
import shutil
import subprocess
import time

HERE = Path(__file__).resolve().parent
HEIGHTS = (1, 3, 5)
ORDER = ("OFF", "ON", "ON", "OFF")


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def environment(stock):
    env = {
        key: value
        for key, value in os.environ.items()
        if key
        not in {
            "JAVA_TOOL_OPTIONS",
            "JDK_JAVA_OPTIONS",
            "_JAVA_OPTIONS",
            "CLASSPATH",
            "WPS_SCRIPTS_OPTS",
        }
        and not key.startswith(("LD_", "TAKT_", "N5_", "NM_", "BENCH_"))
    }
    env.update(
        NM_STOCK=str(stock), JAVA_OPTS="-Xms128m -Xmx4g -XX:ActiveProcessorCount=1"
    )
    return env


def call(stock, directory, script, env):
    command = [
        str(stock / "bin/wps_scripts"),
        "-w",
        str(directory),
        "-s",
        str(HERE / script),
    ]
    with (directory / "STOCK.log").open("wb") as log:
        start = time.perf_counter()
        completed = subprocess.run(
            command,
            cwd=directory,
            env=env,
            stdout=log,
            stderr=subprocess.STDOUT,
            check=False,
        )
        wall = time.perf_counter() - start
    if completed.returncode:
        raise RuntimeError(
            f"Stock exit {completed.returncode}: {directory / 'STOCK.log'}"
        )
    return {"command": command, "rc": completed.returncode, "whole_process_s": wall}


def prepare(stock, root):
    root.mkdir(parents=True, exist_ok=False)
    resources = stock / "resources/org/noise_planet/noisemodelling/wps"
    source_files = sorted(
        p
        for p in resources.iterdir()
        if p.is_file()
        and p.stem.lower() in {"ground_type", "buildings", "receivers", "roads2", "dem"}
    )
    method = {
        "engine": "NoiseModelling 5.0.1; official unchanged WPS launcher and jars",
        "release_url": "https://github.com/Universite-Gustave-Eiffel/NoiseModelling/releases/tag/v5.0.1",
        "data": "Official release tutorial resources; no third-party input payload redistributed here",
        "source_resource_sha256": {p.name: digest(p) for p in source_files},
        "script_sha256": {p.name: digest(p) for p in HERE.glob("*.groovy")},
        "height_m": HEIGHTS,
        "profile_order": ORDER,
        "threads": 1,
        "reflection_order": 1,
        "maximum_source_distance_m": 150,
        "maximum_reflection_distance_m": 50,
        "diffraction_horizontal": True,
        "diffraction_vertical": True,
        "temperature_c": 20,
        "humidity_percent": 70,
        "wall_absorption": 0.1,
        "observer": "Complete native RECEIVERS_LEVEL.csv bytes, no canonicalization; no H2 physical output byte claim",
        "whole_window": "Each family: copy three sealed input DBs, three fresh stock CLIs, complete native CSV exports; verification afterwards",
        "H_rule": "max(ON family walls)/min(OFF family walls)-1; one declared OFF/ON/ON/OFF, no timing repeats",
        "JFR": "JDK profile settings, startup through process exit; CPU sample shares are not wall-time phase shares",
    }
    (root / "METHOD_PRE_RUN.json").write_text(json.dumps(method, indent=2) + "\n")
    for height in HEIGHTS:
        directory = root / "inputs" / f"H{height}"
        directory.mkdir(parents=True)
        env = environment(stock)
        env["BENCH_HEIGHT_M"] = str(height)
        call(stock, directory, "prepare_scene.groovy", env)
        method.setdefault("inputs", {})[f"H{height}"] = {
            "db_sha256": digest(directory / "h2gisdb.mv.db"),
            "scene": json.loads((directory / "SCENE.json").read_text()),
        }
    (root / "METHOD_PRE_RUN.json").write_text(json.dumps(method, indent=2) + "\n")


def run(stock, root):
    method = json.loads((root / "METHOD_PRE_RUN.json").read_text())
    for height in HEIGHTS:
        assert (
            digest(root / "inputs" / f"H{height}" / "h2gisdb.mv.db")
            == method["inputs"][f"H{height}"]["db_sha256"]
        )
    rows = []
    for index, mode in enumerate(ORDER):
        start = time.perf_counter()
        family = root / f"family_{index}_{mode}"
        family.mkdir(exist_ok=False)
        calls = []
        for height in HEIGHTS:
            directory = family / f"H{height}"
            directory.mkdir()
            shutil.copyfile(
                root / "inputs" / f"H{height}" / "h2gisdb.mv.db",
                directory / "h2gisdb.mv.db",
            )
            env = environment(stock)
            if mode == "ON":
                env["JAVA_OPTS"] += (
                    " -XX:StartFlightRecording=filename=stock.jfr,settings=profile,dumponexit=true"
                )
            call_result = call(stock, directory, "run_map.groovy", env)
            call_result.update(
                height_m=height,
                stages=json.loads((directory / "STAGES.json").read_text()),
            )
            calls.append(call_result)
            (root / "PROGRESS.json").write_text(
                json.dumps({"family": index, "mode": mode, "completed_height": height})
                + "\n"
            )
        rows.append(
            {
                "index": index,
                "mode": mode,
                "whole_family_s": time.perf_counter() - start,
                "calls": calls,
            }
        )
        (root / "RESULT.json").write_text(
            json.dumps({"method": method, "families": rows}, indent=2) + "\n"
        )
        print(
            json.dumps(
                {"family": index, "mode": mode, "wall_s": rows[-1]["whole_family_s"]}
            ),
            flush=True,
        )


def verify(root):
    data = json.loads((root / "RESULT.json").read_text())
    assert len(data["families"]) == 4
    checked = []
    for height in HEIGHTS:
        baseline = root / "family_0_OFF" / f"H{height}" / "RECEIVERS_LEVEL.csv"
        with baseline.open(newline="") as stream:
            reader = csv.DictReader(stream)
            columns = reader.fieldnames
            rows = list(reader)
        assert rows and columns
        identities = {(r["IDRECEIVER"], r["PERIOD"]) for r in rows}
        expected_receivers = data["method"]["inputs"][f"H{height}"]["scene"]["counts"][
            "RECEIVERS"
        ]
        assert len(rows) == len(identities)
        assert {r["PERIOD"] for r in rows} == {"D", "E", "N", "DEN"}
        assert len({r["IDRECEIVER"] for r in rows}) == expected_receivers
        period_counts = {
            period: sum(r["PERIOD"] == period for r in rows)
            for period in ("D", "E", "N", "DEN")
        }
        for row in rows:
            for name in columns:
                if name.startswith("HZ") or name in {"LAEQ", "LEQ"}:
                    assert math.isfinite(float(row[name]))
        for family in data["families"]:
            path = (
                root
                / f"family_{family['index']}_{family['mode']}"
                / f"H{height}"
                / "RECEIVERS_LEVEL.csv"
            )
            if family["index"] != 0:
                assert filecmp.cmp(baseline, path, shallow=False), str(path)
            checked.append(
                {
                    "file": str(path.relative_to(root)),
                    "sha256": digest(path),
                    "rows": len(rows),
                    "columns": columns,
                    "period_counts": period_counts,
                    "reference": family["index"] == 0,
                }
            )
    off = [x["whole_family_s"] for x in data["families"] if x["mode"] == "OFF"]
    on = [x["whole_family_s"] for x in data["families"] if x["mode"] == "ON"]
    H = max(on) / min(off) - 1
    result = {
        "full_csv_BYTE_EXACT": checked,
        "H_fraction": H,
        "profile_status": "RAW_OBSERVED_H_LE_5_PERCENT"
        if H <= 0.05
        else "HARNESS_DOMINATED",
        "comparison_outside_window": True,
        "H_is_observed_no_CI": True,
    }
    (root / "VERIFY_RESULT.json").write_text(json.dumps(result, indent=2) + "\n")
    print(
        json.dumps(
            {
                "full_csv_checks": len(checked),
                "H": H,
                "status": result["profile_status"],
            }
        )
    )


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=("prepare", "run", "verify"))
    parser.add_argument(
        "--stock", type=Path, required=True, help="Official 5.0.1 no-GUI distribution"
    )
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.action == "prepare":
        prepare(args.stock.resolve(), args.output.resolve())
    elif args.action == "run":
        run(args.stock.resolve(), args.output.resolve())
    else:
        verify(args.output.resolve())
