package com.acadscatchup.util;

import com.acadscatchup.model.MissedItem;
import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Exports missed items to a CSV file.
 * @author F4TAL
 */
public class CSVExporter {

    public static final String DEVELOPER = "F4TAL";

    public static boolean export(List<MissedItem> items, String filePath) {
        return export(items, new java.io.File(filePath));
    }

    public static boolean export(List<MissedItem> items, java.io.File file) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {

            // Header row
            writer.writeNext(new String[]{
                    "ID", "Student", "Subject", "Prof Name", "Type", "Item Name",
                    "Date Missed", "Deadline", "Status", "Notes"
            });

            // Data rows
            for (MissedItem item : items) {
                String subjectDisplay = item.getSubjectCode() != null
                        ? (item.getSubjectName() != null && !item.getSubjectName().isBlank()
                            ? item.getSubjectCode() + " - " + item.getSubjectName()
                            : item.getSubjectCode())
                        : "";
                writer.writeNext(new String[]{
                        String.valueOf(item.getId()),
                        item.getStudentName() != null ? item.getStudentName() : "",
                        subjectDisplay,
                        item.getProfName() != null ? item.getProfName() : "",
                        item.getItemType() != null ? item.getItemType() : "",
                        item.getItemName() != null ? item.getItemName() : "",
                        item.getDateMissed() != null ? item.getDateMissed().toString() : "",
                        item.getDeadline()   != null ? item.getDeadline().toString()   : "",
                        item.getStatus() != null ? item.getStatus() : "",
                        item.getNotes() != null ? item.getNotes() : ""
                });
            }
            return true;

        } catch (IOException e) {
            System.err.println("CSV export error: " + e.getMessage());
            return false;
        }
    }
}
