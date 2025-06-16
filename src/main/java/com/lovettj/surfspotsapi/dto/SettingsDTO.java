package com.lovettj.surfspotsapi.dto;

import com.lovettj.surfspotsapi.entity.Settings;
import lombok.Data;

@Data
public class SettingsDTO {
    private boolean newSurfSpotEmails;
    private boolean nearbySurfSpotsEmails;
    private boolean swellSeasonEmails;
    private boolean eventEmails;
    private boolean promotionEmails;

    public SettingsDTO(Settings settings) {
        this.newSurfSpotEmails = settings.isNewSurfSpotEmails();
        this.nearbySurfSpotsEmails = settings.isNearbySurfSpotsEmails();
        this.swellSeasonEmails = settings.isSwellSeasonEmails();
        this.eventEmails = settings.isEventEmails();
        this.promotionEmails = settings.isPromotionEmails();
    }
} 