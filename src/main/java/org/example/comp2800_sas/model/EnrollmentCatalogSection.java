package org.example.comp2800_sas.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Immutable section-level meeting, instructor, and room details inside a catalog option.
@JsonIgnoreProperties(ignoreUnknown = true)
public record EnrollmentCatalogSection(
        @JsonProperty("section_type") String sectionType,
        String section,
        @JsonProperty("class_nbr") String classNbr,
        @JsonProperty("meeting_dates") String meetingDates,
        String days,
        String time,
        String room,
        String instructor,
        @JsonProperty("seats_text") String seatsText,
        @JsonProperty("seats_open") Integer seatsOpen,
        @JsonProperty("seats_capacity") Integer seatsCapacity,
        @JsonProperty("manual_entry_required") Boolean manualEntryRequired,
        String notes
) {
    public boolean hasSeatCounts() {
        return seatsOpen != null || seatsCapacity != null;
    }
}
