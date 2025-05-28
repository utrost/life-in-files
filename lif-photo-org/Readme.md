## Table of Contents

1. [Overview](#overview)
2. [Features](#features)
3. [Prerequisites](#prerequisites)
4. [Installation](#installation)
5. [Quick Start](#quick-start)
6. [CLI Usage](#cli-usage)
    - [Options](#options)
    - [Examples](#examples)
7. [Configuration](#configuration)
8. [Development](#development)
    - [Project Structure](#project-structure)
    - [Building](#building)
    - [Testing](#testing)

## Overview

**lif-photo-org** is a command-line application for organizing, converting, and resizing photos and raw images on disk. It mirrors the directory structure of your source folder into a target folder, processes each image (RAW or JPEG) in parallel, and maintains a JSON index of all operations.

At a high level, **lif-photo-org**:

- **Discovers** new or updated image files (JPEG, PNG, TIFF, CR2, DNG, NEF, etc.) under a specified source directory.
- **Converts** RAW files to JPEG (or processes JPEGs directly) using either:
    - **Darktable CLI** (for raw → JPEG conversion + resizing + full metadata preservation), or
    - **ImageIO + Thumbnailator** (for fast in-JVM resizing of JPEGs).
- **Preserves** EXIF/XMP metadata when producing resized JPEGs.
- **Mirrors** your folder hierarchy at any depth under a separate target root, so your albums remain organized.
- **Tracks** progress in real time and writes a `.lif-index.json` file alongside outputs to record source→output mappings and timestamps.
- **Scales** across multiple CPU cores via a configurable thread pool, with safety around Darktable’s database lock.

This tool is ideal for photographers and archivists who need a repeatable, automatable pipeline to process large collections of raw and JPEG images without losing metadata or folder context.

---

## Features

- Recursive directory scanning with optional date-based filtering
- Support for RAW formats (CR2, DNG, NEF, etc.) and standard image types (JPEG, PNG, TIFF)
- Two processing modes:
    - **raw**: uses Darktable CLI for conversion, resizing, and metadata preservation
    - **jpeg**: uses Java’s ImageIO & Thumbnailator for fast in-JVM resizing, with EXIF reinjection
- Mirrored output directory structure
- Configurable maximum output resolution (`--longside`)
- Parallel processing via thread pool (`--threads`)
- Real-time progress reporting
- Persistent JSON index (`.lif-index.json`) of all processed files

---

## Prerequisites

- Java 17 or later
- Maven 3.6+
- For **raw** mode: [darktable-cli](https://www.darktable.org/) installed and on your `PATH`
- Internet access only needed for dependency resolution; all processing is local

---

## Installation

Before you begin, ensure you have:

- **Java 17** or later installed
- **Maven 3.6+**
- For **raw** mode: `darktable-cli` installed and on your `PATH`

Then clone and build:

```bash
# 1) Clone the repository
git clone https://github.com/your-org/lif-photo-org.git
cd lif-photo-org

# 2) Build the “fat JAR” (includes all dependencies)
mvn clean package -DskipTests

# 3) Locate the runnable JAR
ls lif-photo-org/target/lif-photo-org-*-jar-with-dependencies.jar
```
You can now run lif-photo-org directly via:
```bash
java -jar lif-photo-org/target/lif-photo-org-0.1.0-SNAPSHOT-jar-with-dependencies.jar [options]
```
Replace 0.1.0-SNAPSHOT with the actual version number produced by your build.
## Quick Start

Once you have built the “fat” JAR, you can process your photos in one simple command:

```bash
java -jar lif-photo-org/target/lif-photo-org-0.1.0-SNAPSHOT-jar-with-dependencies.jar \
  --source /path/to/your/photos \
  --target /path/to/processed/output \
  --mode raw \
  --longside 3000 \
  --threads 4
  --darktable-path "/Applications/darktable.app/Contents/MacOS/darktable-cli" \
  ```
--source: root folder containing your RAW/JPEG images
--target: empty (or existing) folder where processed JPEGs will be written
--mode: raw to use Darktable CLI (recommended for RAW → JPEG + metadata), or jpeg for in-JVM resizing of JPEGs
--longside: maximum length (in pixels) of the longer side (here: 3000)
--threads: number of parallel workers (here: 4)
--darktable-path: full path to the darktable-cli binary (default: darktable-cli on your PATH)

This command will:

- Recursively scan /path/to/your/photos for supported image files.
- Mirror the full directory hierarchy under /path/to/processed/output.
- Convert and resize each image (capped at 3000 px on its long side), preserving all EXIF/XMP metadata.
- Write a .lif-index.json file in the target root recording every source→output mapping.
## CLI Usage

### Options

| Option                     | Description                                                                          | Default                        |
|----------------------------|--------------------------------------------------------------------------------------|--------------------------------|
| `-s`, `--source <dir>`     | Source directory to scan (required)                                                  | —                              |
| `-t`, `--target <dir>`     | Target directory for output (required)                                               | —                              |
| `-m`, `--mode <raw|jpeg>`  | Processing mode: <br>`raw` → Darktable CLI for conversion+resize<br>`jpeg` → ImageIO+Thumbnailator for JPEG resizing | —                              |
| `--longside <pixels>`      | Maximum length of the longer side (0 = no resize)                                    | `0`                            |
| `--since <ISO>`            | Only include files modified on or after this ISO-8601 timestamp                       | *none*                         |
| `--extensions <csv>`       | Comma-separated list of file extensions to include (e.g. `jpg,png,cr2`)              | All common image/RAW types     |
| `--threads <n>`            | Number of parallel worker threads                                                     | Number of CPU cores            |
| `--darktable-path <path>`  | Full path to the darktable-cli binary (for raw mode)                                    |             |
| `-h`, `--help`             | Show help message                                                                    | —                              |

### Examples

```bash
# 1) JPEG-only fast resize
java -jar lif-photo-org.jar \
  --source ~/Pictures \
  --target ~/Pictures/resized \
  --mode jpeg \
  --longside 2000

# 2) RAW conversion + resize
java -jar lif-photo-org.jar \
  --source ~/RawPhotos \
  --target ~/Processed \
  --mode raw \
  --longside 4000 \
  --since 2025-01-01T00:00:00Z

# 3) Filter to specific extensions
java -jar lif-photo-org.jar \
  --source . \
  --target ./out \
  --mode jpeg \
  --extensions jpg,tif,png
```
## Configuration

All settings in **lif-photo-org** are provided via command-line options—there is no separate configuration file.

- CLI flags control every aspect of the run (source, target, mode, filters, threading, etc.)
- You can combine flags in scripts or shell aliases for reusable workflows.
- Environment variables are not read by the tool; if you need to customize defaults, wrap the `java -jar` invocation in your own shell function.

Example alias in Bash:

```bash
alias lif-jpeg='java -jar /path/to/lif-photo-org.jar --mode jpeg --longside 2000 --threads 4'
```
Feel free to script ```--since``` or ```--extensions``` combinations as needed.

## Development

### Project Structure
```bash
if-parent/
├── pom.xml # Parent POM (Java version, shared dependencyManagement)
├── lif-core/ # Shared utilities module
│ ├── pom.xml
│ └── src/
│ ├── main/java/... # ConfigLoader, LoggerService, LifIndexManager, etc.
│ └── test/java/... # Core unit tests
└── lif-photo-org/ # Photo-org application module
├── pom.xml
└── src/
├── main/java/... # CLI, DirectoryScanner, PhotoProcessor, decoders
└── test/java/... # JPEG/RAW processing integration tests
```

### Building

From the project root (`lif-parent`):

```bash
# Compile, run all tests, and install artifacts to your local Maven repo
mvn clean install
```
To build only the photo-org fat-jar (skipping tests for speed):
```bash
cd lif-photo-org
mvn clean package -DskipTests
# Result: target/lif-photo-org-<version>-jar-with-dependencies.jar
```
Testing

Run the full test suite (unit + integration):
```bash
mvn test
```
Or to run tests for only one module, e.g. photo-org:
```bash
cd lif-photo-org
mvn test
```

