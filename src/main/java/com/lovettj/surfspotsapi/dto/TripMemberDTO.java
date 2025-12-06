package com.lovettj.surfspotsapi.dto;

import com.lovettj.surfspotsapi.entity.TripMember;
import com.lovettj.surfspotsapi.entity.TripInvitation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripMemberDTO {
    private String id;
    private String userId; // null for pending invitations
    private String userName; // null for pending invitations
    private String userEmail;
    private LocalDateTime addedAt; // null for pending invitations
    private String status; // "ACCEPTED" for members, "PENDING" for invitations
    private LocalDateTime invitedAt; // for pending invitations

    // Constructor for accepted members
    public TripMemberDTO(TripMember tripMember) {
        this.id = tripMember.getId();
        this.userId = tripMember.getUser().getId();
        this.userName = tripMember.getUser().getName();
        this.userEmail = tripMember.getUser().getEmail();
        this.addedAt = tripMember.getAddedAt();
        this.status = "ACCEPTED";
    }

    // Constructor for pending invitations
    public TripMemberDTO(TripInvitation invitation) {
        this.id = invitation.getId();
        this.userId = null;
        this.userName = null;
        this.userEmail = invitation.getEmail();
        this.addedAt = null;
        this.status = invitation.getStatus();
        this.invitedAt = invitation.getInvitedAt();
    }
}




