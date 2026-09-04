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
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {

            // Header row
            writer.writeNext(new String[]{
                    "ID", "Student", "Subject", "Type", "Item Name",
                    "Date Missed", "Deadline", "Status", "Notes"
            });

            // Data rows
            for (MissedItem item : items) {
                writer.writeNext(new String[]{
                        String.valueOf(item.getId()),
                        item.getStudentName(),
                        item.getSubjectCode() + " - " + item.getSubjectName(),
                        item.getItemType(),
                        item.getItemName(),
                        item.getDateMissed() != null ? item.getDateMissed().toString() : "",
                        item.getDeadline()   != null ? item.getDeadline().toString()   : "",
                        item.getStatus(),
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
