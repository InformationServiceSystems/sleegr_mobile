package com.iss.android.wearable.datalayer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Euler on 12/19/2015.
 */
public class UploadingManager {


    // a simple wrapper around UploadFileToServer
    public static void UploadUserFileToServer(byte[] content, String name, String uploadUrl, String UserID){
        String completeUrl = uploadUrl + UserID;
        UploadFileToServer(content, name, completeUrl);
    }

    // Returns String which is a server response
    public static void UploadFileToServer(byte[] content, String name, String uploadUrl) {

        final byte [] filecontents = content;
        final String attachmentFileName = name;
        final String serverurl = uploadUrl;

        /*StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);*/

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {

                    // Static stuff:

                    String attachmentName = "file";
                    String crlf = "\r\n";
                    String twoHyphens = "--";
                    String boundary = "*****";

                    //Setup the request:

                    HttpURLConnection httpUrlConnection = null;
                    URL url = new URL(serverurl);
                    httpUrlConnection = (HttpURLConnection) url.openConnection();
                    httpUrlConnection.setUseCaches(false);
                    httpUrlConnection.setDoOutput(true);

                    httpUrlConnection.setRequestMethod("POST");
                    httpUrlConnection.setRequestProperty("Connection", "Keep-Alive");
                    httpUrlConnection.setRequestProperty("Cache-Control", "no-cache");
                    httpUrlConnection.setRequestProperty(
                            "Content-Type", "multipart/form-data;boundary=" + boundary);

                    // Start content wrapper:

                    DataOutputStream request = new DataOutputStream(
                            httpUrlConnection.getOutputStream());

                    request.writeBytes(twoHyphens + boundary + crlf);
                    request.writeBytes("Content-Disposition: form-data; name=\"" +
                            attachmentName + "\";filename=\"" +
                            attachmentFileName + "\"" + crlf);
                    request.writeBytes(crlf);

                    // read all file bytes

                    request.write(filecontents);

                    request.writeBytes(crlf);
                    request.writeBytes(twoHyphens + boundary +
                            twoHyphens + crlf);

                    // flush the output buffer

                    request.flush();
                    request.close();

                    // Get server response:

                    InputStream responseStream = new
                            BufferedInputStream(httpUrlConnection.getInputStream());

                    BufferedReader responseStreamReader =
                            new BufferedReader(new InputStreamReader(responseStream));

                    String line = "";
                    StringBuilder stringBuilder = new StringBuilder();

                    while ((line = responseStreamReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    responseStreamReader.close();

                    String response = stringBuilder.toString();

                    // release resources:

                    responseStream.close();
                    httpUrlConnection.disconnect();

                } catch (Exception ex) {

                }


            }
        }).start();


    }


}
