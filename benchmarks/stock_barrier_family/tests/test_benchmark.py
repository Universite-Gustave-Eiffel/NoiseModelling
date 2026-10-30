# SPDX-License-Identifier: GPL-3.0-only
# Copyright (C) 2026 TAKT contributors
"""Small synthetic fixtures; no stock download, Java or timed run required."""

import contextlib
import csv
import importlib.util
import io
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

HERE = Path(__file__).resolve().parent


def load_module(name):
    spec = importlib.util.spec_from_file_location(name, HERE.parent / f"{name}.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


benchmark = load_module("benchmark_barriers")
jfr = load_module("summarize_jfr")


class VerifierTests(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        fixture = (HERE / "fixtures/levels.csv").read_bytes()
        self.result = {
            "families": [
                {"index": i, "mode": mode, "whole_family_s": 1.0}
                for i, mode in enumerate(benchmark.ORDER)
            ],
            "method": {
                "inputs": {
                    f"H{height}": {"scene": {"counts": {"RECEIVERS": 2}}}
                    for height in benchmark.HEIGHTS
                }
            },
        }
        (self.root / "RESULT.json").write_text(json.dumps(self.result))
        for family in self.result["families"]:
            for height in benchmark.HEIGHTS:
                path = self.csv_path(family["index"], height)
                path.parent.mkdir(parents=True)
                path.write_bytes(fixture)

    def csv_path(self, index=0, height=1):
        return (
            self.root
            / f"family_{index}_{benchmark.ORDER[index]}"
            / f"H{height}"
            / "RECEIVERS_LEVEL.csv"
        )

    def verify(self):
        with contextlib.redirect_stdout(io.StringIO()):
            benchmark.verify(self.root)

    def test_complete_identical_exports_pass(self):
        self.verify()
        result = json.loads((self.root / "VERIFY_RESULT.json").read_text())
        self.assertEqual(len(result["full_csv_BYTE_EXACT"]), 12)

    def test_nonidentical_csv_rejected_even_in_nonlevel_field(self):
        path = self.csv_path(1)
        path.write_bytes(path.read_bytes().replace(b"POINT(0 0)", b"POINT(9 9)", 1))
        with self.assertRaisesRegex(ValueError, "Complete CSV bytes differ"):
            self.verify()

    def test_duplicate_identity_rejected(self):
        path = self.csv_path()
        lines = path.read_text().splitlines(keepends=True)
        path.write_text("".join(lines + [lines[1]]))
        with self.assertRaisesRegex(ValueError, "Duplicate receiver/period"):
            self.verify()

    def test_nonfinite_levels_rejected(self):
        path = self.csv_path()
        original = path.read_bytes()
        for value in (b"nan", b"inf", b"-inf"):
            with self.subTest(value=value):
                path.write_bytes(
                    original.replace(b",30,31,32", b"," + value + b",31,32", 1)
                )
                with self.assertRaisesRegex(ValueError, "Non-finite level"):
                    self.verify()

    def test_missing_receiver_record_rejected(self):
        path = self.csv_path()
        lines = path.read_text().splitlines(keepends=True)
        path.write_text("".join(lines[:-1]))
        with self.assertRaisesRegex(ValueError, "receiver records"):
            self.verify()

    def test_missing_nonreference_period_record_rejected(self):
        path = self.csv_path(2)
        lines = path.read_text().splitlines(keepends=True)
        path.write_text("".join(lines[:1] + lines[2:]))
        with self.assertRaisesRegex(ValueError, "Complete CSV bytes differ"):
            self.verify()

    def test_missing_export_file_rejected(self):
        self.csv_path(3).unlink()
        with self.assertRaises(FileNotFoundError):
            self.verify()

    def test_malformed_row_shape_rejected(self):
        path = self.csv_path()
        with path.open(newline="") as stream:
            rows = list(csv.reader(stream))
        rows[1].append("unexpected")
        with path.open("w", newline="") as stream:
            csv.writer(stream).writerows(rows)
        with self.assertRaisesRegex(ValueError, "row shape"):
            self.verify()


class PinTests(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        self.stock = self.root / "stock"
        for name in benchmark.STOCK_SCRIPTS:
            path = self.stock / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(f"// synthetic stock fixture {name}\n")
        self.resource = (
            self.stock / "resources/org/noise_planet/noisemodelling/wps/dem.geojson"
        )
        self.resource.parent.mkdir(parents=True)
        self.resource.write_text("{}\n")
        self.method = {
            "stock_script_sha256": benchmark.stock_script_hashes(self.stock),
            "script_sha256": {
                p.name: benchmark.digest(p) for p in benchmark.HERE.glob("*.groovy")
            },
            "source_resource_sha256": {
                self.resource.name: benchmark.digest(self.resource)
            },
            "inputs": {},
        }
        for height in benchmark.HEIGHTS:
            path = self.root / "inputs" / f"H{height}" / "h2gisdb.mv.db"
            path.parent.mkdir(parents=True)
            path.write_bytes(b"synthetic prepared input")
            self.method["inputs"][f"H{height}"] = {"db_sha256": benchmark.digest(path)}

    def test_unchanged_pins_pass(self):
        benchmark.check_pins(self.stock, self.root, self.method)

    def test_each_loaded_stock_script_mismatch_rejected(self):
        for name in benchmark.STOCK_SCRIPTS:
            with self.subTest(script=name):
                path = self.stock / name
                original = path.read_bytes()
                path.write_bytes(original + b"// changed\n")
                with self.assertRaisesRegex(ValueError, "Stock script hashes"):
                    benchmark.check_pins(self.stock, self.root, self.method)
                path.write_bytes(original)

    def test_prepared_input_mismatch_stops_run_before_launch(self):
        path = self.root / "inputs/H1/h2gisdb.mv.db"
        path.write_bytes(b"changed")
        (self.root / "METHOD_PRE_RUN.json").write_text(json.dumps(self.method))
        with patch.object(benchmark, "call") as call:
            with self.assertRaisesRegex(ValueError, "database hash differs"):
                benchmark.run(self.stock, self.root)
            call.assert_not_called()
        self.assertFalse((self.root / "family_0_OFF").exists())

    def test_resource_mismatch_rejected(self):
        self.resource.write_text("changed\n")
        with self.assertRaisesRegex(ValueError, "Input resource hash"):
            benchmark.check_pins(self.stock, self.root, self.method)

    def test_wrapper_mismatch_rejected(self):
        self.method["script_sha256"]["run_map.groovy"] = "0" * 64
        with self.assertRaisesRegex(ValueError, "Benchmark Groovy script"):
            benchmark.check_pins(self.stock, self.root, self.method)


class JfrTests(unittest.TestCase):
    def test_truncated_stacks_counted_without_inventing_frames(self):
        events = json.loads((HERE / "fixtures/jfr_samples.json").read_text())[
            "recording"
        ]["events"]
        summary = jfr.summarize_events(events, "synthetic.jfr")
        self.assertEqual(summary["events"], 4)
        self.assertEqual(summary["truncated_stacks"], 2)
        self.assertEqual(
            summary["event_types"],
            {"jdk.ExecutionSample": 3, "jdk.NativeMethodSample": 1},
        )
        self.assertEqual(
            summary["nearest_recognized_stack_categories"],
            {
                "noise_pathfinder": 1,
                "geometry": 1,
                "other_or_no_stack": 2,
            },
        )
        self.assertEqual(dict(summary["top_leaf_methods"])["NO_STACK"], 2)
        self.assertNotIn(
            "noise_propagation", summary["nearest_recognized_stack_categories"]
        )

    def test_empty_events_rejected(self):
        with self.assertRaisesRegex(ValueError, "No stack sample events"):
            jfr.summarize_events([], "empty.jfr")

    def test_missing_recordings_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaisesRegex(ValueError, "two profiled families"):
                jfr.summarize(Path(directory))


if __name__ == "__main__":
    unittest.main()
