# Stock tutorial screen-family benchmark

This manual benchmark uses the published tutorial scene shipped in the official
[NoiseModelling 5.0.1 no-GUI release](https://github.com/Universite-Gustave-Eiffel/NoiseModelling/releases/tag/v5.0.1).
It imports the same five input layers as `get_started_tutorial.groovy`, adds one
screen and runs the complete stock traffic-noise calculation and CSV export.
It requires no separate service or accelerator.

Use Java 17 and Python 3. Set `JAVA_HOME` to a JDK with the `jfr` command, and
unpack the official distribution. From this directory:

```sh
python benchmark_barriers.py prepare --stock /path/to/NoiseModelling_without_gui-5.0.1 --output /path/to/new-run
python benchmark_barriers.py run --stock /path/to/NoiseModelling_without_gui-5.0.1 --output /path/to/new-run
python benchmark_barriers.py verify --stock /path/to/NoiseModelling_without_gui-5.0.1 --output /path/to/new-run
python summarize_jfr.py /path/to/new-run
```

Preparation and measurement refuse to overwrite their respective output paths.
Keep the scripts unchanged between preparation and measurement. Setup imports
the published `buildings`, `ROADS2`, `receivers`, `dem` and `ground_type` inputs.
`METHOD_PRE_RUN.json` records hashes of all input resource files and both Groovy
scripts, the three prepared H2 database hashes and complete scene descriptions.
Keep that file with the results. H2 file hashes identify the prepared local
inputs; byte-identical independently rebuilt H2 container files are not assumed.

The new screen is 20 m long and 0.2 m wide. Placement is deterministic: scan
receivers in PK order and the six listed offsets; choose the first footprint
more than 1 m from all original roads and buildings. The tested dataset places
it near receiver 1 in EPSG:2154. The three independent inputs change only the
screen's height to 1, 3 or 5 m. `SCENE.json` stores its exact WKT, primary key,
height, placement rule and table counts before any timed run. This avoids
placing a source directly on the new screen; it is not a workaround in the
reflection algorithm.

Calculation settings are fixed: one processing thread, reflection order 1,
source distance 150 m, reflection distance 50 m, both diffraction options on,
maximum error 0, temperature 20 C, humidity 70%, wall absorption 0.1. Other
settings retain stock 5.0.1 defaults, with its scripts pinned by hash. Every
case launches a fresh stock CLI on a copy of the prepared input database and
exports **all** `RECEIVERS_LEVEL` columns and rows using the official exporter.

The declared order is OFF / ON / ON / OFF, with all three heights in each
family. ON enables the JDK's built-in `profile` JFR configuration from JVM
startup through exit; no Java agent or engine patch is used. The complete
family timer includes the input copies, three CLI lifetimes and all full CSV
exports. Per-command wall time includes JVM startup, Groovy compilation, H2
opening, computation, writing, export and shutdown. `STAGES.json` additionally
separates stock-script parsing, the complete noise call and complete export.
The noise call includes input reading, preparation, propagation and database
writing; these are not falsely presented as separately timed kernels.

Verification and JFR decoding occur afterwards. The verifier records actual native receiver/period coverage, requires unique
identities and all input receivers in the combined export, finite level columns
and byte-identical **complete**
CSV files for the same height across all four families. It does not assert raw
H2 output-file byte identity. Recorded JFR stacks are available in full; the
summary classifies each sample by the nearest recognized library frame and
also reports leaf methods. Those counts are sampled runnable-thread stacks,
not percentages of elapsed wall time or complete CPU accounting.

The observed JFR overhead is `max(ON family times)/min(OFF family times)-1` from
this single declared sequence. No repeats are selected for timing. A value over
5% marks the profile `HARNESS_DOMINATED`; common runner overhead is not
independently measured. These raw stock diagnostics do not establish an
internal wall-time capacity passport or a confidence interval.

This is a manual diagnostic benchmark, not an added CI job. It can inform the
general performance discussion in
[NoiseModelling #223](https://github.com/Universite-Gustave-Eiffel/NoiseModelling/issues/223).
That issue does not establish this screen family as its original workload.
The benchmark makes no GPU or acceleration claim.

New code: GPL-3.0-only, matching the upstream project's GPL v3 licensing.
Attribution for the original scene and resource files remains with the
NoiseModelling tutorial distribution. No original dataset, derived database,
JAR or recording is included in the patch; use the official release and its
notices. A separate dataset-specific license was not found in the tested
resource directory; this contribution does not relabel those inputs.

JFR command/configuration references:
[Java launcher](https://docs.oracle.com/en/java/javase/17/docs/specs/man/java.html)
and [jfr tool](https://docs.oracle.com/en/java/javase/17/docs/specs/man/jfr.html).

Local validation used stock 5.0.1, not the latest engine. All twelve calls
exited 0. Each complete CSV contained 3098 rows and 13 columns: DEN for 830
receivers, D/E/N for 756 each. All nine non-reference same-height comparisons
passed byte for byte. Six JFR recordings contain 5119 stack samples. Observed
JFR overhead was 5.52%, so this local profile is HARNESS_DOMINATED and remains
a raw diagnostic, without a wall-time capacity claim.
