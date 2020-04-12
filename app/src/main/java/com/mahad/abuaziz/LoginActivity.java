package com.mahad.abuaziz;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.internal.ImageRequest;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import com.mahad.abuaziz.models.ModelUser;
import com.mahad.abuaziz.utils.DBHandler;
import com.mahad.abuaziz.utils.HandlerServer;
import com.mahad.abuaziz.utils.ResponServer;
import com.mahad.abuaziz.utils.ServiceAddress;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = "LoginActivity";
    private FirebaseAuth mAuth;
    private Button button_google;
    private ProgressBar progressBar;
    private String ID_LOGIN, NAMA, EMAIL, SUMBER_LOGIN;
    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient mGoogleSignInClient;
    private DBHandler dbHandler;

    private LoginButton signInFacebook;
    private Button button_facebook;

    private FirebaseUser currentUser;

    private CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mAuth = FirebaseAuth.getInstance();
        progressBar = findViewById(R.id.login_progressBar);

        button_google = findViewById(R.id.button_google);
        button_google.setOnClickListener(this);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        button_facebook = findViewById(R.id.button_facebook);
        button_facebook.setOnClickListener(this);
        signInFacebook = findViewById(R.id.signInFacebook);
        signInFacebook.setReadPermissions("email");
//        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();


        dbHandler = new DBHandler(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
        currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            logOut();
        }

        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        Log.e(TAG, "onSuccess: facebook manager: " + loginResult);
                        handleFacebookToken(loginResult.getAccessToken());
                    }

                    @Override
                    public void onCancel() {
                        Log.e(TAG, "onSuccess: facebook manager: Cancel");
                        Toast.makeText(LoginActivity.this, "Login Facebook Batal", Toast.LENGTH_LONG).show();
                        button_google.setEnabled(true);
                        button_facebook.setEnabled(true);
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        Log.e(TAG, "onSuccess: facebook manager: " + exception);
                        Toast.makeText(LoginActivity.this, "Ada kesalahan, Hubungi Team IT Mahad Abu Aziz", Toast.LENGTH_LONG).show();
                        button_google.setEnabled(true);
                        button_facebook.setEnabled(true);
                    }
                });
    }

    @Override
    public void onClick(View v) {
        button_google.setEnabled(false);
        button_facebook.setEnabled(false);
            switch (v.getId()){
                case R.id.button_facebook:
                    signInFacebook.performClick();
                    loginFacebookClicked();
                    break;
                case R.id.button_google:
                    progressBar.setVisibility(View.VISIBLE);
                    loginGoogleCLicked();
                    break;
            }
    }

    private void loginFacebookClicked(){
        Log.e(TAG, "loginFacebookClicked: FACEBOOK");
    }

    private void loginGoogleCLicked() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Log.e(TAG, "onActivityResult: GOOGLE");
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                assert account != null;
                handleGoogleToken(account);
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed" + e.getStatusCode());
                progressBar.setVisibility(View.GONE);
                button_google.setEnabled(true);
                button_facebook.setEnabled(true);
                Toast.makeText(this, "Login Google batal", Toast.LENGTH_LONG).show();
            }
        } else {
            progressBar.setVisibility(View.VISIBLE);
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void handleFacebookToken(AccessToken accessToken) {
        AuthCredential credential = FacebookAuthProvider.getCredential(accessToken.getToken());
        mAuth.signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()){
                    currentUser = mAuth.getCurrentUser();
                    SUMBER_LOGIN = "FACEBOOK";
                    assert currentUser != null;
                    responLogin(currentUser);
                } else {
                    Toast.makeText(LoginActivity.this, "Tidak dapat mendaftarkan ke Server, hubungi Team IT Mahad Abu Aziz", Toast.LENGTH_LONG).show();
                    button_google.setEnabled(true);
                    button_facebook.setEnabled(true);
                }
            }
        });
    }

    private void handleGoogleToken(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential:success");
                            currentUser = mAuth.getCurrentUser();
                            assert currentUser != null;
                            SUMBER_LOGIN = "GOOGLE";
                            responLogin(currentUser);
                        } else {
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Gagal masuk dengan akun Google atau periksa koneksi internet", Toast.LENGTH_LONG).show();
                            progressBar.setVisibility(View.GONE);
                            button_google.setEnabled(true);
                            button_facebook.setEnabled(true);
                        }
                    }
                });
    }

    private void responLogin(FirebaseUser currentUser) {
        ID_LOGIN = currentUser.getUid();
        NAMA = currentUser.getDisplayName();
        EMAIL = currentUser.getEmail();
        Log.e(TAG, "onComplete: GOOGLE LOGIN: " + ID_LOGIN + " " + NAMA + " " + EMAIL + " " + SUMBER_LOGIN);
        dbHandler.deleteDB();
        dbHandler.addUser(new ModelUser(1, SUMBER_LOGIN, ID_LOGIN, NAMA, EMAIL));
        checkLocalDB();
    }

    private void checkLocalDB(){
        ArrayList<HashMap<String, String>> userDB = dbHandler.getUser(1);
        String ID_LOGINDB = null;
        for (Map<String, String> map : userDB){
            ID_LOGINDB = map.get("id_login");
        }
        if (ID_LOGINDB != null){
            Uri photo;
//            int dimensionPixelSize = getResources().getDimensionPixelSize(com.facebook.R.dimen.com_facebook_profilepictureview_preset_size_large);
//            if (SUMBER_LOGIN.equals("FACEBOOK")) {
//                photo = ImageRequest.getProfilePictureUri(ID_LOGIN, dimensionPixelSize, dimensionPixelSize);
//                Log.e(TAG, "checkLocalDB: FACEBOOK USER");
//            } else {
//                currentUser = mAuth.getCurrentUser();
//                assert currentUser != null;
//                photo = currentUser.getPhotoUrl();
//            }

            photo = currentUser.getPhotoUrl();

            if (photo != null){
                saveDataToServer(photo);
            } else {
                Log.e(TAG, "checkLocalDB: PHOTO: " + null);
            }

        } else {
            Toast.makeText(this, "Gagal masuk hubungi Developer", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            button_google.setEnabled(true);
            button_facebook.setEnabled(true);
        }
    }

    private void saveDataToServer(Uri photo){
        Log.e(TAG, "saveDataToServer: "+ photo);
        List<String> list = new ArrayList<>();
        list.add(SUMBER_LOGIN);
        list.add(ID_LOGIN);
        list.add(NAMA);
        list.add(EMAIL);
        list.add(String.valueOf(photo));
        HandlerServer handlerServer = new HandlerServer(this, ServiceAddress.TAMBAHUSER);
        synchronized (this){
            handlerServer.sendDataToServer(new ResponServer() {
                @Override
                public void gagal(String result) {
                    Log.e(TAG, "gagal: " + result);
                    progressBar.setVisibility(View.GONE);
                    button_google.setEnabled(true);
                    button_facebook.setEnabled(true);
                }

                @Override
                public void berhasil(JSONArray jsonArray) {
                    Log.e(TAG, "berhasil: " + jsonArray);
                    progressBar.setVisibility(View.GONE);
                    button_google.setEnabled(true);
                    button_facebook.setEnabled(true);
                    finish();
                }
            }, list);
        }
    }

    private void logOut() {
        mAuth.signOut();
        mGoogleSignInClient.signOut();

        LoginManager.getInstance().logOut();

        button_google.setEnabled(true);
        button_facebook.setEnabled(true);
        Log.e(TAG, "logOut: sukses");
    }
}
