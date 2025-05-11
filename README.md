# Life in Files (LiF) – Project Specification

## Overview

**Life in Files (LiF)** is a modular suite of Java 17 command-line tools designed to manage, organize, and enrich different types of personal and archival data — including photos, documents, ebooks, music, and more. Each tool operates independently, but all are developed in a shared monorepo using a consistent architecture and configuration style.

---

## Technology Stack

- **Language:** Java 17
- **Build System:** Maven (multi-module setup)
- **IDE:** IntelliJ IDEA
- **Repository Host:** [GitHub.com/utrost](https://github.com/utrost)
- **Main Package:** `org.trostheide.lif`
  - Submodules use subpackages, e.g.:
    - `org.trostheide.lif.photos`
    - `org.trostheide.lif.docs`
    - `org.trostheide.lif.ebooks`
    - `org.trostheide.lif.music`
- **Library Preference:** Use **Apache Commons** libraries wherever applicable

---

## Repository Structure

### Repository: `life-in-files`

```
life-in-files/
├── pom.xml                    # Maven parent project
├── lif-core/                  # Shared logic, utilities, config, logging
├── lif-photo-org/             # Photo organizing, sorting, rendering, resizing
├── lif-photo-ocr/             # OCR text extraction from photos
├── lif-photo-faces/           # Face clustering and tagging
├── lif-photo-tagging/         # AI-generated photo captions and tags
├── lif-docs/                  # Document sorting and OCR handling
├── lif-ebooks/                # Ebook metadata extraction and categorization
├── lif-music/                 # Music metadata cleanup and organization
├── config-examples/          # Sample configuration files
├── sample-input/             # Test data and input examples
├── docs/                     # Project documentation and specs
└── README.md
```

---

## Common Features

- Java 17-based tools, CLI-first
- Designed for **manual execution** but extensible to automation
- External configuration via JSON/YAML files
- Dry-run, full-run, and delta-run execution modes
- Detailed logging and optional manifest tracking
- Source directory is always **read-only**; processing occurs via **copy/render/enrich to target**

---

## Shared Module: `lif-core`

Provides utilities and base classes for all tools:
- `RunContext`: manages mode (`dry-run`, `delta-run`, `full-run`)
- `FileOps`: abstraction for file handling (dry-run-aware)
- `ConfigLoader`: loads and validates external configuration
- `LoggerService`: standard logging framework
- `ManifestWriter`: optional tracking of processed files

---

## Module Overview

| Module               | Description                                                         | Status       |
|----------------------|---------------------------------------------------------------------|--------------|
| `lif-photo-org`      | Organizes, resizes, and converts photos from RAW to JPEG            | In Development |
| `lif-photo-ocr`      | OCR text extraction from image files                                | Planned      |
| `lif-photo-faces`    | Detects and clusters faces, enables tagging and metadata sync       | Planned      |
| `lif-photo-tagging`  | AI-based captioning and keyword tagging using OpenAI or local model | Planned      |
| `lif-docs`           | Organizes scanned PDFs and documents by type, year, source          | Planned      |
| `lif-ebooks`         | Extracts metadata and organizes EPUB/PDF files                      | Planned      |
| `lif-music`          | Cleans and organizes music metadata, sorts by album/artist/year     | Planned      |

---

## Development Guidelines

- Each module lives in its own subdirectory with independent `pom.xml`
- Dependencies are managed via the root `pom.xml` (Maven BOM style)
- Shared logic must live in `lif-core` to avoid duplication
- Logging should be consistent and structured
- Run modes must be implemented through `RunContext` to ensure consistency
- Use **Apache Commons** libraries (e.g., commons-io, commons-imaging) where suitable

---

## GitHub Guidelines

- Single repository: `life-in-files`
- Hosted under GitHub user: [`utrost`](https://github.com/utrost)
- One `README.md` with module-specific sections
- GitHub Projects used to track work across modules
- Per-module release tags (e.g., `lif-photo-org-v1.0`)
- Use GitHub Actions CI for per-module builds and checks

---

## Future Enhancements

- Optional web-based dashboard for status and metrics
- GUI launcher wrapper
- Systemd/cron integration
- Tag-based search index for Obsidian or local web viewer
- Offline and local LLM/image model integration

---
