package org.example.comp2800_sas.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EnrollmentCatalogCourse(
        @JsonProperty("course_code") String courseCode,
        @JsonProperty("course_name") String courseName,
        @JsonProperty("class_option_count") Integer classOptionCount,
        @JsonProperty("detail_url") String detailUrl,
        String description,
        String units,
        String grading,
        String components,
        @JsonProperty("course_career") String courseCareer,
        List<EnrollmentCatalogOption> options
) {
    public EnrollmentCatalogCourse {
        options = options == null ? List.of() : List.copyOf(options);
    }

    public int optionCount() {
        if (classOptionCount != null && classOptionCount > 0) {
            return classOptionCount;
        }
        return options.size();
    }
}
