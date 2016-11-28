package com.iss.android.wearable.datalayer;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WebAppActivity extends Activity {

    private WebView webView;
    private String WebsiteURL;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_app);
        WebsiteURL = getString(R.string.server_website_port);

        webView = (WebView) findViewById(R.id.webView1);
        webView.setWebViewClient(new WebViewClient());
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        String userprofileAsJsonString = null;
        try {
            userprofileAsJsonString = (new ObjectMapper()).writeValueAsString(UserData.getUserProfile());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        Log.d("User profile as JSON", userprofileAsJsonString);
        webView.loadUrl(WebsiteURL);

    }

}
