package com.mahad.abuaziz.models;

public class ModelUser {
    private int id;
    private String sumber_login, id_login, nama, email;

    public ModelUser() {
    }

    public ModelUser(int id, String sumber_login, String id_login, String nama, String email) {
        this.id = id;
        this.sumber_login = sumber_login;
        this.id_login = id_login;
        this.nama = nama;
        this.email = email;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSumber_login() {
        return sumber_login;
    }

    public String getId_login() {
        return id_login;
    }

    public String getNama() {
        return nama;
    }

    public String getEmail() {
        return email;
    }
}
