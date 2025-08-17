# lif-photo-org

## Table of Contents

1.  [Overview](#overview)
2.  [Features](#features)
3.  [CLI Usage](#cli-usage)
    -   [Options](#options)
    -   [Examples](#examples)
4.  [Sorting Modes Explained](#sorting-modes-explained)
    -   [Structure Mode (`--order structure`)](#structure-mode---order-structure)
    -   [Date Mode (`--order date`)](#date-mode---order-date)
    -   [Event Mode (`--order event`)](#event-mode---order-event)
5.  [Prerequisites & Installation](#prerequisites--installation)
6.  [Development](#development)

## Overview

**lif-photo-org** is a command-line application for organizing, converting, and resizing photos and raw images. It processes images from a source directory, converts them to high-quality JPEGs, and organizes them in a target directory based on one of three powerful sorting modes: `structure`, `date`, or `event`.

It is designed to be a robust, stateful assistant for building a clean, context-aware photo archive.

---

## Features

-   Recursive directory scanning with optional date-based filtering.
-   Support for RAW formats (CR2, DNG, NEF, etc.) and standard image types (JPEG, PNG, TIFF).
-   Two processing backends:
    -   **`raw` mode**: Uses **Darktable CLI** for high-quality conversion and full metadata preservation.
    -   **`jpeg` mode**: Uses Java’s **Thumbnailator** library for fast in-JVM resizing of JPEGs, with EXIF metadata preservation.
-   Three powerful output ordering modes: `structure`, `date`, and `event`.
-   **Intelligent Event Discovery**: Automatically learns events (e.g., "Rom", "Hochzeit Dirk") and their date ranges from your folder structure.
-   **Persistent Event Calendar**: Saves learned events to a `lif-events.json` file, allowing it to build a knowledge base of your photo archive over time.
-   Parallel processing across multiple CPU cores.

---

## CLI Usage

### Options

| Option | Description | Default |
| --- | --- | --- |
| `-s`, `--source <dir>` | Source directory to scan (required). | — |
| `-t`, `--target <dir>` | Target directory for output (required). | — |
| `-m`, `--mode <raw\|jpeg>` | Processing mode. | — |
| `-o`, `--order <mode>` | Output folder layout: `structure`, `date`, or `event`. | `structure` |
| `--event-rescan` | Forces a full rescan of all events, ignoring the saved `lif-events.json` cache. | `false` |
| `--longside <px>` | Maximum length of the longer side (0 = no resize). | `0` |
| `--quality <1-100>` | JPEG compression quality percentage. | `95` |
| `--since <ISO>` | Only include files modified on or after this ISO-8601 timestamp. | *none* |
| `--threads <n>` | Number of parallel worker threads. | CPU cores |
| ... | *(other options)* | |

### Examples

```bash
# 1) Simple RAW conversion, mirroring the original folder structure
java -jar lif-photo-org-*-jar-with-dependencies.jar \
  --source ~/PhotosRaw \
  --target ~/PhotosArchive \
  --mode raw \
  --order structure

# 2) Intelligent event-based sorting
# This will first learn events from your source folders, then sort all photos accordingly.
java -jar lif-photo-org-*-jar-with-dependencies.jar \
  --source "/mnt/Archive 8TB/Photos/Uwe" \
  --target "/home/uwe/Documents/Photos/Archive" \
  --mode raw \
  --order event \
  --threads 16

# 3) Force a full rescan of all events, ignoring the cache
java -jar lif-photo-org-*-jar-with-dependencies.jar \
  --source "/mnt/Archive 8TB/Photos/Uwe" \
  --target "/home/uwe/Documents/Photos/Archive" \
  --mode raw \
  --order event \
  --event-rescan
```
Sorting Modes Explained
Structure Mode (--order structure)

This is the default mode. It creates an exact mirror of your source directory structure in the target directory. It's ideal for when your photos are already perfectly organized.
Date Mode (--order date)

This mode reorganizes your photos into a clean YYYY/MM structure. It uses a hybrid logic to determine the correct date for each photo, prioritizing in this order:

    Date from Folder Name: Detects dates in formats like "2016 - Rom", "Dezember 2018", or "1998-08".
    EXIF Date: If the folder name contains no date, it reads the original capture date from the photo's EXIF metadata.
    File System Date: As a last resort, it uses the file's creation date.

Event Mode (--order event)

This is the most powerful mode. It turns the tool into a stateful, context-aware assistant.

    Learning Phase: The tool first scans your entire source directory to find "event folders" (e.g., /Travel/2016 - Rom). It analyzes the photos within to learn the exact date range of each event (e.g., "Rom" took place from May 3rd to May 16th, 2016).
    Saving Knowledge: This "event calendar" is saved to a lif-events.json file in your target directory. On subsequent runs, the tool loads this knowledge and only scans for new events.
    Processing Phase: For every single photo, it checks if its capture date falls within a known event period.

        If it matches (e.g., a photo from your mobile phone taken on May 10th, 2016), it will be sorted into .../2016/05/Rom.
        If it doesn't match, it falls back to the standard date mode logic.

This allows you to automatically group photos from different sources (DSLR, mobile phone, friends' cameras) into a single, contextually correct event folder.
Prerequisites & Installation

    Java 17 or later

    Maven 3.6+

    For raw mode: darktable-cli installed and on your PATH

# 1) Clone the repository
git clone <repository-url>
cd life-in-files

# 2) Build the entire project from the root directory
mvn clean install

# 3) The runnable JAR will be in the module's target folder
ls lif-photo-org/target/lif-photo-org-*-jar-with-dependencies.jar

