package im.where.whereim;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

public class LoginActivity extends BaseActivity {
    private CallbackManager mCallbackManager;

    private Handler mHandler = new Handler();
    private View mLogin;
    private View mRetry;
    private View mLoading;

    private Models.BinderTask mTask = new Models.BinderTask(){

        @Override
        public void onBinderReady(final CoreService.CoreBinder binder) {
            startLoading();

            binder.register_client(mProvider, mAuthId, mName, new CoreService.RegisterClientCallback() {
                @Override
                public void onCaptchaRequired() {
                    Intent intent = new Intent(LoginActivity.this, CaptchaActivity.class);
                    startActivityForResult(intent, 0);
                }

                @Override
                public void onExhausted() {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(LoginActivity.this, R.string.error_exhausted, Toast.LENGTH_LONG).show();
                            checkLogin();
                        }
                    }, 1500);
                }

                @Override
                public void onDone() {
                    checkLogin();
                }
            });
        }
    };

    private String mProvider;
    private String mAuthId;
    private String mName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mLoading = findViewById(R.id.loading);
        mLogin = findViewById(R.id.login);
        mRetry = findViewById(R.id.retry);

        mRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postBinderTask(mTask);
            }
        });

        FacebookSdk.sdkInitialize(getApplicationContext());
        mCallbackManager = CallbackManager.Factory.create();

        LoginButton fbLoginButton = (LoginButton) findViewById(R.id.fb_login_button);
        fbLoginButton.setReadPermissions("public_profile");
        fbLoginButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(final LoginResult loginResult) {
                mTrial = true;
                mLogin.setVisibility(View.GONE);
                mProvider = "facebook";
                startLoading();
                Profile profile = Profile.getCurrentProfile();
                if(profile==null){
                    new ProfileTracker() {
                        @Override
                        protected void onCurrentProfileChanged(
                                Profile oldProfile,
                                Profile currentProfile) {
                            String name = currentProfile.getName();
                            stopTracking();

                            mAuthId = loginResult.getAccessToken().getUserId();
                            mName = name;

                            SharedPreferences.Editor editor = getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
                            editor.putString(Models.KEY_PROVIDER, mProvider);
                            editor.putString(Models.KEY_ID, mAuthId);
                            editor.putString(Models.KEY_NAME, mName);
                            editor.apply();

                            postBinderTask(mTask);
                        }
                    }.startTracking();
                }else{
                    mAuthId = loginResult.getAccessToken().getUserId();
                    mName = profile.getName();

                    SharedPreferences.Editor editor = getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
                    editor.putString(Models.KEY_PROVIDER, mProvider);
                    editor.putString(Models.KEY_ID, mAuthId);
                    editor.putString(Models.KEY_NAME, mName);
                    editor.apply();

                    postBinderTask(mTask);
                }
            }

            @Override
            public void onCancel() {
                // App code
            }

            @Override
            public void onError(FacebookException exception) {
                // App code
            }
        });

        postBinderTask(new Models.BinderTask() {
            @Override
            public void onBinderReady(CoreService.CoreBinder binder) {
                checkLogin();
            }
        });
    }

    boolean mTrial = false;
    private void checkLogin(){
        if(getBinder().getClientId()==null){
            stopLoading();
            SharedPreferences sp = getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
            mAuthId = sp.getString(Models.KEY_ID, null);
            mName = sp.getString(Models.KEY_NAME, null);
            mProvider = sp.getString(Models.KEY_PROVIDER, null);
            if(mAuthId==null){
                mLogin.setVisibility(View.VISIBLE);
            }else{
                mLogin.setVisibility(View.GONE);
                if(!mTrial){
                    mTrial = true;
                    postBinderTask(mTask);
                }else{
                    mRetry.setVisibility(View.VISIBLE);
                }
            }
        }else{
            Log.e("LoginActivity", "start ChannelListActivity");
            Intent intent = new Intent(LoginActivity.this, ChannelListActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void startLoading(){
        mLoading.setVisibility(View.VISIBLE);
        mLogin.setVisibility(View.GONE);
        mRetry.setVisibility(View.GONE);
    }

    private void stopLoading(){
        mLoading.setVisibility(View.GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==0){
            postBinderTask(mTask);
            return;
        }

        if(FacebookSdk.isFacebookRequestCode(requestCode)) {
            mCallbackManager.onActivityResult(requestCode, resultCode, data);
            return;
        }
    }
}
