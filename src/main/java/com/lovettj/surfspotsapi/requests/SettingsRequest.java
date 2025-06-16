package com.lovettj.surfspotsapi.requests;

import lombok.Data;

@Data
public class SettingsRequest {
    private String userId;
    private boolean newSurfSpotEmails;
    private boolean nearbySurfSpotsEmails;
    private boolean swellSeasonEmails;
    private boolean eventEmails;
    private boolean promotionEmails;
}