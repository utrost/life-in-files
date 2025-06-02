
# lif-photo-faces

**Automated Face Recognition and Person Metadata Management for Photo Archives**

## Overview

`lif-photo-faces` is a Java 17 CLI tool for automated face detection and grouping in photo collections. It enables the identification of individuals in images, bi-directionally linking image files and person records. The system integrates smoothly into the *Life in Files* (lif) ecosystem and operates on open standards: Markdown, YAML, and JPEG metadata.

---

## Features

- **Automated Face Detection**: Finds faces in JPEG images using OpenCV.
- **Face Clustering**: Groups similar faces across your collection using unsupervised clustering (K-means/DBSCAN).
- **Proto-Person Creation**: For each face cluster, generates a Markdown person file (e.g., `person-1.md`) and links detected images.
- **Manual Assignment**: User manually renames proto-person files (e.g., `person-1.md` → `MaxMustermann.md`) for known individuals.
- **Bi-Directional Linking**: Maintains links between image sidecars (`.yaml`) and person Markdown files.
- **Metadata Embedding**: Updates JPEG metadata with person names for downstream compatibility.
- **CLI-Driven Workflow**: Supports detection, synchronization, dry-run, and logging, consistent with other lif-* modules.

---

## Installation

Requires:
- Java 17+
- [OpenCV for Java](https://opencv.org/releases/)
- All dependencies are managed via Maven (see `pom.xml`)

---

## Usage

### Detect & Group Faces

Scans a directory of images, detects faces, clusters by similarity, and creates proto-person Markdown files.

```sh
java -jar lif-photo-faces.jar detect   --image-dir /photos/to/scan   --person-dir /photos/persons
```

- `--image-dir`: Directory containing images (JPEG)
- `--person-dir`: Directory where person Markdown files will be created/updated

Only images with 1–3 faces will be processed. Others are ignored for precision.

### Manual Assignment

1. Review all person files in `/photos/persons/` (e.g., `person-1.md`).
2. If you recognize a person, rename the file (e.g., `person-1.md` → `MaxMustermann.md`).
3. Optionally add notes or metadata to the Markdown frontmatter.

### Synchronize Names

After renaming, update all sidecars and JPEGs to reference real names instead of proto-person IDs:

```sh
java -jar lif-photo-faces.jar sync   --image-dir /photos/to/scan   --person-dir /photos/persons
```

---

## Data Structures

### Image Sidecar Example (`IMG_001.yaml`)

```yaml
persons:
  - MaxMustermann
  - ErikaMusterfrau
```

### Person Markdown Example (`MaxMustermann.md`)

```markdown
---
person_id: MaxMustermann
aliases: [person-1]
photos:
  - IMG_001.jpg
  - IMG_042.jpg
---

# Max Mustermann

[Optional notes, added by user]
```

---

## Workflow Summary

1. **Detection**:
    - Detect faces in all images with 1–3 faces.
    - For each detected face, compute embedding and cluster.
    - Create/update proto-person files listing all matching photos.

2. **Manual Assignment**:
    - User inspects proto-person files, renames those recognized, and may add notes.

3. **Synchronization**:
    - Tool updates all YAML sidecars and JPEG metadata to use person names from Markdown.
    - Ensures links and metadata are consistent.

---

## Technical Details

- **Face Detection & Embedding**: Uses OpenCV’s DNN module (Java binding).
- **Clustering**: K-means or DBSCAN (via Smile, ELKI, or similar Java libraries).
- **Metadata Writing**: Utilizes SnakeYAML for YAML, and internal JPEG metadata writer for EXIF/XMP.
- **Markdown Handling**: Simple text IO.
- **CLI**: Apache Commons CLI.

---

## Example Directory Layout

```
/photos/to/scan/
  IMG_001.jpg
  IMG_001.yaml
  IMG_002.jpg
  IMG_002.yaml
  ...

/photos/persons/
  person-1.md
  person-2.md
  MaxMustermann.md
```

---

## Options & Parameters

| Option            | Description                                    | Required | Example                    |
|-------------------|------------------------------------------------|----------|----------------------------|
| `--image-dir`     | Directory containing JPG images and sidecars    | Yes      | `/photos/to/scan`          |
| `--person-dir`    | Directory for person Markdown files             | Yes      | `/photos/persons`          |
| `--dry-run`       | Preview actions without writing changes         | No       |                            |
| `--since-date`    | Only process images created after this date     | No       | `2024-01-01`               |
| `--thumbnail-width`| Resize faces for faster embedding (px)         | No       | `256`                      |

---

## Logging & Output

- Logs number of images processed, faces detected, clusters created, and estimated run time.
- Reports skipped files and error conditions.
- CLI outputs summary report at completion.

---

## Integration

- Designed to work alongside `lif-photo-org`, `lif-photo-tagging`, and other *Life in Files* tools.
- Open formats ensure portability to Obsidian, PhotoPrism, and other tools.

---

## License

MIT License (or your preferred open-source license)

---

## Roadmap & Future Ideas

- Interactive UI for merging/splitting person clusters
- Support for video frame extraction
- Export to other metadata standards (XMP, MWG, etc.)
- Improved recognition (integrate with external APIs or models)
