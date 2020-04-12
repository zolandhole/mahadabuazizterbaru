package com.mahad.abuaziz.models;

import com.mahad.abuaziz.models.RecyclerViewItem;

public class ModelIklan extends RecyclerViewItem {
    private String PHOTOIKLAN;
    private String JUDULIKLAN;
    private String DESKRIPSIIKLAN;

    public ModelIklan(String PHOTOIKLAN, String JUDULIKLAN, String DESKRIPSIIKLAN) {
        this.PHOTOIKLAN = PHOTOIKLAN;
        this.JUDULIKLAN = JUDULIKLAN;
        this.DESKRIPSIIKLAN = DESKRIPSIIKLAN;
    }

    public String getPHOTOIKLAN() {
        return PHOTOIKLAN;
    }

    public String getJUDULIKLAN() {
        return JUDULIKLAN;
    }

    public String getDESKRIPSIIKLAN() {
        return DESKRIPSIIKLAN;
    }
}
