## Overview

**lif-photo-tagging** is a command-line tool designed to automate the annotation and tagging of JPEG images within a given directory. The module leverages a Large Language Model (LLM) with vision capabilities (e.g., Gemma-3 via Ollama) to analyze image content, generate descriptive text, and extract meaningful tags (such as “NSFW”, “b/w”, “landscape”, etc.). Tags and descriptions are saved as a human-editable YAML sidecar file placed next to the original image. (Embedding into IPTC/XMP is planned for a future version.)

Key goals include maximizing automation, maintaining flexibility in the tagging approach, and supporting integration into larger photo and content management workflows. The module is extensible and configurable, allowing future enhancements such as custom tag vocabularies and multi-language support.

**Typical use cases:**
- Automatic semantic enrichment of personal or archival photo collections
- Tagging for easier searching, filtering, or content warnings
- Consistent description and keyword extraction for later ingestion into digital asset management systems or Obsidian-based knowledge bases

lif-photo-tagging is designed for integration into the broader “Life in Files” ecosystem and follows established conventions for CLI usage, configuration, and output formats.

## Features

- **Automated Tagging and Description**  
  Processes all `.jpg`/`.jpeg` images in a specified directory (recursively), generating a concise natural language description and relevant tags using a configurable LLM with vision capabilities.

- **Configurable LLM Integration**  
  Supports local LLMs via Ollama (default: Gemma-3). The model, prompt, and API endpoint are fully configurable.

- **Efficient Image Processing**  
  Automatically generates a temporary, resized version of each image to optimize input for the LLM. Original images are never altered.

- **Flexible Tagging Approach**  
  By default, the LLM generates tags freely. Optionally, a user-defined tag list may be provided to constrain or prioritize specific keywords.

- **YAML Sidecar Support**  
  Stores all generated metadata (description, tags, source image name, timestamp, and model used) as a sidecar YAML file (`photo.yaml`) next to the original image for easy manual review, editing, or further processing.

- **Incremental and Batch Processing**  
  Supports skipping already-tagged files by default. CLI options allow forced reprocessing (`--rerun`) or updating existing sidecar files (`--update`).

- **Selective Processing**  
  Can filter images by creation date (`--since <date>`), enabling efficient incremental processing of new or updated images.

- **CLI-First, Scriptable Workflow**  
  Designed for command-line use, supporting integration into automation pipelines and batch workflows.

- **Single-Threaded Simplicity**  
  Processes images one at a time to avoid complexity and ensure predictable resource use. Parallelism can be added in future versions.
## Workflow

The typical operation of **lif-photo-tagging** consists of the following steps:

1. **Directory Traversal**  
   The tool recursively scans the specified root directory for `.jpg`/`.jpeg` image files.

2. **Image Preprocessing**  
   For each image, a temporary, downscaled copy is created using the internal `ThumbnailUtils`. This version is used exclusively for LLM-based analysis to ensure efficient processing and reduced resource usage.

3. **LLM-Based Analysis**  
   The resized image is submitted to the configured Large Language Model (e.g., Gemma-3 via Ollama) via its `/api/generate` endpoint.
  - The LLM analyzes the image and returns a JSON object containing a natural language description and a list of tags (either “freestyle” or based on an optional supplied tag vocabulary).

4. **Metadata Extraction and Formatting**  
   The tool parses the LLM response, extracts the `description` and `tags`, and prepares a metadata object suitable for writing.

5. **Sidecar YAML Writing**  
   The metadata is saved to a YAML sidecar file (`<original-image>.yaml`) placed alongside the original image.
  - Each sidecar includes fields for description, tags, timestamp, source filename, full file path, and the LLM model used.
  - If the file already exists, it is skipped by default unless `--update` or `--rerun` is specified.

6. **Sidecar Update Logic**
  - With `--update`, the sidecar is overwritten to reflect the latest LLM output.
  - With `--rerun`, all images are reprocessed regardless of existing sidecars.

7. **CLI-Driven Control**  
   Users can control processing scope and behavior via command-line flags such as `--since <date>`, `--dry-run`, `--width`, and `--model`.

8. **Completion and Reporting**  
   Upon completion, the tool prints a summary of processed, skipped, and failed images. Errors are shown inline. Future versions may add optional CSV or structured logging.

**This workflow ensures efficient, automated enrichment of photo metadata while maintaining full user control and transparency throughout the tagging process.**
## Configuration

lif-photo-tagging is designed to be flexible and configurable through command-line options. The following parameters control the tool’s behavior:

### Command-Line Options

- **`--input <directory>`**  
  Root directory to recursively scan for `.jpg`/`.jpeg` images.

- **`--since <YYYY-MM-DD>`**  
  Only process images with a creation date on or after the specified date.

- **`--model <name>`**  
  Name of the LLM model to use (default: `gemma3:4b`).

- **`--prompt <text>`**  
  Custom prompt to override the default instruction sent to the LLM.

- **`--width <pixels>`**  
  Width for the temporary downscaled image (default: 512).

- **`--tags <tag1,tag2,...>`**  
  Optional: Comma-separated list of tags to constrain or influence LLM output. If omitted, tagging is freestyle.

- **`--update`**  
  Update existing YAML sidecars with new results from the LLM.

- **`--rerun`**  
  Reprocess all images, even those with existing sidecar files. Overrides `--update`.

- **`--dry-run`**  
  Perform all steps except writing YAML files. Useful for previewing actions.

- **`--help`**  
  Display help and usage instructions.

### Optional Configuration File

*Note: Configuration file support is not implemented in the current version. All options must be passed via command line.*  
Future versions may support reading from a YAML file (e.g., `lif-photo-tagging.yaml`) to define default values for CLI options.

### LLM/Backend Configuration

- **Model Selection**  
  By default, the tool uses `gemma3:4b` served via a local Ollama instance (`http://localhost:11434/api/generate`). The model name, prompt, and API endpoint can be overridden via CLI options.

- **API Invocation**  
  The tool encodes a resized version of each image as a base64 string and sends it to the configured Ollama endpoint. The response is expected as a structured JSON object containing `description` and `tags` fields.

### Defaults and Presets

Sensible defaults are provided for all optional parameters:
- Default model: `gemma3:4b`
- Default endpoint: `http://localhost:11434/api/generate`
- Default thumbnail width: `512`
- Default prompt: _Describe this image briefly and provide a list of relevant tags._

Only the `--input` directory is required for a successful run.

**Example Invocation**

```sh
java -jar lif-photo-tagging.jar --input /photos/2024 --since 2024-01-01 --update
```
# Configuration

lif-photo-tagging is designed to be flexible and configurable through command-line options. The following parameters control the tool’s behavior:

## Command-Line Options

- **`--input <directory>`**  
  Root directory to recursively scan for `.jpg`/`.jpeg` images. _(Required)_

- **`--since <YYYY-MM-DD>`**  
  Only process images with a creation date on or after the specified date. Useful for incremental processing.

- **`--model <name>`**  
  Name of the LLM model to use (default: `gemma3:4b`).

- **`--prompt <text>`**  
  Custom prompt sent to the LLM (default: “Describe this image briefly and provide a list of relevant tags.”).

- **`--width <pixels>`**  
  Width for the temporary downscaled image sent to the LLM (default: 512).

- **`--tags <tag1,tag2,...>`**  
  Optional: Comma-separated list of tags to guide or constrain LLM output. If omitted, the model tags freely.

- **`--update`**  
  Update and overwrite existing YAML sidecar files with fresh descriptions and tags.

- **`--rerun`**  
  Reprocess all images, even if a sidecar file already exists. Overrides `--update`.

- **`--dry-run`**  
  Perform all steps without writing any output files. Useful for testing.

- **`--help`**  
  Display usage instructions.


## Optional Configuration File

*Note: A YAML configuration file is not yet supported in the current version.*  
All options must be specified via the command line. Support for optional config files may be added in a future release.

## LLM/Backend Configuration

- **Model Selection:**  
  By default, the tool uses `gemma3:4b` via Ollama, targeting the `/api/generate` endpoint on `http://localhost:11434`. This can be overridden using the `--model` CLI option.

- **API Invocation:**  
  The tool resizes each image, encodes it in base64, and sends it to the LLM with a configurable prompt. The response is expected as structured JSON with `description` and `tags`. The prompt, model, and tag constraints can be customized via CLI.

## Defaults and Presets

If no values are specified for optional parameters, the following defaults apply:

- **Model:** `gemma3:4b`
- **Prompt:** “Describe this image briefly and provide a list of relevant tags.”
- **Thumbnail width:** `512`
- **Tag list:** *(freestyle)*
- **Dry-run:** Disabled
- **Update/rerun:** Disabled (images with YAML sidecars are skipped)

Only `--input` is required.

## Example Invocation

```sh
java -jar lif-photo-tagging.jar --input /photos/2024 --since 2024-01-01 --update
```
This command will scan the directory /photos/2024, process all JPEGs created since January 1, 2024, and update their metadata even if sidecar files already exist. The tool is CLI-first and designed for easy integration into ad-hoc scripts or fully automated pipelines.
## Output Formats

lif-photo-tagging produces sidecar metadata files to ensure seamless integration, auditability, and human readability. The following outputs are generated for each processed image:

### 1. YAML Sidecar File

- **Filename:**  
  `photo.yaml` (placed in the same directory as the corresponding image)

- **Contents:**  
  The YAML file stores the most recent description and list of tags generated by the LLM, along with the full path to the original image and model metadata.

- **Append Behavior:**  
  If the sidecar already exists:
  - It is skipped by default.
  - If `--update` is passed, the contents are merged/appended.
  - If `--rerun` is passed, it is completely overwritten.

**Example `photo.yaml`:**
```yaml
filename: photo.jpg
path: /photos/2024/05/27/photo.jpg
description: "A black-and-white portrait of a woman in a vintage dress, standing by a window."
tags:
  - b/w
  - portrait
  - vintage
  - woman
  - window
  - indoor
llm:
  model: gemma3:4b
  endpoint: http://localhost:11434/api/generate
  processed: 2025-05-30T10:15:32Z
```
2. Embedded Metadata (Planned Feature)

   Note: Embedding of IPTC or XMP metadata directly into the image files is not implemented in the current version. Future versions may support embedding descriptions and keywords into standard fields like:

        IPTC: Keywords, Caption/Abstract

        XMP: dc:description, dc:subject, photoshop:Headline

This output strategy ensures that tagging results are durable, portable, and easy to integrate into additional workflows such as photo cataloging, backups, or knowledge bases.

### 3. Processing Log (CSV) — Planned Feature

- **Status:**  
  A CSV-based processing log is planned but not yet implemented.

- **Intended Behavior:**  
  In future versions, this log will capture each image processed, along with:
  - File path
  - Timestamp of processing
  - LLM model used
  - Description (truncated if long)
  - Tags (comma-separated)
  - Outcome or error status

- **Planned Filename:**  
  Default: `lif-photo-tagging-log.csv`  
  Customizable via a future `--log` CLI flag.

**Example CSV row (planned format):**

/photos/2024/photo.jpg,2025-05-30T10:15:32Z,gemma3:4b,"A black-and-white portrait...",b/w,portrait,vintage,woman,window,indoor,success


### 4. Error and Summary Output

- **Console Output:**  
  After execution, a concise summary is printed, showing:
  - Number of images processed, updated, skipped, and errored
  - Details for any images that failed to process

### Notes

- **No JSON Sidecars:**  
  YAML is the sole format used for sidecar metadata. It was chosen for readability and seamless integration with Markdown-based workflows (e.g., Obsidian).

- **No File Versioning:**  
  Existing YAML files are skipped, appended, or overwritten based on CLI options. The tool does not maintain historical versions or backups of old metadata.

These output formats emphasize both human usability and long-term portability while leaving room for enhanced machine-readability and auditing in future releases.## Implementation Notes

## Technical Design and Implementation

### 1. Technology Stack

- **Programming Language:**  
  Java 17 (Maven project, package: `org.trostheide.lif.phototagging`)

- **Metadata Handling:**  
  Apache Commons Imaging (planned) — IPTC/XMP metadata embedding is not yet implemented.

- **Image Resizing:**  
  Thumbnailator for efficient temporary thumbnail generation (implemented)

- **YAML Processing:**  
  SnakeYAML (used for reading/writing sidecar metadata files)

- **CSV Logging:**  
  Not yet implemented — placeholder feature described in README

- **HTTP/LLM API:**  
  Java HTTP client used to call Ollama-compatible endpoints via `POST /api/generate` with `base64` image payloads

### 2. LLM Integration

- **API Call:**  
  The resized image is encoded as base64 and submitted to the LLM endpoint (Ollama/Gemma-3) with an appropriate prompt and `format: "json"` (implemented)

- **Prompting:**  
  Uses a fixed prompt:  
  `"Describe this image briefly and provide a list of relevant tags."`  
  A future update could append vocabulary constraints when `--tag-vocabulary` is supplied (partially implemented)

- **Timeout/Retry:**  
  Basic HTTP error handling is included; retry logic is not yet implemented (planned)

### 3. Metadata Embedding

- **IPTC/XMP Update:**  
  Not yet implemented — currently only YAML sidecars are written (planned)

- **Fallback Handling:**  
  YAML generation is robust; other metadata embedding to come with error handling

### 4. YAML Sidecar Management

- **Appending/Merging:**  
  Fully implemented — new tags are merged with existing ones if `--update` is used

- **Atomic Writes:**  
  Currently uses a simple overwrite strategy; atomic temp-file rename may be added (planned)

### 5. CSV Logging

- **Entry Structure:**  
  Logging format is documented but not yet active — to be added in future release

- **Resume Support:**  
  Currently based on `.yaml` sidecar presence (`--update`, `--rerun` flags control reprocessing)

### 6. Error Handling

- **Robust Exception Handling:**  
  Each image is processed in isolation, and errors are logged per file (implemented)

- **User Feedback:**  
  Summary statistics printed at the end, with per-file error messages (implemented)

### 7. Extensibility & Future-Proofing

- **Modular Design:**  
  Code is structured with clear separation of concerns:
  - CLI
  - Configuration
  - LLM communication
  - Thumbnail generation
  - YAML writing

- **Configuration Extensibility:**  
  CLI and YAML configuration are unified; defaults provided with override logic (implemented)

### 8. Testing & Validation

- **Test Data:**  
  Real-world photo directories are supported; no bundled test data yet (planned)

- **Dry Run Mode:**  
  Planned for future implementation — not yet available

---

This implementation provides a reliable foundation for automated image annotation using LLMs, with a roadmap for enhancements like metadata embedding, dry-run support, CSV logging, and multi-language tagging.
