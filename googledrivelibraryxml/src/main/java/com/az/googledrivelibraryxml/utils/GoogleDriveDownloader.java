package com.az.googledrivelibraryxml.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class GoogleDriveDownloader {

    private static final String BASE_URL = "https://www.googleapis.com/drive/v3/files/";

    public void downloadFile(String fileId, String outputPath, String accessToken) throws IOException {
        // 1. Create OkHttpClient instance
        OkHttpClient client = new OkHttpClient.Builder().build();

        // 2. Build the request URL
        HttpUrl url = HttpUrl.parse(BASE_URL + fileId);

        // 3. Create request with authorization header
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        // 4. Execute the request
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to download file: " + response.code());
            }

            // 5. Read and write the file stream
            ResponseBody body = response.body();
            if (body != null) {
                InputStream inputStream = body.byteStream();
                FileOutputStream outputStream = new FileOutputStream(outputPath);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.close();
                inputStream.close();
            } else {
                throw new IOException("Response body is null");
            }
        }
    }
}
