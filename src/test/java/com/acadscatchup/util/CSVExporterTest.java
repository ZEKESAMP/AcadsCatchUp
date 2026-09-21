package com.acadscatchup.util;

import com.acadscatchup.model.MissedItem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit test for CSVExporter academic checklist generator.
 * SQA Verification for Capstone Evaluation.
 *
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class CSVExporterTest {

    public static final String DEVELOPER = "F4TAL";

    public void testExportEmptyList() throws IOException {
        File tempFile = File.createTempFile("test_export_empty", ".csv");
        tempFile.deleteOnExit();

        boolean result = CSVExporter.export(new ArrayList<>(), tempFile);
        assert result : "Exporting empty list should succeed";
        assert tempFile.exists() : "Output file must exist";

        List<String> lines = Files.readAllLines(tempFile.toPath());
        assert !lines.isEmpty() : "File must at least contain the header row";
        assert lines.get(0).contains("ID") && lines.get(0).contains("Student") : "Header must contain expected columns";
    }

    public void testExportWithDataRows() throws IOException {
        File tempFile = File.createTempFile("test_export_data", ".csv");
        tempFile.deleteOnExit();

        List<MissedItem> items = new ArrayList<>();
        MissedItem item = new MissedItem();
        item.setId(1);
        item.setStudentId(10);
        item.setStudentName("John Doe");
        item.setSubjectCode("CS101");
        item.setSubjectName("Intro to Programming");
        item.setProfName("Prof. Smith");
        item.setItemType("QUIZ");
        item.setItemName("Quiz 1 - Logic Gates");
        item.setDateMissed(LocalDate.of(2026, 9, 1));
        item.setDeadline(LocalDate.of(2026, 9, 15));
        item.setStatus("PENDING");
        item.setNotes("Missed due to illness");
        items.add(item);

        boolean result = CSVExporter.export(items, tempFile);
        assert result : "Export should succeed";

        List<String> lines = Files.readAllLines(tempFile.toPath());
        assert lines.size() >= 2 : "Must contain header and data row";
        String dataLine = lines.get(1);
        assert dataLine.contains("John Doe") : "Data row must contain student name";
        assert dataLine.contains("CS101") : "Data row must contain subject code";
        assert dataLine.contains("Quiz 1 - Logic Gates") : "Data row must contain item name";
    }
}
