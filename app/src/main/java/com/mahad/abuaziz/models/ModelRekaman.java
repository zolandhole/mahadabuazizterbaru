package com.mahad.abuaziz.models;

public class ModelRekaman extends RecyclerViewItem {
    private String id, nama, upload_date, status;

    public ModelRekaman(String id, String nama, String upload_date, String status) {
        this.id = id;
        this.nama = nama;
        this.upload_date = upload_date;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getNama() {
        return nama;
    }

    public String getUpload_date() {
        return upload_date;
    }

    public String getStatus() {
        return status;
    }
}
