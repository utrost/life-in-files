## Overview

**lif-photo-tagging** is a command-line tool designed to automate the annotation and tagging of JPEG images within a given directory. The module leverages a Large Language Model (LLM) with vision capabilities (e.g., Gemma-3 via Ollama) to analyze image content, generate descriptive text, and extract meaningful tags (such as “NSFW”, “b/w”, “landscape”, etc.). Tags and descriptions are embedded directly into each image’s metadata (IPTC and XMP) and saved as a human-editable YAML sidecar file placed next to the original image.

Key goals include maximizing automation, maintaining flexibility in the tagging approach, and supporting integration into larger photo and content management workflows. The module is extensible and configurable, allowing future enhancements such as custom tag vocabularies and multi-language support.

**Typical use cases:**
- Automatic semantic enrichment of personal or archival photo collections
- Tagging for easier searching, filtering, or content warnings
- Consistent description and keyword extraction for later ingestion into digital asset management systems or Obsidian-based knowledge bases

lif-photo-tagging is designed for integration into the broader “Life in Files” ecosystem and follows established conventions for CLI usage, configuration, and output formats.
## Features

- **Automated Tagging and Description:**  
  Processes all `.jpg`/`.jpeg` images in a specified directory (recursively), generating a concise natural language description and relevant tags using a configurable LLM with vision capabilities.

- **Configurable LLM Integration:**  
  Supports local or remote LLMs (default: Gemma-3 via Ollama). Model endpoint and invocation options are user-configurable.

- **Efficient Image Processing:**  
  Automatically generates a temporary, resized version of each image using the ThumbnailatorResizer to optimize LLM input without altering originals.

- **Flexible Tagging Approach:**  
  By default, the LLM selects tags (“freestyle”). Optionally, users may supply a tag vocabulary file to constrain the output.

- **Multi-Target Metadata Writing:**  
  Embeds tags and descriptions into the original image’s IPTC and XMP metadata using Apache Commons Imaging.

- **YAML Sidecar Support:**  
  Stores all generated metadata (description and tags) as a sidecar YAML file (`photo.yaml`) next to the original image for easy manual review, editing, or further processing.

- **Incremental and Batch Processing:**  
  Maintains a CSV log of processed images for resumable operations. Supports skipping, updating, or reprocessing images via CLI options (`--update`, `--rerun`).

- **Selective Processing:**  
  Can filter images by modification date (`--since <date>`), enabling efficient incremental tagging.

- **Append-Mode Sidecar Updates:**  
  When updating existing YAML sidecars, new tags and descriptions are appended or merged rather than overwriting previous entries.

- **CLI-First, Scriptable Workflow:**  
  Designed for command-line use, supporting integration into automation pipelines and batch workflows.

- **Single-Threaded Simplicity:**  
  Processes images one at a time to avoid complexity and ensure predictable resource use (parallelism can be added in future versions).
## Workflow

The typical operation of **lif-photo-tagging** consists of the following steps:

1. **Directory Traversal:**  
   The tool recursively scans the specified root directory for `.jpg`/`.jpeg` image files.

2. **Image Preprocessing:**  
   For each image, a temporary, downscaled copy is created using the ThumbnailatorResizer. This version is used exclusively for LLM-based analysis to ensure efficient processing and reduced resource usage.

3. **LLM-Based Analysis:**  
   The resized image is submitted to the configured Large Language Model (e.g., Gemma-3 via Ollama).
    - The LLM analyzes the image and returns a JSON payload containing a natural language description and a set of tags (either “freestyle” or constrained by a supplied tag vocabulary file, if provided).

4. **Metadata Extraction and Formatting:**  
   The tool parses the LLM response, extracts the description and tags, and prepares the metadata for downstream use.

5. **Metadata Embedding:**
    - The description and tags are embedded directly into the original image file’s IPTC and XMP metadata fields using Apache Commons Imaging.
    - At the same time, the metadata is saved (or appended) to a YAML sidecar file named `photo.yaml`, placed alongside the image.

6. **Processed File Tracking:**
    - The tool records each processed image (including file path, processing date, tags, and description) in a central CSV log for auditing and resumption.
    - By default, images with an existing YAML sidecar are skipped, unless `--update` (append/merge) or `--rerun` (overwrite) options are specified.

7. **CLI-Driven Control:**
    - Users can control processing scope and behavior via command-line flags (e.g., specifying a start date for incremental processing using `--since <date>`).

8. **Completion and Reporting:**  
   Upon completion, the tool provides a summary of processed images, skipped files, and any encountered errors. The CSV log can be reviewed for detailed processing status.

**This workflow ensures efficient, automated enrichment of photo metadata while maintaining full auditability and user control over the tagging process.**
## Configuration

lif-photo-tagging is designed to be flexible and configurable through both command-line options and an optional configuration file. The following parameters control the tool’s behavior:

### Command-Line Options

- **`--input <directory>`**  
  Root directory to recursively scan for `.jpg`/`.jpeg` images.

- **`--since <YYYY-MM-DD>`**  
  Only process images with a modification date after the specified date.

- **`--ollama-endpoint <url>`**  
  Specify the Ollama or LLM endpoint for image analysis (default: local Ollama instance).

- **`--thumbnail-width <pixels>`**  
  Width for the temporary downscaled image (default: 512px).

- **`--tag-vocabulary <file>`**  
  Optional: YAML or plain text file listing tag vocabulary to constrain LLM output. If omitted, tagging is freestyle.

- **`--update`**  
  Update/append new tags and descriptions to existing YAML sidecars.

- **`--rerun`**  
  Overwrite existing YAML sidecars and metadata, forcing complete reprocessing.

- **`--log <csv-file>`**  
  Path to the CSV log file. If omitted, a default log is maintained in the working directory.

- **`--help`**  
  Display help and usage instructions.

### Optional Configuration File

A configuration file (`lif-photo-tagging.yaml`) may be used to specify default values for any of the above options. Command-line arguments always take precedence over configuration file settings.

**Example configuration file:**
```yaml
input: /path/to/photos
ollama-endpoint: http://localhost:11434
thumbnail-width: 512
log: lif-photo-tagging-log.csv
tag-vocabulary: tags.yaml
```
#### LLM/Backend Configuration

- Model Selection:
    By default, the tool uses Gemma-3 via Ollama, but alternative LLM endpoints or models may be specified in the configuration.

- API Invocation:
    The tool is responsible for preparing the LLM API request (base64 or binary image, prompt, etc.) and parsing the standardized JSON response.

#### Defaults and Presets

If no configuration file is provided, sensible defaults are used for all options. Only the input directory is required.
Example Invocation

java -jar lif-photo-tagging.jar --input /photos/2024 --since 2024-01-01 --update

This configuration ensures the tool can be flexibly integrated into various workflows, from ad-hoc batch runs to automated pipelines.
## CLI Usage

lif-photo-tagging operates as a standalone command-line tool. Below are the primary usage patterns, options, and practical examples.

### Basic Syntax

```sh
java -jar lif-photo-tagging.jar [options]
```
# Configuration

lif-photo-tagging is designed to be flexible and configurable through both command-line options and an optional configuration file. The following parameters control the tool’s behavior:

## Command-Line Options

- **`--input <directory>`**  
  Root directory to recursively scan for `.jpg`/`.jpeg` images.

- **`--since <YYYY-MM-DD>`**  
  Only process images with a modification date after the specified date.

- **`--ollama-endpoint <url>`**  
  Specify the Ollama or LLM endpoint for image analysis (default: local Ollama instance).

- **`--thumbnail-width <pixels>`**  
  Width for the temporary downscaled image (default: 512px).

- **`--tag-vocabulary <file>`**  
  Optional: YAML or plain text file listing tag vocabulary to constrain LLM output. If omitted, tagging is freestyle.

- **`--update`**  
  Update/append new tags and descriptions to existing YAML sidecars.

- **`--rerun`**  
  Overwrite existing YAML sidecars and metadata, forcing complete reprocessing.

- **`--log <csv-file>`**  
  Path to the CSV log file. If omitted, a default log is maintained in the working directory.

- **`--help`**  
  Display help and usage instructions.

## Optional Configuration File

A configuration file (`lif-photo-tagging.yaml`) may be used to specify default values for any of the above options. Command-line arguments always take precedence over configuration file settings.

**Example configuration file:**
```yaml
input: /path/to/photos
ollama-endpoint: http://localhost:11434
thumbnail-width: 512
log: lif-photo-tagging-log.csv
tag-vocabulary: tags.yaml
```

## LLM/Backend Configuration

- **Model Selection:**  
  By default, the tool uses Gemma-3 via Ollama, but alternative LLM endpoints or models may be specified in the configuration.

- **API Invocation:**  
  The tool is responsible for preparing the LLM API request (base64 or binary image, prompt, etc.) and parsing the standardized JSON response.

## Defaults and Presets

If no configuration file is provided, sensible defaults are used for all options. Only the input directory is required.

## Example Invocation

```sh
java -jar lif-photo-tagging.jar --input /photos/2024 --since 2024-01-01 --update
```

This configuration ensures the tool can be flexibly integrated into various workflows, from ad-hoc batch runs to automated pipelines.
## Output Formats

lif-photo-tagging produces several types of outputs to ensure seamless integration, traceability, and human readability. The following output formats are generated for each processed image:

### 1. Image Metadata (IPTC & XMP)

- **Embedded Metadata:**  
  Tags and description generated by the LLM are embedded directly into the image file’s metadata using both the IPTC and XMP standards. This ensures that metadata travels with the image and remains accessible to photo management tools and operating systems.
    - **Fields Used:**
        - *IPTC:* `Keywords`, `Caption/Abstract`, and optionally custom IPTC fields for extensibility.
        - *XMP:* Standard descriptive fields and keywords.

### 2. YAML Sidecar File

- **Filename:**  
  `photo.yaml` (placed in the same directory as the corresponding image)
- **Contents:**  
  The YAML file stores the most recent set of tags and the description, along with additional information useful for further processing or manual review.
- **Append Behavior:**  
  If the sidecar already exists, new tags/descriptions are appended or merged, never overwritten unless `--rerun` is used.

**Example `photo.yaml`:**
```yaml
filename: photo.jpg
description: "A black-and-white portrait of a woman in a vintage dress, standing by a window."
tags:
  - b/w
  - portrait
  - vintage
  - woman
  - window
  - indoor
llm:
  model: gemma-3
  endpoint: http://localhost:11434
  processed: 2024-05-27T14:15:32Z
  confidence: 0.97
```

### 3. Processing Log (CSV)

- **Filename:**  
  Configurable via `--log` (default: `lif-photo-tagging-log.csv`)
- **Contents:**  
  Each row documents one processed image, including:
    - File path
    - Processing timestamp
    - LLM model used
    - Description (truncated if long)
    - Tags (comma-separated)
    - Processing outcome/status

**Example CSV row:**
```
/photos/2024/photo.jpg,2024-05-27T14:15:32Z,gemma-3,"A black-and-white portrait...",b/w,portrait,vintage,woman,window,indoor,success
```

### 4. Error and Summary Output

- **Console Output:**  
  On completion, a concise summary is printed, detailing:
    - Number of images processed, updated, skipped, and errored
    - Any images that failed to process and the reason

### Notes

- **No JSON Sidecars:**  
  Only YAML is used for human-friendly editing and compatibility with Obsidian/Markdown workflows.
- **No File Versioning:**  
  Old YAML or metadata is replaced/merged per CLI option; no automatic backups or versioning are maintained by the tool itself.

These output formats ensure that metadata enrichment is both machine- and human-usable, easily integrated into downstream workflows, and fully auditable.
## Implementation Notes

### 1. Technology Stack

- **Programming Language:**  
  Java 17 (Maven project, package: `org.trostheide.lif.phototagging`)
- **Metadata Handling:**  
  Apache Commons Imaging for IPTC/XMP embedding
- **Image Resizing:**  
  Thumbnailator for efficient downscaling
- **YAML Processing:**  
  SnakeYAML or similar
- **CSV Logging:**  
  OpenCSV or built-in Java CSV libraries
- **HTTP/LLM API:**  
  Java HTTP client for RESTful interaction with Ollama or compatible LLM endpoints

### 2. LLM Integration

- **API Call:**  
  The tool must convert each temporary thumbnail to base64 or binary (as required by the LLM API), submit it via a POST request, and parse the standardized JSON response.
- **Prompting:**  
  The prompt sent to the LLM should request a concise English description and a list of tags. If a tag vocabulary file is provided, this should be included or referenced in the prompt.
- **Timeout/Retry:**  
  The tool should handle timeouts and retry logic for failed LLM calls, with user feedback on errors.

### 3. Metadata Embedding

- **IPTC/XMP Update:**  
  Both standards should be updated in-place. If fields exist, merge new tags/descriptions with existing ones unless `--rerun` is specified.
- **Fallback Handling:**  
  On embedding failure, log the error and continue processing other images.

### 4. YAML Sidecar Management

- **Appending/Merging:**  
  When `--update` is used, merge new tags with existing lists, preserving uniqueness and order.
- **Atomic Writes:**  
  Use a temporary file and atomic rename to prevent corruption if the process is interrupted.

### 5. CSV Logging

- **Entry Structure:**  
  Log each image’s relative path, processing time, model/endpoint used, result summary, and error status.
- **Resume Support:**  
  At startup, read the CSV log to skip previously processed files unless `--update` or `--rerun` is specified.

### 6. Error Handling

- **Robust Exception Handling:**  
  All major operations (LLM call, image write, YAML/CSV update) should be wrapped in try-catch blocks to prevent interruption of batch processing.
- **User Feedback:**  
  Summarize all errors and skipped files at the end of execution; optionally provide detailed logs for debugging.

### 7. Extensibility & Future-Proofing

- **Modular Design:**  
  Architect the tool for easy integration of alternative LLMs, additional taggers, or future enhancements (e.g., manual tag correction, multi-language support).
- **Configuration Extensibility:**  
  Add new options via configuration file/CLI as needs evolve without breaking existing workflows.

### 8. Testing & Validation

- **Test Data:**  
  Include a small test set with diverse photo types to validate all major processing branches.
- **Dry Run Mode (Future):**  
  Consider implementing a “dry-run” option for preview-only tagging in future versions.

This approach ensures maintainability, resilience, and adaptability for future requirements while delivering reliable batch image annotation today.
