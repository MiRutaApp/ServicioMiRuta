package com.miruta.serviciomiruta;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;

/**
 * Created by Gogodr on 14/05/2016.
 */
public class BackgroundService extends Service {
    private Handler handler;
    private Thread thread;
    private ParseGeoPoint geolocation;
    String idUnidad;
    private ParseObject unidad;

    public BackgroundService() {
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Toast.makeText(BackgroundService.this, "Enviando Información " + idUnidad, Toast.LENGTH_SHORT).show();

            }

        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences sharedPref = getSharedPreferences("ServicioMiRuta", Context.MODE_PRIVATE);
        idUnidad = sharedPref.getString("idUnidad","");

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Unidad");
        query.getInBackground(idUnidad, new GetCallback<ParseObject>() {
            public void done(ParseObject object, ParseException e) {
                if (e == null) {
                    unidad = object;

                    Log.d("Service", "comenzo");
                    LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    LocationListener locationListener = new LocationListener() {
                        public void onLocationChanged(Location location) {
                            Log.d("Service",location.toString());
                            geolocation = new ParseGeoPoint(location.getLatitude(),location.getLongitude());
                        }

                        public void onStatusChanged(String provider, int status, Bundle extras) {
                        }

                        public void onProviderEnabled(String provider) {
                        }

                        public void onProviderDisabled(String provider) {
                        }
                    };

                    //noinspection MissingPermission
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
                    Toast.makeText(getApplicationContext(),"Iniciando Servicio",Toast.LENGTH_SHORT).show();
                    thread = new Thread(new Runnable(){
                        public void run() {
                            try {
                                while(true)
                                {
                                    Thread.sleep(30000);
                                    handler.sendEmptyMessage(0);
                                    Log.d("Service","enviando");
                                }

                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                        }
                    });
                    thread.start();
                } else {
                    Toast.makeText(getApplicationContext(),"No se Encontro Unidad",Toast.LENGTH_SHORT).show();
                    stopSelf();
                }
            }
        });

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if(thread != null){
            thread.interrupt();
        }
        Toast.makeText(this,"Service Stopped",Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}