package com.mahad.abuaziz.utils;

import org.json.JSONObject;

public interface ResponShoutcast {
    void result(JSONObject jsonObject);
    void failed(String error);
}
