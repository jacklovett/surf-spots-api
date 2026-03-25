package com.lovettj.surfspotsapi.entity;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Data;

@Data
@MappedSuperclass
public abstract class SluggableEntity {

    protected String slug;

    // All implementing entities must have a `getName()` method for slug generation
    protected abstract String getName();

    @PrePersist
    @PreUpdate
    public void generateSlug() {
        this.slug = slugFromName(getName());
    }

    /**
     * Same rules as {@link #generateSlug()} for a given name. Used by seeding and lookups
     * so JSON names match persisted rows by slug even when trimmed-name keys differ (e.g. Unicode whitespace).
     */
    public static String slugFromName(String name) {
        if (name == null) {
            return null;
        }
        // Slug generation logic: convert to lowercase, replace spaces with hyphens, and
        // remove non-alphanumeric characters
        return name.toLowerCase()
                .replaceAll("\\s+", "-") // Replace spaces with hyphens
                .replaceAll("[^a-z0-9\\-]", ""); // Remove all non-alphanumeric characters except hyphens
    }
}
