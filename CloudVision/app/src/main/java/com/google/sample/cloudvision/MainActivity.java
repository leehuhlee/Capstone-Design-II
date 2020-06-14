/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.sample.cloudvision;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.sample.cloudvision.feedback.FeedbackActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

//// feddback - onclicklistener 추가 ////
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String CLOUD_VISION_API_KEY = "AIzaSyCqJbvsJa_QkM2ZJp3vn-bFGlJkoJkwppQ";
    public static final String FILE_NAME = "temp.jpg";
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";
    private static final int MAX_LABEL_RESULTS = 10;
    private static final int MAX_DIMENSION = 1200;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int GALLERY_PERMISSIONS_REQUEST = 0;
    private static final int GALLERY_IMAGE_REQUEST = 1;
    public static final int CAMERA_PERMISSIONS_REQUEST = 2;
    public static final int CAMERA_IMAGE_REQUEST = 3;

    private TextView mImageDetails, tv_option1, tv_option2, tv_option3, tv_nation, tv_airline;
    private ImageView mMainImage;
    private Button btn_album, btn_camera, btn_keyboard, btn_nation, btn_airline;

    //Firebase DB
    private ArrayList<Stuff> airlineStuffList, nationStuffList;
    private FirebaseDatabase database;
    private DatabaseReference databaseReferenceStuff, databaseReferenceLog;

    //국가지정
    private int nation = 0;
    /* 0 : South Korea
       1 : United State
    */

    //항공사 지정
    private int airline = 1;
    /*  0 : Asiana Airlines
        1 : Korean Air
    */

    //DB
    private int Airline_DB = 0;
    private int Nation_DB = 1;

    private TabLayout tab_layout;
    private int pos = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_option1 = findViewById(R.id.tv_option1);
        tv_option2 = findViewById(R.id.tv_option2);
        tv_option3 = findViewById(R.id.tv_option3);
        tv_airline = findViewById(R.id.tv_airline);
        tv_nation = findViewById(R.id.tv_nation);

        tv_airline.setText("Korean Air");
        tv_nation.setText("South Korea");

        nationStuffList = new ArrayList<>(); //Stuff를 담을 ArrayList
        airlineStuffList = new ArrayList<>(); //
        database = FirebaseDatabase.getInstance(); //Firebase DB 연동
        databaseReferenceStuff = database.getReference("Stuff"); //DB 테이블 연결
        databaseReferenceLog = database.getReference("Log");

        databaseReferenceStuff.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //Firebase DB의 데이터를 받아오는 곳
                airlineStuffList.clear();
                nationStuffList.clear();//초기화

                int which_db = 0; // 0: Airline, 1: Nation
                // Stuff의 children: Nation, Airline
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    // Airline DB 불러오기
                    if (which_db == Airline_DB) {
                        int i = 0;
                        for (DataSnapshot snapshot2 : snapshot.getChildren()) {
                            if (airline == i) {
                                for (DataSnapshot snapshot3 : snapshot2.getChildren()) {
                                    Stuff stuff = snapshot3.getValue(Stuff.class);
                                    airlineStuffList.add(stuff);
                                }
                                break;
                            }
                            i++;
                        }
                    }

                    // Nation DB 불러오기
                    if (which_db == Nation_DB) {
                        int i = 0;
                        for (DataSnapshot snapshot2 : snapshot.getChildren()) {
                            if (nation == i) {
                                for (DataSnapshot snapshot3 : snapshot2.getChildren()) {
                                    Stuff stuff = snapshot3.getValue(Stuff.class);
                                    nationStuffList.add(stuff);
                                }
                                break;
                            }
                            i++;
                        }
                    }
                    which_db++;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                //에러 발생 시
                Log.e("MainActivity", String.valueOf(databaseError.toException()));
            }
        });

        btn_album = findViewById(R.id.btn_album);
        btn_album.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGalleryChooser();
            }
        });

        btn_camera = findViewById(R.id.btn_camera);
        btn_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCamera();
            }
        });

        btn_keyboard = findViewById(R.id.btn_keyboard);
        btn_keyboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder ad = new AlertDialog.Builder(MainActivity.this);
                ad.setIcon(R.mipmap.ic_launcher);
                ad.setTitle("Search by Keyboard");
                ad.setMessage("What is your stuff?");

                final EditText et = new EditText(MainActivity.this);
                ad.setView(et);

                ad.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String result = et.getText().toString();
                        mImageDetails.setText(result);
                        SearchbyKeyboard(result);
                        dialog.dismiss();
                    }
                });

                ad.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                ad.show();

            }
        });

        btn_nation = findViewById(R.id.btn_nation);
        btn_nation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CharSequence nations[] = new CharSequence[]{"South Korea", "United States"};

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Select Nation");
                builder.setItems(nations, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                nation = 0;
                                tv_nation.setText("South Korea");
                                break;
                            case 1:
                                nation = 1;
                                tv_nation.setText("United States");
                                break;
                        }

                        databaseReferenceStuff.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                //Firebase DB의 데이터를 받아오는 곳
                                nationStuffList.clear();//초기화

                                int which_db = 0; // 0: Airline, 1: Nation
                                // Stuff의 children: Nation, Airline
                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                    // Nation DB 불러오기
                                    if (which_db == Nation_DB) {
                                        int i = 0;
                                        for (DataSnapshot snapshot2 : snapshot.getChildren()) {
                                            if (nation == i) {
                                                for (DataSnapshot snapshot3 : snapshot2.getChildren()) {
                                                    Stuff stuff = snapshot3.getValue(Stuff.class);
                                                    nationStuffList.add(stuff);
                                                }
                                                break;
                                            }
                                            i++;
                                        }
                                    }
                                    which_db++;
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                //에러 발생 시
                                Log.e("MainActivity", String.valueOf(databaseError.toException()));
                            }
                        });

                    }
                });
                builder.show();

            }
        });

        btn_airline = findViewById(R.id.btn_airline);
        btn_airline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CharSequence airlines[] = new CharSequence[]{"Asiana Airlines", "Korean Air"};

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Select Airline");
                builder.setItems(airlines, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                airline = 0;
                                tv_airline.setText("Asiana Airlines");
                                break;
                            case 1:
                                airline = 1;
                                tv_airline.setText("Korean Air");
                                break;
                        }
                        databaseReferenceStuff.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                //Firebase DB의 데이터를 받아오는 곳
                                airlineStuffList.clear();//초기화

                                int which_db = 0; // 0: Airline, 1: Nation
                                // Stuff의 children: Nation, Airline
                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                    // Airline DB 불러오기
                                    if (which_db == Airline_DB) {
                                        int i = 0;
                                        for (DataSnapshot snapshot2 : snapshot.getChildren()) {
                                            if (airline == i) {
                                                for (DataSnapshot snapshot3 : snapshot2.getChildren()) {
                                                    Stuff stuff = snapshot3.getValue(Stuff.class);
                                                    airlineStuffList.add(stuff);
                                                }
                                                break;
                                            }
                                            i++;
                                        }
                                    }
                                    which_db++;
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                //에러 발생 시
                                Log.e("MainActivity", String.valueOf(databaseError.toException()));
                            }
                        });

                    }
                });
                builder.show();
            }
        });

        tab_layout = findViewById(R.id.tab_layout);
        tab_layout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                pos = tab.getPosition();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                pos = tab.getPosition();
                Log.e("position", String.valueOf(pos));
            }
        });

        mImageDetails = findViewById(R.id.image_details);
        mMainImage = findViewById(R.id.main_image);

        //// feedback 버튼, 리스너 추가
        Button btn_feedback = (Button)findViewById(R.id.btn_feedback);
        btn_feedback.setOnClickListener(this);
        ////

    }


    public void startGalleryChooser() {
        if (PermissionUtils.requestPermission(this, GALLERY_PERMISSIONS_REQUEST, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select a photo"),
                    GALLERY_IMAGE_REQUEST);
        }
    }

    public void startCamera() {
        if (PermissionUtils.requestPermission(
                this,
                CAMERA_PERMISSIONS_REQUEST,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA)) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, CAMERA_IMAGE_REQUEST);
        }
    }

    public File getCameraFile() {
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return new File(dir, FILE_NAME);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            uploadImage(data.getData());
        } else if (requestCode == CAMERA_IMAGE_REQUEST && resultCode == RESULT_OK) {
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
            uploadImage(photoUri);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, CAMERA_PERMISSIONS_REQUEST, grantResults)) {
                    startCamera();
                }
                break;
            case GALLERY_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, GALLERY_PERMISSIONS_REQUEST, grantResults)) {
                    startGalleryChooser();
                }
                break;
        }
    }

    public void uploadImage(Uri uri) {
        if (uri != null) {
            try {
                // scale the image to save on bandwidth
                Bitmap bitmap =
                        scaleBitmapDown(
                                MediaStore.Images.Media.getBitmap(getContentResolver(), uri),
                                MAX_DIMENSION);

                callCloudVision(bitmap);
                mMainImage.setImageBitmap(bitmap);

            } catch (IOException e) {
                Log.d(TAG, "Image picking failed because " + e.getMessage());
                Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
            }
        } else {
            Log.d(TAG, "Image picker gave us a null image.");
            Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
        }
    }

    private Vision.Images.Annotate prepareAnnotationRequest(Bitmap bitmap) throws IOException {
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        VisionRequestInitializer requestInitializer =
                new VisionRequestInitializer(CLOUD_VISION_API_KEY) {
                    /**
                     * We override this so we can inject important identifying fields into the HTTP
                     * headers. This enables use of a restricted cloud platform API key.
                     */
                    @Override
                    protected void initializeVisionRequest(VisionRequest<?> visionRequest)
                            throws IOException {
                        super.initializeVisionRequest(visionRequest);

                        String packageName = getPackageName();
                        visionRequest.getRequestHeaders().set(ANDROID_PACKAGE_HEADER, packageName);

                        String sig = PackageManagerUtils.getSignature(getPackageManager(), packageName);

                        visionRequest.getRequestHeaders().set(ANDROID_CERT_HEADER, sig);
                    }
                };

        Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
        builder.setVisionRequestInitializer(requestInitializer);

        Vision vision = builder.build();

        BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                new BatchAnnotateImagesRequest();
        batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
            AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

            // Add the image
            Image base64EncodedImage = new Image();
            // Convert the bitmap to a JPEG
            // Just in case it's a format that Android understands but Cloud Vision
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
            byte[] imageBytes = byteArrayOutputStream.toByteArray();

            // Base64 encode the JPEG
            base64EncodedImage.encodeContent(imageBytes);
            annotateImageRequest.setImage(base64EncodedImage);

            switch (pos) {
                case 0:
                    // add the features we want
                    annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                        Feature labelDetection = new Feature();
                        labelDetection.setType("LABEL_DETECTION");
                        labelDetection.setMaxResults(MAX_LABEL_RESULTS);
                        add(labelDetection);
                    }});
                    break;
                case 1:
                    annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                        Feature textDetection = new Feature();
                        textDetection.setType("TEXT_DETECTION");
                        textDetection.setMaxResults(10);
                        add(textDetection);
                    }});
                    break;
            }

            // Add the list of one thing to the request
            add(annotateImageRequest);
        }});

        Vision.Images.Annotate annotateRequest =
                vision.images().annotate(batchAnnotateImagesRequest);
        // Due to a bug: requests to Vision API containing large images fail when GZipped.
        annotateRequest.setDisableGZipContent(true);
        Log.d(TAG, "created Cloud Vision request object, sending request");

        return annotateRequest;
    }

    private class LableDetectionTask extends AsyncTask<Object, Void, String> {
        private final WeakReference<MainActivity> mActivityWeakReference;
        private Vision.Images.Annotate mRequest;

        LableDetectionTask(MainActivity activity, Vision.Images.Annotate annotate) {
            mActivityWeakReference = new WeakReference<>(activity);
            mRequest = annotate;
        }

        @Override
        protected String doInBackground(Object... params) {
            try {
                Log.d(TAG, "created Cloud Vision request object, sending request");
                BatchAnnotateImagesResponse response = mRequest.execute();
                return convertResponseToString(response);

            } catch (GoogleJsonResponseException e) {
                Log.d(TAG, "failed to make API request because " + e.getContent());
            } catch (IOException e) {
                Log.d(TAG, "failed to make API request because of other IOException " +
                        e.getMessage());
            }
            return "Cloud Vision API request failed. Check logs for details.";
        }

        protected void onPostExecute(String result) {
            MainActivity activity = mActivityWeakReference.get();
            String str1 = "Carry On : O", str2 = "Checked : O", str3 = "Detail : ";
            String option1 = null, option2 = null, option3 = null;
            String nation_option1 = null, nation_option2 = null, nation_option3 = "";
            String airline_option1 = null, airline_option2 = null, airline_option3 = "";
            int i = 0, j = 0;

            if (activity != null && !activity.isFinishing()) {
                TextView imageDetail = activity.findViewById(R.id.image_details);
                imageDetail.setText(result);

                activity.tv_option1.setVisibility(View.VISIBLE);
                activity.tv_option2.setVisibility(View.VISIBLE);
                activity.tv_option3.setVisibility(View.VISIBLE);

                for (Stuff element : activity.nationStuffList) {
                    if (element.name.equals(result)) {
                        nation_option1 = activity.nationStuffList.get(i).getOption1();
                        nation_option2 = activity.nationStuffList.get(i).getOption2();
                        nation_option3 = activity.nationStuffList.get(i).getOption3();
                        break;
                    }
                    i++;
                }

                for (Stuff element : activity.airlineStuffList) {
                    if (element.name.equals(result)) {
                        airline_option1 = activity.airlineStuffList.get(j).getOption1();
                        airline_option2 = activity.airlineStuffList.get(j).getOption2();
                        airline_option3 = activity.airlineStuffList.get(j).getOption3();
                        break;
                    }
                    j++;
                }

                if ("X".equals(nation_option1) || "X".equals(airline_option1))
                    option1 = "X";
                else
                    option1 = "O";

                if ("X".equals(nation_option2) || "X".equals(airline_option2))
                    option2 = "X";
                else
                    option2 = "O";

                if (nation_option3.length() < airline_option3.length())
                    option3 = airline_option3;
                else
                    option3 = nation_option3;

                str1 = "Carry On : " + option1;
                str2 = "Checked : " + option2;
                str3 = "Detail : " + option3;

                activity.tv_option1.setText(str1);
                activity.tv_option2.setText(str2);
                activity.tv_option3.setText(str3);

            }
        }
    }

    private void callCloudVision(final Bitmap bitmap) {
        // Switch text to loading
        mImageDetails.setText(R.string.loading_message);
        tv_option1.setVisibility(View.GONE);
        tv_option2.setVisibility(View.GONE);
        tv_option3.setVisibility(View.GONE);

        // Do the real work in an async task, because we need to use the network anyway
        try {
            AsyncTask<Object, Void, String> labelDetectionTask = new LableDetectionTask(this, prepareAnnotationRequest(bitmap));
            labelDetectionTask.execute();
        } catch (IOException e) {
            Log.d(TAG, "failed to make API request because of other IOException " +
                    e.getMessage());
        }
    }

    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    private String convertResponseToString(BatchAnnotateImagesResponse response) {
        switch (pos) {
            case 0:
                StringBuilder message = new StringBuilder("");
                List<EntityAnnotation> labels = response.getResponses().get(0).getLabelAnnotations();
                if (labels != null) {
                    EntityAnnotation label = labels.get(0);
                    message.append(String.format(Locale.US, "%s", label.getDescription()));
                } else {
                    message.append("nothing");
                }
                return message.toString();
            case 1:
                String message_logo = "";
                String[] message_array;
                List<EntityAnnotation> labels_logo = response.getResponses().get(0).getTextAnnotations();
                if (labels_logo != null) {
                    message_logo  = labels_logo.get(0).getDescription();
                    message_array = message_logo.split("\n");
                    message_logo = message_array[0];
                } else {
                    message_logo  = "nothing";
                }
                return message_logo;
        }
        return null;
    }

    private void SearchbyKeyboard(String result) {
        String str1 = "Carry On : O", str2 = "Checked : O", str3 = "Detail : ";
        String option1 = null, option2 = null, option3 = null;
        String nation_option1 = null, nation_option2 = null, nation_option3 = "";
        String airline_option1 = null, airline_option2 = null, airline_option3 = "";
        String log_msg = null;
        int i = 0, j = 0;
        boolean is_new_keyword = true;

        tv_option1.setVisibility(View.VISIBLE);
        tv_option2.setVisibility(View.VISIBLE);
        tv_option3.setVisibility(View.VISIBLE);

        for (Stuff element : nationStuffList) {
            if (element.name.equalsIgnoreCase(result)) {
                nation_option1 = nationStuffList.get(i).getOption1();
                nation_option2 = nationStuffList.get(i).getOption2();
                nation_option3 = nationStuffList.get(i).getOption3();
                is_new_keyword = false;
                break;
            }
            i++;
        }

        for (Stuff element : airlineStuffList) {
            if (element.name.equalsIgnoreCase(result)) {
                airline_option1 = airlineStuffList.get(j).getOption1();
                airline_option2 = airlineStuffList.get(j).getOption2();
                airline_option3 = airlineStuffList.get(j).getOption3();
                is_new_keyword = false;
                break;
            }
            j++;
        }

        if ("X".equals(nation_option1) || "X".equals(airline_option1))
            option1 = "X";
        else
            option1 = "O";

        if ("X".equals(nation_option2) || "X".equals(airline_option2))
            option2 = "X";
        else
            option2 = "O";

        if (nation_option3.length() < airline_option3.length())
            option3 = airline_option3;
        else
            option3 = nation_option3;

        str1 = "Carry On : " + option1;
        str2 = "Checked : " + option2;
        str3 = "Detail : " + option3;

        if (is_new_keyword == true) {
            // log_msg : A1N0 toy (Airline: Asiana, Nation: South Korea, Stuff: toy)
            log_msg = "A" + airline + "N" + nation + " " + result;
            databaseReferenceLog.push().setValue(log_msg);
        }

        tv_option1.setText(str1);
        tv_option2.setText(str2);
        tv_option3.setText(str3);

        mMainImage.setImageBitmap(null);
    }

    //// feedback 버튼 클릭하면 feedback activity 실행

    @Override
    public void onClick(View v) {
        Intent i = null;
        switch (v.getId()) {
            case R.id.btn_feedback:
                i = new Intent(this, FeedbackActivity.class);
                startActivity(i);
                break;
            default:
                break;
        }
    }

    ////
}
