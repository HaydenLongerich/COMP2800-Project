package org.example.comp2800_sas.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EnrollmentCatalogOption(
        @JsonProperty("option_number") String optionNumber,
        String status,
        String session,
        List<EnrollmentCatalogSection> sections
) {
    public EnrollmentCatalogOption {
        sections = sections == null ? List.of() : List.copyOf(sections);
    }

    public boolean isOpen() {
        return status != null && "open".equalsIgnoreCase(status.trim());
    }
}
