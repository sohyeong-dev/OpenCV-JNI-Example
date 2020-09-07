package com.example.opencvjniexample;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class OcrApi {

    Context context;

    private static final String TAG = "OcrApi";

    ArrayList<Bitmap> bitmapImages;
    ArrayList<String> imagePaths;

    private static final String SEPARATOR = "///";

    public OcrApi(Context context, ArrayList<Bitmap> bitmaps) {
        this.context = context;

        this.bitmapImages = bitmaps;

        imagePaths = new ArrayList<String>();
        for (int i = 0; i < bitmapImages.size(); i++) {
            imagePaths.add(SaveBitmapToFile(bitmapImages.get(i)));
        }
    }

    private String SaveBitmapToFile(Bitmap bitmap) {
        File cacheDir = context.getCacheDir();
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        FileOutputStream output = null;

        String filePath = null;

        try {
            // creates temporary file
            File f = File.createTempFile("ocr_", ".jpg", cacheDir);

            output = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);

            // prints absolute path
            System.out.println("File path: "+f.getAbsolutePath());
            filePath = f.getAbsolutePath();

            // deletes file when the virtual machine terminate
            f.deleteOnExit();
        } catch (IOException e) {
            // if any error occurs
            e.printStackTrace();
        } finally {
            try {
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return filePath;
    }

    public ArrayList<String> recognizing() {
        ArrayList<String> keywords = new ArrayList<String>();
/*
        String imagePath = "/storage/emulated/0/DCIM/Screenshots/Screenshot_20200804-203626.jpg";
        try {
            String keyword = new KakaoOcrTask().execute(imagePath).get();
            Log.d(TAG, "recognizing: " + keyword);
            keywords.add(keyword);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

 */
        for (String imagePath: imagePaths) {
            Log.d(TAG, "recognizing: " + imagePath);
            try {
                String keyword = new KakaoOcrTask().execute(imagePath).get();
                Log.d(TAG, "recognizing: " + keyword);
                keywords.add(keyword);
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return keywords;
    }

    private class KakaoOcrTask extends AsyncTask<String, Integer, String> {

        // REST API 키
        String secretKey = BuildConfig.KAKAO_OCR_REST_API_KEY;

        @Override
        protected String doInBackground(String... strings) {
            int count = strings.length;

            String imagePath = strings[0];
            File file = new File(imagePath);

            String API_URL = "https://dapi.kakao.com/v2/vision/text/ocr";

            String output = internetConnect(API_URL, file, null);

            JSONObject outputJsonObject = null;
            try {
                outputJsonObject = new JSONObject(output);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            // 추출한 문자 영역과 텍스트 정보
            JSONArray resultArray = null;
            try {
                resultArray = outputJsonObject.getJSONArray("result");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            // System.out.println(resultArray.toString());
            int top_y = 0;
            int bottom_y = 0;
            String keyword = "";
            String words = "";
            for (int jsonArrayIdx = 0; jsonArrayIdx < resultArray.length(); jsonArrayIdx++) {
                JSONObject infoJsonObject = null;
                try {
                    infoJsonObject = (JSONObject) resultArray.get(jsonArrayIdx);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                // 추출한 문자 영역에 대한 x와 y 좌표
                JSONArray boxes = null;
                try {
                    boxes = infoJsonObject.getJSONArray("boxes");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                // System.out.println(boxes.toString());
                JSONArray top = null;
                try {
                    top = (JSONArray) boxes.get(0);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                // System.out.println(top.get(1));
                try {
                    if ((int) top.get(1) > bottom_y) {
                        if (keyword != "") {
                            System.out.println(keyword);
//                            System.out.println(preProcessing(keyword));
                            words += keyword + SEPARATOR;
                        }
                        // System.out.println("get");
                        JSONArray bottom = (JSONArray) boxes.get(2);
                        top_y = (int) top.get(1);
                        bottom_y = (int) bottom.get(1);

                        // 문자 영역에 있는 문자를 텍스트로 변경한 결과 값
                        JSONArray keywordJsonArray = infoJsonObject.getJSONArray("recognition_words");
                        keyword = keywordJsonArray.get(0).toString();
                    } else if ((int) top.get(1) > top_y - 3 && (int) top.get(1) < top_y + 3) {    // 한 줄이라고 판단
                        JSONArray keywordJsonArray = infoJsonObject.getJSONArray("recognition_words");
                        keyword += " " + keywordJsonArray.get(0).toString();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (keyword != "") {
                System.out.println(keyword);
//                System.out.println(preProcessing(keyword));
                words += keyword + SEPARATOR;
            }
/*
            String detectURL = "https://kapi.kakao.com/v1/vision/text/detect";
            String recognizeURL = "https://kapi.kakao.com/v1/vision/text/recognize";

            String returnString = internetConnect(detectURL, file, null);

            String boxes = null;
            try {
                JSONObject responseJsonObject = new JSONObject(returnString);
                JSONObject resultJsonObject = responseJsonObject.getJSONObject("result");
                JSONArray boxesArray = resultJsonObject.getJSONArray("boxes");
                boxes = boxesArray.toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            returnString = internetConnect(recognizeURL, file, boxes);

            String words = "";
            try {
                JSONObject responseJsonObject = new JSONObject(returnString);
                JSONObject resultJsonObject = responseJsonObject.getJSONObject("result");
                JSONArray wordsArray = resultJsonObject.getJSONArray("recognition_words");
                for (int jsonArrayIdx = 0; jsonArrayIdx < wordsArray.length(); jsonArrayIdx++) {
                    words += wordsArray.get(jsonArrayIdx) + " ";
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
*/
            return words.substring(0, words.lastIndexOf(SEPARATOR));
        }

        private String internetConnect(String apiURL, File file, String postParams) {
            String boxes = null;

            URL url;
            HttpURLConnection con = null;

            try {
                // URL 인스턴스
                url = new URL(apiURL);
                // HTTP 클라이언트 만들기
                con = (HttpURLConnection)url.openConnection();
                con.setUseCaches(false);
                con.setDoInput(true);
                con.setDoOutput(true);
                con.setReadTimeout(30000);
                // POST로 호출
                con.setRequestMethod("POST");
                // file을 업로드하는 경우
                String boundary = "----" + UUID.randomUUID().toString().replaceAll("-", "");
                con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                // REST API 키를 사용
                con.setRequestProperty("Authorization", "KakaoAK " + secretKey);

                con.connect();
                // 데이터 출력 스트림 객체를 생성
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                // long start = System.currentTimeMillis();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    writeMultiPart(wr, postParams, file, boundary);
                }
                wr.close();

                // status code
                int responseCode = con.getResponseCode();
                BufferedReader br;
                if (responseCode == 200) {    // OK
                    // response body
                    br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                } else {
                    br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                }
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = br.readLine()) != null) {
                    response.append(inputLine);
                }
                br.close();

                System.out.println(response);

                boxes = response.toString();
            } catch (Exception e) {
                System.out.println(e);
            } finally {
                if (con != null) {
                    con.disconnect();
                }

                return boxes;
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        private void writeMultiPart(DataOutputStream out, String jsonMessage, File file, String boundary) {
            // data
            if (jsonMessage != null && jsonMessage != "") {
                StringBuilder sb = new StringBuilder();
                sb.append("--").append(boundary).append("\r\n");
                sb.append("Content-Disposition:form-data; name=\"boxes\"\r\n\r\n");
                sb.append(jsonMessage);
                sb.append("\r\n");

                try {
                    out.write(sb.toString().getBytes("UTF-8"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // files
            if (file != null && file.isFile()) {
                try {
                    out.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                StringBuilder fileString = new StringBuilder();
                fileString
                        .append("Content-Disposition:form-data; name=\"image\"; filename=");
                fileString.append("\"" + file.getName() + "\"\r\n");
                fileString.append("Content-Type: application/octet-stream\r\n\r\n");
                try {
                    out.write(fileString.toString().getBytes("UTF-8"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = fis.read(buffer)) != -1) {
                        out.write(buffer, 0, count);
                    }
                    out.write("\r\n".getBytes());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    out.write(("--" + boundary + "--\r\n").getBytes("UTF-8"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
