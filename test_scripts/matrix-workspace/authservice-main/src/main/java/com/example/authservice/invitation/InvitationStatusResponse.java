package com.example.authservice.invitation;

public class InvitationStatusResponse {
    private boolean inviteOnly;

    public InvitationStatusResponse(boolean inviteOnly) {
        this.inviteOnly = inviteOnly;
    }

    public boolean isInviteOnly() {
        return inviteOnly;
    }
}
