package com.chrisitstyle.mediscanflow.medicalplatform.patients;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class PatientSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private PatientSpecifications() {
    }

    public static Specification<Patient> archiveFilter(boolean includeArchived) {
        return (root, query, criteriaBuilder) -> includeArchived
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.isFalse(root.get("archived"));
    }

    public static Specification<Patient> textSearch(String search) {
        if (search == null || search.isBlank()) {
            return Specification.unrestricted();
        }

        String pattern = toLikePattern(search);

        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
                containsIgnoreCase(root, criteriaBuilder, "firstName", pattern),
                containsIgnoreCase(root, criteriaBuilder, "lastName", pattern),
                containsIgnoreCase(root, criteriaBuilder, "medicalRecordNumber", pattern)
        );
    }

    private static Predicate containsIgnoreCase(
            Root<Patient> root,
            CriteriaBuilder criteriaBuilder,
            String fieldName,
            String pattern
    ) {
        Expression<String> field = root.get(fieldName);

        return criteriaBuilder.like(
                criteriaBuilder.lower(field),
                pattern,
                LIKE_ESCAPE
        );
    }

    private static String toLikePattern(String value) {
        String escapedValue = value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");

        return "%" + escapedValue + "%";
    }
}
