package com.mahad.abuaziz.models;

public class ModelChat extends RecyclerViewItem {
    private String id, pesan, jam, photo, pengirim;

    public ModelChat(String id, String pesan, String jam, String photo, String pengirim) {
        this.id = id;
        this.pesan = pesan;
        this.jam = jam;
        this.photo = photo;
        this.pengirim = pengirim;
    }

    public String getId() { return id; }

    public String getPesan() {
        return pesan;
    }

    public String getJam() {
        return jam;
    }

    public String getPhoto() {
        return photo;
    }

    public String getPengirim() {
        return pengirim;
    }
}
