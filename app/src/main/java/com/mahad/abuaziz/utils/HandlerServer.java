package com.mahad.abuaziz.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HandlerServer {
    private Context context;
    private String alamatServer;
    private static final String TAG = "HandlerServer";

    public HandlerServer(Context context, String alamatServer) {
        this.context = context;
        this.alamatServer = alamatServer;
    }

    public void sendDataToServer(final ResponServer responServer, final List<String> list) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, alamatServer,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            if (jsonObject.optString("error").equals("true")) {
                                responServer.gagal(jsonObject.getString("pesan"));
                            } else if (jsonObject.optString("error").equals("false")){
                                JSONArray jsonArray = jsonObject.getJSONArray("pesan");
                                responServer.berhasil(jsonArray);
                            }
                        } catch (JSONException e) {
                            responServer.gagal(String.valueOf(e));
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "onErrorResponse: " + error);
                        if (String.valueOf(error).equals("com.android.volley.TimeoutError")){
                            Toast.makeText(context, "Tidak dapat menghubungi Server, Hubungi Administrator", Toast.LENGTH_SHORT).show();
                        }
                        responServer.gagal(String.valueOf(error));
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("params", String.valueOf(list));
                Log.e(TAG, "surampak: " + list);
                return params;
            }
        };
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        requestQueue.add(stringRequest);
    }

    public void getStatusServerShoutcast(final ResponShoutcast responShoutcast) {
        Log.e(TAG, "getStatusServerShoutcast: "+ alamatServer);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, alamatServer,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            responShoutcast.result(jsonObject);
                        } catch (JSONException e) {
                            Log.e(TAG, "onResponse: " + e);
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        responShoutcast.failed(String.valueOf(error));
                    }
                });
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        requestQueue.add(stringRequest);
    }
}
