package org.trostheide.lif.photoorg;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class DateExtractor {

    private static final DateTimeFormatter EXIF_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy:MM:dd[ HH:mm:ss]");

    public record PathInfo(LocalDate date, String qualifier) {}

    private static final Map<String, Month> ALL_MONTHS = new HashMap<>();
    static {
        Stream.of(
                new String[][] {
                        {"januar", "january"}, {"februar", "february"}, {"märz", "maerz", "march"},
                        {"april", "april"}, {"mai", "may"}, {"juni", "june"}, {"juli", "july"},
                        {"august", "august"}, {"september", "september"}, {"oktober", "october"},
                        {"november", "november"}, {"dezember", "december"}
                }
        ).forEach(names -> {
            Month month = Month.valueOf(names[names.length - 1].toUpperCase());
            for (String name : names) {
                ALL_MONTHS.put(name, month);
            }
        });
    }

    private static final Set<String> SUPPORTED_IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "tif", "tiff", "cr2", "nef", "arw", "dng", "orf", "raf", "rw2", "pef", "srw", "kdc"
    );

    private static final String MONTH_REGEX_PART = String.join("|", ALL_MONTHS.keySet());
    private static final String YEAR_REGEX_PART = "(19[89]\\d|20\\d\\d)";

    private static final Pattern QUALIFIER_MONTH_YEAR_PATTERN = Pattern.compile(
            "^(.*?)\\s+(" + MONTH_REGEX_PART + ")\\s+" + YEAR_REGEX_PART + "\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern YEAR_MONTH_QUALIFIER_PATTERN = Pattern.compile(
            "^" + YEAR_REGEX_PART + "\\s+(" + MONTH_REGEX_PART + ")\\s+(.*?)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern YYYY_MM_QUALIFIER_PATTERN = Pattern.compile(
            "^" + YEAR_REGEX_PART + "[-_](\\d{1,2})\\s+(.*?)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern MONTH_YEAR_PATTERN = Pattern.compile(
            "^(" + MONTH_REGEX_PART + ")\\s+" + YEAR_REGEX_PART + "\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern YEAR_MONTH_PATTERN = Pattern.compile(
            "^" + YEAR_REGEX_PART + "\\s+(" + MONTH_REGEX_PART + ")\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern YYYY_MM_ONLY_PATTERN = Pattern.compile(
            "^" + YEAR_REGEX_PART + "[-_](\\d{1,2})\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern YYYY_QUALIFIER_PATTERN = Pattern.compile(
            "^" + YEAR_REGEX_PART + "\\s+[-_]?\\s+(.*?)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern YYYY_ONLY_PATTERN = Pattern.compile(
            "^" + YEAR_REGEX_PART + "$");


    public PathInfo extractPathInfo(File file) {
        if (file.isDirectory()) {
            return getInfoFromPath(file);
        }

        if (!isSupportedImageFile(file)) {
            return null;
        }

        PathInfo pathInfo = getInfoFromPath(file.getParentFile());
        if (pathInfo != null) {
            System.out.println(String.format("[DEBUG] Using info from path for %s: %s", file.getName(), pathInfo));
            return pathInfo;
        }

        LocalDate exifDate = getDateFromExif(file);
        if (exifDate != null) {
            System.out.println(String.format("[DEBUG] Using date from EXIF for %s: %s", file.getName(), exifDate));
            return new PathInfo(exifDate.withDayOfMonth(1), null);
        }

        System.err.println(String.format("[WARN] No date in path or EXIF for %s, falling back to file system date.", file.getName()));
        return new PathInfo(getDateFromFileSystem(file).withDayOfMonth(1), null);
    }

    private PathInfo getInfoFromPath(File directory) {
        if (directory == null) return null;
        String dirName = directory.getName();
        Matcher m;

        // PRIORITY 1: Patterns with month names are most specific.
        m = QUALIFIER_MONTH_YEAR_PATTERN.matcher(dirName);
        if (m.matches()) {
            System.out.println("[DEBUG] Matched Rule 1 (Qualifier Month Year) for: " + dirName);
            int year = Integer.parseInt(m.group(3));
            Month month = ALL_MONTHS.get(m.group(2).toLowerCase());
            String qualifier = sanitizeQualifier(m.group(1));
            return new PathInfo(LocalDate.of(year, month, 1), qualifier);
        }

        m = YEAR_MONTH_QUALIFIER_PATTERN.matcher(dirName);
        if (m.matches()) {
            System.out.println("[DEBUG] Matched Rule 2 (Year Month Qualifier) for: " + dirName);
            int year = Integer.parseInt(m.group(1));
            Month month = ALL_MONTHS.get(m.group(2).toLowerCase());
            String qualifier = sanitizeQualifier(m.group(3));
            return new PathInfo(LocalDate.of(year, month, 1), qualifier);
        }

        m = MONTH_YEAR_PATTERN.matcher(dirName);
        if (m.matches()) {
            System.out.println("[DEBUG] Matched Rule 3 (Month Year) for: " + dirName);
            int year = Integer.parseInt(m.group(2));
            Month month = ALL_MONTHS.get(m.group(1).toLowerCase());
            return new PathInfo(LocalDate.of(year, month, 1), null);
        }

        m = YEAR_MONTH_PATTERN.matcher(dirName);
        if (m.matches()) {
            System.out.println("[DEBUG] Matched Rule 4 (Year Month) for: " + dirName);
            int year = Integer.parseInt(m.group(1));
            Month month = ALL_MONTHS.get(m.group(2).toLowerCase());
            return new PathInfo(LocalDate.of(year, month, 1), null);
        }

        // PRIORITY 2: Patterns with YYYY-MM format.
        m = YYYY_MM_QUALIFIER_PATTERN.matcher(dirName);
        if (m.matches()) {
            System.out.println("[DEBUG] Matched Rule 5 (YYYY-MM Qualifier) for: " + dirName);
            int year = Integer.parseInt(m.group(1));
            int month = Integer.parseInt(m.group(2));
            String qualifier = sanitizeQualifier(m.group(3));
            return new PathInfo(LocalDate.of(year, month, 1), qualifier);
        }

        m = YYYY_MM_ONLY_PATTERN.matcher(dirName);
        if (m.matches()) {
            System.out.println("[DEBUG] Matched Rule 6 (YYYY-MM Only) for: " + dirName);
            int year = Integer.parseInt(m.group(1));
            int month = Integer.parseInt(m.group(2));
            return new PathInfo(LocalDate.of(year, month, 1), null);
        }

        // PRIORITY 3: Generic patterns as a last resort.
        m = YYYY_QUALIFIER_PATTERN.matcher(dirName);
        if (m.matches()) {
            System.out.println("[DEBUG] Matched Rule 7 (YYYY Qualifier) for: " + dirName);
            int year = Integer.parseInt(m.group(1));
            String qualifier = sanitizeQualifier(m.group(2));
            return new PathInfo(LocalDate.of(year, 1, 1), qualifier);
        }

        m = YYYY_ONLY_PATTERN.matcher(dirName);
        if (m.matches()) {
            System.out.println("[DEBUG] Matched Rule 8 (YYYY Only) for: " + dirName);
            int year = Integer.parseInt(m.group(1));
            return new PathInfo(LocalDate.of(year, 1, 1), null);
        }

        return null;
    }

    private String sanitizeQualifier(String raw) {
        if (raw == null) return null;
        String cleaned = raw.trim()
                .replaceAll("[^a-zA-Z0-9\\säöüÄÖÜß-]", "")
                .replaceAll("\\s+", " ");
        return cleaned.isEmpty() ? null : cleaned;
    }

    private boolean isSupportedImageFile(File file) {
        String name = file.getName().toLowerCase();
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex == -1) return false;
        String extension = name.substring(dotIndex + 1);
        return SUPPORTED_IMAGE_EXTENSIONS.contains(extension);
    }

    private LocalDate getDateFromExif(File file) {
        if (!isSupportedImageFile(file)) {
            return null;
        }

        try {
            ImageMetadata metadata = Imaging.getMetadata(file);
            TiffImageMetadata exif = null;

            if (metadata instanceof JpegImageMetadata) {
                exif = ((JpegImageMetadata) metadata).getExif();
            } else if (metadata instanceof TiffImageMetadata) {
                exif = (TiffImageMetadata) metadata;
            }

            if (exif != null) {
                TiffField dateField = exif.findField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
                if (dateField != null) {
                    String dateStr = dateField.getStringValue().trim();
                    return LocalDate.parse(dateStr, EXIF_DATE_FORMATTER);
                }
            }
        } catch (DateTimeParseException e) {
            System.err.println(String.format("[WARN] Could not parse EXIF date for %s: %s", file.getName(), e.getMessage()));
        } catch (Exception e) {
            // Ignore other exceptions
        }
        return null;
    }

    private LocalDate getDateFromFileSystem(File file) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            return attrs.creationTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        } catch (IOException e) {
            System.err.println(String.format("[ERROR] Could not read file attributes for %s, using current date as last resort.", file.getName()));
            e.printStackTrace(System.err);
            return LocalDate.now();
        }
    }
}
