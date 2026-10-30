#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-only
# Copyright (C) 2026 TAKT contributors
"""Decode complete JFR recordings after timing; report CPU stack samples only."""

import argparse
import json
import os
import shutil
import subprocess
from collections import Counter
from pathlib import Path

PREFIXES = (
    ("org/noise_planet/noisemodelling/pathfinder/", "noise_pathfinder"),
    ("org/noise_planet/noisemodelling/propagation/", "noise_propagation"),
    ("org/noise_planet/noisemodelling/emission/", "noise_emission"),
    ("org/noise_planet/noisemodelling/jdbc/", "noise_jdbc"),
    ("org/locationtech/jts/", "geometry"),
    ("org/h2/", "database"),
    ("org/codehaus/groovy/", "groovy"),
)


def summarize_events(events, recording):
    """Summarize recorded frames without treating truncated stacks as complete."""
    if not events:
        raise ValueError(f"No stack sample events: {recording}")
    categories, leaves, event_types = Counter(), Counter(), Counter()
    truncated = 0
    for event in events:
        event_types[event["type"]] += 1
        stack = event["values"].get("stackTrace") or {}
        frames = stack.get("frames") or []
        truncated += bool(stack.get("truncated"))
        methods = [
            f["method"]["type"]["name"].replace(".", "/") + "/" + f["method"]["name"]
            for f in frames
        ]
        leaves[methods[0] if methods else "NO_STACK"] += 1
        category = "other_or_no_stack"
        for method in methods:
            match = next(
                (label for prefix, label in PREFIXES if method.startswith(prefix)),
                None,
            )
            if match:
                category = match
                break
        categories[category] += 1
    return {
        "recording": recording,
        "events": len(events),
        "event_types": event_types,
        "truncated_stacks": truncated,
        "nearest_recognized_stack_categories": categories,
        "top_leaf_methods": leaves.most_common(20),
        "unit": "recorded runnable-thread stack samples, not wall seconds or complete CPU accounting",
    }


def summarize(root):
    tool = (
        Path(os.environ["JAVA_HOME"]) / "bin/jfr"
        if "JAVA_HOME" in os.environ
        else shutil.which("jfr")
    )
    summaries = []
    for path in sorted(root.glob("family_*_ON/H*/stock.jfr")):
        result = subprocess.run(
            [
                str(tool),
                "print",
                "--json",
                "--stack-depth",
                "128",
                "--events",
                "jdk.ExecutionSample,jdk.NativeMethodSample",
                str(path),
            ],
            check=True,
            text=True,
            capture_output=True,
        )
        path.with_suffix(".samples.json").write_text(result.stdout)
        events = json.loads(result.stdout)["recording"]["events"]
        summaries.append(summarize_events(events, str(path.relative_to(root))))
    if len(summaries) != 6:
        raise ValueError("Expected two profiled families of three inputs")
    (root / "JFR_SUMMARY.json").write_text(json.dumps(summaries, indent=2) + "\n")
    print(
        json.dumps(
            {
                "recordings": len(summaries),
                "total_samples": sum(x["events"] for x in summaries),
            }
        )
    )


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    summarize(args.output.resolve())
