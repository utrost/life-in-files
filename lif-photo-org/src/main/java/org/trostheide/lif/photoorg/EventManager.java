package org.trostheide.lif.photoorg;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class EventManager {

    private final List<Event> eventCalendar = new ArrayList<>();
    private final DateExtractor dateExtractor = new DateExtractor();
    private final ObjectMapper mapper;
    private final File eventFile;

    public record Event(String name, LocalDate startDate, LocalDate endDate) {
        public long durationInDays() {
            return ChronoUnit.DAYS.between(startDate, endDate) + 1;
        }
    }

    public EventManager(File targetDir) {
        this.eventFile = new File(targetDir, "lif-events.json");
        this.mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void loadEvents() {
        if (eventFile.exists()) {
            try {
                List<Event> loadedEvents = mapper.readValue(eventFile, new TypeReference<>() {});
                eventCalendar.addAll(loadedEvents);
                System.out.println(String.format("Successfully loaded %d events from %s", loadedEvents.size(), eventFile.getName()));
            } catch (IOException e) {
                System.err.println(String.format("Could not load event calendar from %s. A new one will be created.", eventFile.getName()));
                e.printStackTrace(System.err);
            }
        }
    }

    public void saveEvents() {
        try {
            mapper.writeValue(eventFile, eventCalendar);
            System.out.println(String.format("Successfully saved %d events to %s", eventCalendar.size(), eventFile.getName()));
        } catch (IOException e) {
            System.err.println(String.format("Could not save event calendar to %s", eventFile.getName()));
            e.printStackTrace(System.err);
        }
    }

    public void discoverNewEvents(File sourceRoot) {
        System.out.println("--- Starting Event Discovery Phase ---");
        try (Stream<Path> paths = Files.walk(sourceRoot.toPath())) {
            paths.filter(Files::isDirectory).forEach(dirPath -> {
                // Convert Path to File before passing to the method
                DateExtractor.PathInfo info = dateExtractor.extractPathInfo(dirPath.toFile());
                if (info != null && info.qualifier() != null) {
                    boolean eventExists = eventCalendar.stream().anyMatch(e -> e.name().equalsIgnoreCase(info.qualifier()));
                    if (!eventExists) {
                        System.out.println(String.format("Found new potential event folder: '%s' with qualifier '%s'", dirPath, info.qualifier()));
                        calculateAndStoreEventRange(dirPath.toFile(), info.qualifier());
                    } else {
                        System.out.println(String.format("[DEBUG] Skipping already known event: '%s'", info.qualifier()));
                    }
                }
            });
        } catch (IOException e) {
            System.err.println("Error during event discovery walk");
            e.printStackTrace(System.err);
        }
        System.out.println("--- Event Discovery Phase Complete ---");
        System.out.println(String.format("Final event calendar contains %d events.", eventCalendar.size()));
    }

    private void calculateAndStoreEventRange(File eventDir, String eventName) {
        List<LocalDate> dates = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(eventDir.toPath())) {
            paths.filter(Files::isRegularFile)
                    .map(path -> dateExtractor.extractPathInfo(path.toFile())) // Convert Path to File
                    .filter(Objects::nonNull)
                    .map(DateExtractor.PathInfo::date)
                    .filter(Objects::nonNull)
                    .forEach(dates::add);
        } catch (IOException e) {
            System.err.println("Could not read files in event directory: " + eventDir);
            e.printStackTrace(System.err);
            return;
        }

        if (dates.isEmpty()) {
            System.err.println(String.format("No processable files found in event folder '%s', skipping.", eventDir));
            return;
        }

        LocalDate minDate = dates.stream().min(LocalDate::compareTo).get();
        LocalDate maxDate = dates.stream().max(LocalDate::compareTo).get();

        Event event = new Event(eventName, minDate, maxDate);
        eventCalendar.add(event);
        System.out.println(String.format("=> Learned Event: '%s' spans from %s to %s (%d days)",
                event.name(), event.startDate(), event.endDate(), event.durationInDays()));
    }

    public Event findBestEventForDate(LocalDate date) {
        return eventCalendar.stream()
                .filter(event -> !date.isBefore(event.startDate()) && !date.isAfter(event.endDate()))
                .min(Comparator.comparingLong(Event::durationInDays))
                .orElse(null);
    }
}
