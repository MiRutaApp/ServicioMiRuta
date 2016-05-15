package com.miruta.serviciomiruta;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.miruta)
    RelativeLayout miruta;
    @BindView(R.id.sendingData)
    ImageView sendingData;
    @BindView(R.id.idUnidadText)
    TextView idUnidadText;
    @BindView(R.id.idUnidadEditText)
    EditText idUnidadEditText;

    boolean animating = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.setDebug(true);
        ButterKnife.bind(this);


        final Animation pulseAnimation = new ScaleAnimation(1,0.95f,1,0.95f,Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);
        pulseAnimation.setRepeatMode(Animation.REVERSE);
        pulseAnimation.setRepeatCount(Animation.INFINITE);
        pulseAnimation.setDuration(600);
        pulseAnimation.setInterpolator(new AccelerateInterpolator(0.8f));

        final Animation alphaPulse = new AlphaAnimation(1,0);
        alphaPulse.setRepeatMode(Animation.REVERSE);
        alphaPulse.setRepeatCount(Animation.INFINITE);
        alphaPulse.setDuration(600);
        alphaPulse.setInterpolator(new AccelerateInterpolator(0.5f));

        final Intent serv = new Intent(getApplicationContext(),BackgroundService.class);


        final ServiceConnection serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {

            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                sendingData.setVisibility(View.GONE);
                animating =false;
                miruta.clearAnimation();
                sendingData.clearAnimation();
            }
        };

        miruta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(animating){
                    sendingData.setVisibility(View.GONE);
                    animating =false;
                    stopService(serv);
                    miruta.clearAnimation();
                    sendingData.clearAnimation();
                }else{
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},15);
                    }

                    sendingData.setVisibility(View.VISIBLE);
                    animating =true;
                    stopService(serv);
                    bindService(serv,serviceConnection,BIND_AUTO_CREATE);
                    miruta.startAnimation(pulseAnimation);
                    sendingData.startAnimation(alphaPulse);
                }
            }
        });

        idUnidadText.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                idUnidadEditText.setVisibility(View.VISIBLE);

                idUnidadEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
                idUnidadEditText.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(idUnidadEditText, InputMethodManager.SHOW_IMPLICIT);

                idUnidadText.setVisibility(View.GONE);
                idUnidadEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            idUnidadText.setText(idUnidadEditText.getText().toString());


                            SharedPreferences sharedPref = getSharedPreferences("ServicioMiRuta", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putString("idUnidad", idUnidadEditText.getText().toString());
                            editor.commit();

                            String idUnidad = sharedPref.getString("idUnidad","");
                            Log.d("Nuevo Id",idUnidad);
                            idUnidadEditText.setVisibility(View.GONE);
                            idUnidadText.setVisibility(View.VISIBLE);
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(idUnidadEditText.getWindowToken(), 0);
                            return true;
                        }
                        return false;
                    }
                });


                return false;
            }
        });
        if(isMyServiceRunning(BackgroundService.class)){
            animating =true;
            miruta.startAnimation(pulseAnimation);
            sendingData.startAnimation(alphaPulse);
        }
    }
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
