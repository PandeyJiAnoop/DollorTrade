package Fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.munche.OrdersActivity;
import com.example.munche.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.myhexaville.smartimagepicker.ImagePicker;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import UI.LoginActivity;
import de.hdodenhof.circleimageview.CircleImageView;

public class MyProfileFragment extends Fragment implements View.OnClickListener {

    private View view;
    private TextView mLogOutText,mUserProfileName,mUserProfileNum;
    private FirebaseAuth mAuth;
    private String uid, userImageUrl;
    private FirebaseFirestore db;
    private ImageView mMyOrdersText;
    private CircleImageView mUserProfileImage;
    Uri mImageUri;
    private ProgressDialog mProgressDialog;
    private StorageReference mUserImageRef;
    private StorageReference filePath;
    private DocumentReference mUserRef;
    private ImagePicker imagePicker;

    public MyProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_my_profile, container, false);

        init();
        getUserInfo();
        mLogOutText.setOnClickListener(this);
        mMyOrdersText.setOnClickListener(this);
        mUserProfileImage.setOnClickListener(this);

        return view;
    }

    private void init() {
        mProgressDialog = new ProgressDialog(getContext());
        db = FirebaseFirestore.getInstance();
        mLogOutText = view.findViewById(R.id.logOutText);
        mUserProfileName = view.findViewById(R.id.userProfileName);
        mUserProfileNum = view.findViewById(R.id.userProfileNumber);
        mAuth = FirebaseAuth.getInstance();
        uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        mMyOrdersText = view.findViewById(R.id.myOrdersImage);
        mUserProfileImage = view.findViewById(R.id.userProfileImage);
        mUserImageRef = FirebaseStorage.getInstance().getReference();
        mUserRef = db.collection("UserList").document(uid);
    }

    private void getUserInfo() {
        mUserRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                DocumentSnapshot docRef = task.getResult();
                String imageRef = (String) Objects.requireNonNull(docRef).get("user_profile_image");
                String userName = (String) docRef.get("name");
                String userPhoneNum = (String) docRef.get("phonenumber");
                Glide.with(Objects.requireNonNull(getActivity()))
                        .load(imageRef)
                        .placeholder(R.drawable.user_placeholder)
                        .into(mUserProfileImage);
                mUserProfileName.setText(userName);
                mUserProfileNum.setText(userPhoneNum);
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){

            case R.id.userProfileImage:
                showAll();
                break;

            case R.id.logOutText:
                new AlertDialog.Builder(Objects.requireNonNull(getContext()))
                        .setMessage("Are you sure you want to log out ?")
                        .setPositiveButton("Log Out", (dialog, which) -> {

                            Map<String, Object> updateDeviceToken = new HashMap<>();
                            updateDeviceToken.put("devicetoken", FieldValue.delete());

                            db.collection("UserList").document(uid).update(updateDeviceToken).addOnSuccessListener(aVoid -> {
                                mAuth.signOut();
                                sendUserToLogin();
                            });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                break;

            case R.id.myOrdersImage:
                Intent intent = new Intent(getActivity(), OrdersActivity.class);
                startActivity(intent);
                break;

        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        imagePicker.handleActivityResult(resultCode, requestCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        imagePicker.handlePermission(requestCode, grantResults);
    }

    public void showAll() {
        refreshImagePicker();
        imagePicker.choosePicture(false);
    }

    private void refreshImagePicker() {
        imagePicker = new ImagePicker(getActivity(),
                this,
                imageUri -> {
                    mProgressDialog.setMessage("Uploading, Please wait...");
                    mProgressDialog.show();
                    mUserProfileImage.setImageURI(imageUri);
                    mImageUri = imageUri;
                    filePath = mUserImageRef.child("user_profile_image").child(uid + ".jpg");
                    filePath.putFile(mImageUri).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            mUserImageRef.child("user_profile_image").child(uid + ".jpg").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    final String downloadUrl = uri.toString();
                                    UploadTask uploadTask = filePath.putFile(mImageUri);
                                    uploadTask.addOnSuccessListener(taskSnapshot -> {
                                        if (task.isSuccessful()){
                                            Map updateHashmap = new HashMap<>();
                                            updateHashmap.put("user_profile_image", downloadUrl);
                                            mUserRef.update(updateHashmap).addOnSuccessListener(o -> {
                                                mProgressDialog.dismiss();
                                            });
                                        }
                                    });
                                }
                            });
                        }
                    });
                });
    }

    private void sendUserToLogin() {
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}