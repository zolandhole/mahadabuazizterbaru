package com.mahad.abuaziz.utils;

public class ServiceAddress {

    private static String base_url = "http://122.248.39.157:12345/abuaziz";
    public static final String INFOKAJIAN = base_url+"/info_kajian/service_judul_kajian";
    public static final String TAMBAHUSER = base_url+"/pengguna/create_data";
    public static final String GETPROFILE = base_url+"/pengguna/get_data_id_login";
    public static final String GETDATACHAT = base_url+"/chatting/get_data_chat";
    public static final String TAMBAHCHAT = base_url+"/chatting/create_data";
    public static final String GETDATAIKLAN = base_url+"/iklan/get_data_iklan";
    public static final String GETDATAREKAMAN = base_url+"/rekaman/api_data_rekaman";
}
