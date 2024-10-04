package com.lovettj.surfspotsapi.entity;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class SluggableEntity {

  protected String slug;

  // All implementing entities must have a `getName()` method for slug generation
  protected abstract String getName();

  @PrePersist
  @PreUpdate
  public void generateSlug() {
    this.slug = this.generateSlugFromName(getName());
  }

  private String generateSlugFromName(String name) {
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
