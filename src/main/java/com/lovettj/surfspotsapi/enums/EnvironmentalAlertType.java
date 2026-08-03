package com.lovettj.surfspotsapi.enums;

/**
 * Kind of environmental alert stored on {@code environmental_alert.type}.
 * UK overflow providers write {@link #SEWAGE_OVERFLOW} today.
 * Other values are reserved for the next feeds (e.g. EEA bathing water, beach closures).
 */
public enum EnvironmentalAlertType {
    /** Official bathing-water / water-quality advisory (e.g. future EEA feed). */
    WATER_QUALITY_ADVISORY,
    /** Live or recent sewage / storm overflow (UK water companies). */
    SEWAGE_OVERFLOW,
    /** Beach or bathing access closed by an authority. */
    BEACH_ACCESS_CLOSED
}
