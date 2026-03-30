package org.example.comp2800_sas.model;

import java.util.List;

// Flattened course row used by course browsing and reporting views.
public record CourseBrowserEntry(
        Integer courseId,
        String courseCode,
        String courseTitle,
        String description,
        List<String> prerequisites
) {
    public CourseBrowserEntry {
        prerequisites = prerequisites == null ? List.of() : List.copyOf(prerequisites);
    }

    public boolean hasPrerequisites() {
        return !prerequisites.isEmpty();
    }
}
