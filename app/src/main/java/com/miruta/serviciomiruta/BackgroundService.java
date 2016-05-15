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
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

/**
 * Created by Gogodr on 14/05/2016.
 */
public class BackgroundService extends Service {
    private Handler handler;
    private Thread thread;

    String idUnidad;

    private final IBinder mBinder = new LocalBinder();
    private ParseGeoPoint geolocation;
    private ParseObject unidad;
    private OnConnectedService onConnectedService;
    LocationManager locationManager;
    LocationListener locationListener;


    public class LocalBinder extends Binder {
        BackgroundService getService() {
            return BackgroundService.this;
        }
    }

    public BackgroundService() {
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if(unidad != null && geolocation!= null){
                    unidad.put("posicion",geolocation);
                    unidad.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if(e == null){
                                Toast.makeText(BackgroundService.this, "Posicion enviada de " + idUnidad, Toast.LENGTH_SHORT).show();
                            }else{
                                Log.d("Service",e.getMessage());
                            }
                        }
                    });
                }

            }

        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if(thread != null){
            thread.interrupt();
        }

        if(locationManager !=null){
            //noinspection MissingPermission
        locationManager.removeUpdates(locationListener);
        }
        onConnectedService.stop();

        Toast.makeText(this,"Cerrando Conexión",Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }


    @Override
    public IBinder onBind(Intent intent) {
        Toast.makeText(getApplicationContext(), "Iniciando Conexión", Toast.LENGTH_SHORT).show();
        SharedPreferences sharedPref = getSharedPreferences("ServicioMiRuta", Context.MODE_PRIVATE);
        idUnidad = sharedPref.getString("idUnidad","");
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Unidad");
        Log.d("ServiceParse","query a " + idUnidad);
        query.getInBackground(idUnidad, new GetCallback<ParseObject>() {
            public void done(ParseObject object, ParseException e) {
                if (e == null) {
                    if(object != null) {
                        unidad = object;
                        Log.d("Service", "comenzo");
                        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                        locationListener = new LocationListener() {
                            public void onLocationChanged(Location location) {
                                Log.d("Service", location.toString());
                                geolocation = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
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
                        Toast.makeText(getApplicationContext(), "Conexion Establecida", Toast.LENGTH_SHORT).show();
                        thread = new Thread(new Runnable() {
                            public void run() {
                                try {
                                    while (true) {
                                        Thread.sleep(30000);
                                        handler.sendEmptyMessage(0);
                                        Log.d("Service", "enviando");
                                    }

                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                            }
                        });
                        thread.start();
                        onConnectedService.run();
                    }else{
                        Log.d("Service","no se encontro");
                        Toast.makeText(getApplicationContext(),"No se Encontro la Unidad",Toast.LENGTH_SHORT).show();
                        stopSelf();
                    }
                } else {
                    Log.d("Service",e.getMessage());
                    Toast.makeText(getApplicationContext(),"No se Encontro la Unidad",Toast.LENGTH_SHORT).show();
                    stopSelf();
                }
            }
        });

        return mBinder;
    }

    public void setAnimation(OnConnectedService onConnectedService){
        this.onConnectedService = onConnectedService;

    }
    interface OnConnectedService{
        void run();
        void stop();
    };
}