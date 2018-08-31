package com.example.owais.photoblogapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class SetupActivity extends AppCompatActivity {

    private Toolbar setupToolbar;

    private CircleImageView setupImage;
    private Uri mainImageURI;

    private String user_id;
    private boolean isChanged = false;

    private EditText setupName;
    private Button setupSaveBtn;
    private ProgressBar setupProgress;

    private StorageReference storageReference;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        setupToolbar = (Toolbar) findViewById(R.id.setup_toolbar);
        setSupportActionBar(setupToolbar);
        getSupportActionBar().setTitle("Account Setting");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        firebaseAuth = FirebaseAuth.getInstance();

        user_id = firebaseAuth.getCurrentUser().getUid();

        storageReference = FirebaseStorage.getInstance().getReference();
        firebaseFirestore = FirebaseFirestore.getInstance();

        setupImage = (CircleImageView) findViewById(R.id.setup_image);
        setupName = (EditText) findViewById(R.id.setup_name);
        setupSaveBtn = (Button) findViewById(R.id.setup_btn);
        setupProgress = (ProgressBar) findViewById(R.id.setup_progress);

        setupProgress.setVisibility(View.VISIBLE);
        setupSaveBtn.setEnabled(false);

        firebaseFirestore.collection("Users").document(user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                //when user data is fetch from Users/<user_id>
                if(task.isSuccessful()) {

                    //if data exist for perticular user means already set up profile image and name
                    if(task.getResult().exists()) {

                        String name = task.getResult().getString("name");
                        String image = task.getResult().getString("image");

                        mainImageURI = Uri.parse(image);

                        setupName.setText(name);

                        RequestOptions placeHolderRequest = new RequestOptions();
                        placeHolderRequest.placeholder(R.mipmap.default_image);

                        Glide.with(SetupActivity.this).setDefaultRequestOptions(placeHolderRequest).load(image).into(setupImage);

                        Toast.makeText(SetupActivity.this, "Data Exists", Toast.LENGTH_LONG).show();
                    } else {

                        Toast.makeText(SetupActivity.this, "Data doesn't Exists", Toast.LENGTH_LONG).show();
                    }

                } else { //when it fails to fetch data from Users/<user_id>
                    String errorMsg = task.getException().getMessage();
                    Toast.makeText(SetupActivity.this, "FIRESTORE RETRIEVE Error : "+errorMsg, Toast.LENGTH_LONG).show();
                }

                setupProgress.setVisibility(View.INVISIBLE);
                setupSaveBtn.setEnabled(true);
            }
        });

        setupSaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final String user_name = setupName.getText().toString();

                if (!TextUtils.isEmpty(user_name) && mainImageURI != null) {

                    setupProgress.setVisibility(View.VISIBLE);

                    if(isChanged) {



                            user_id = firebaseAuth.getCurrentUser().getUid();

                            //Initiating reference where to upload the image with what name in this case <user_id>.jpg
                            final StorageReference image_path = storageReference.child("profile_images").child(user_id + ".jpg");

                            UploadTask uploadTask = image_path.putFile(mainImageURI);
                            Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                                @Override
                                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                                    if (!task.isSuccessful()) {
                                        throw task.getException();
                                    }

                                    // Continue with the task to get the download URL
                                    return image_path.getDownloadUrl();
                                }
                            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {
                                    if (task.isSuccessful()) {
                                        storeAtFirestore(task, user_name);
                                    } else {
                                        // Handle failures
                                        String errorMsg = task.getException().getMessage();
                                        Toast.makeText(SetupActivity.this, "IMAGE Error : " + errorMsg, Toast.LENGTH_LONG).show();

                                        setupProgress.setVisibility(View.INVISIBLE);
                                    }
                                }
                            });

                            //uploading image to firebase
                            //                    image_path.putFile(mainImageURI).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                            //                        @Override
                            //                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            //
                            //                            if(task.isSuccessful()) {
                            //                                Uri downlod_uri = task.getResult().getUploadSessionUri();
                            //                                Toast.makeText(SetupActivity.this, "The image is uploaded", Toast.LENGTH_LONG).show();
                            //
                            //                            } else {
                            //                                String errorMsg = task.getException().getMessage();
                            //                                Toast.makeText(SetupActivity.this, "Error : "+errorMsg, Toast.LENGTH_LONG).show();
                            //                            }
                            //
                            //                            setupProgress.setVisibility(View.INVISIBLE);
                            //
                            //
                            //                        }
                            //                    });
                        }  else {
                        storeAtFirestore(null, user_name);
                    }
                }

            }
        });

        setupImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if(ContextCompat.checkSelfPermission(SetupActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(SetupActivity.this, "Permission Denied", Toast.LENGTH_LONG).show();
                        ActivityCompat.requestPermissions(SetupActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                    } else {
                        // start picker to get image for cropping and then use the image in cropping activity
                        imagePicker();
                    }
                } else {
                    imagePicker();
                }
            }
        });
    }

    private void storeAtFirestore(@NonNull Task<Uri> task, String user_name) {

        Uri downloadUri;
        if( task != null) {
            downloadUri = task.getResult();
        } else {
            downloadUri = mainImageURI;
        }

        Map<String, String> userMap = new HashMap<>();
        userMap.put("name", user_name);
        userMap.put("image", downloadUri.toString());
        firebaseFirestore.collection("Users").document(user_id).set(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<   Void> task) {

                if(task.isSuccessful()) {
                    Toast.makeText(SetupActivity.this, "The user setting are updated : ", Toast.LENGTH_LONG).show();
                    Intent mainIntent = new Intent(SetupActivity.this, MainActivity.class);
                    startActivity(mainIntent);
                    finish();

                } else {
                    String errorMsg = task.getException().getMessage();
                    Toast.makeText(SetupActivity.this, "FIRESTORE Error : "+errorMsg, Toast.LENGTH_LONG).show();
                }
                setupProgress.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void imagePicker() {
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1, 1)
                .start(SetupActivity.this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                mainImageURI = result.getUri();
                setupImage.setImageURI(mainImageURI);

                isChanged = true;

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }
}
