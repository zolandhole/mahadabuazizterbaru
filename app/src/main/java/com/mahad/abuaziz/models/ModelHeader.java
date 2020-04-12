package com.mahad.abuaziz.models;

public class ModelHeader extends RecyclerViewItem {
    private String NAMAPROFILE;
    private String EMAILPROFILE;
    private String IMAGEURL;

    public ModelHeader(String NAMAPROFILE, String EMAILPROFILE, String IMAGEURL) {
        this.NAMAPROFILE = NAMAPROFILE;
        this.EMAILPROFILE = EMAILPROFILE;
        this.IMAGEURL = IMAGEURL;
    }

    public String getNAMAPROFILE() { return NAMAPROFILE; }
    public String getEMAILPROFILE() {
        return EMAILPROFILE;
    }
    public String getIMAGEURL() {
        return IMAGEURL;
    }
}
