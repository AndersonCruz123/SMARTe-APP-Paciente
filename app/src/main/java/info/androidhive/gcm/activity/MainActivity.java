package info.androidhive.gcm.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import info.androidhive.gcm.R;
import info.androidhive.gcm.adapter.ChatRoomsAdapter;
import info.androidhive.gcm.app.Config;
import info.androidhive.gcm.app.MyApplication;
import info.androidhive.gcm.gcm.GcmIntentService;
import info.androidhive.gcm.model.Alerta;
import info.androidhive.gcm.model.Message;

public class MainActivity extends AppCompatActivity {

    private String TAG = MainActivity.class.getSimpleName();
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private ArrayList<Alerta> alertaArrayList;
    private ChatRoomsAdapter mAdapter;

    private Context context;
    String TAGG = "GPS";
    String longitude = "0";
    String latitude = "0";
    String status = "0";
    LocationManager locationManager;
    LocationListener locationListener;
    TextView Textlongitude;
    TextView Textlatitude;
    TextView TextContador;
    Handler handler = new Handler();
    Runnable runnable;
    private long miliTimeInicial;
    boolean flag = false;
    String ip = "192.168.0.101";
    String ipServidorFamiliares = "192.168.0.101";
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (MyApplication.getInstance().getPrefManager().getUser() == null) {
            launchLoginActivity();
        }

        setContentView(R.layout.activity_main);

        //Textlongitude = (TextView) findViewById(R.id.longitude);
        //Textlatitude = (TextView) findViewById(R.id.latitude);
        //TextContador.setText("Tempo para pressionar alarme falso: 15.0");
        //getGpsData();
        context = this;
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();


        //Envia requisição a cada 3 segundos para o GALILEO

        runnable = new Runnable() {
            public void run() {
                Log.d("THREAD", "THREAAAAAAAAD");
                //Neste if é verificado a resposta da requisicao, caso seja diferente de 0, significa que houve uma ocorrência
                if (status != null && status.equals("0")) {
                    resquestHttp(); //Envia requisicao
                    miliTimeInicial = System.currentTimeMillis();
                    flag = false;
                } else {
                    verifyContador(); //Atualiza contador
                }
                //verifyStatus(); //Verifica a resposta da requisicao, caso seja diferente de "0" chama o botão de alarme falso
                handler.postDelayed(this, 4000);
            }
        };
        runnable.run();


        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Config.SENT_TOKEN_TO_SERVER)) {
                    // gcm registration id is stored in our server's MySQL
                    Log.e(TAG, "GCM registration id is sent to our server");

                } else if (intent.getAction().equals(Config.PUSH_NOTIFICATION)) {
                    // new push notification is received
                    handlePushNotification(intent);
                }
            }
        };

        alertaArrayList = new ArrayList<>();
        mAdapter = new ChatRoomsAdapter(this, alertaArrayList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);

        if (checkPlayServices()) {
            registerGCM();

        }
    }


    private void handlePushNotification(Intent intent) {
        int type = intent.getIntExtra("type", -1);

        if (type == Config.PUSH_TYPE_USER) {
            Message message = (Message) intent.getSerializableExtra("message");
            Toast.makeText(getApplicationContext(), "New push: " + message.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void launchLoginActivity() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // starting the service to register with GCM
    private void registerGCM() {
        Intent intent = new Intent(this, GcmIntentService.class);
        intent.putExtra("key", "register");
        startService(intent);
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported. Google Play Services not installed!");
                Toast.makeText(getApplicationContext(), "This device is not supported. Google Play Services not installed!", Toast.LENGTH_LONG).show();
                finish();
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.action_logout:
                MyApplication.getInstance().logout();
                break;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    public void verifyContador() {
        Log.d("statusssss", "statussss");

        long current_time = System.currentTimeMillis();
        double miliTimeAtual = (current_time - miliTimeInicial) / 1000.0;

        if (miliTimeAtual < 15.0 && flag == false) {
            DecimalFormat df = new DecimalFormat("#.00");
            String time = df.format(15.0 - miliTimeAtual);
            TextContador.setText("Tempo para pressionar alarme falso: " + time);
            Log.d("MILISEGUNDOS", "" + miliTimeAtual);
        } else if (miliTimeAtual > 15.0 && flag == false && longitude.equals("0") == true && longitude.equals("0") == true) {
            ImageButton btn = (ImageButton) findViewById(R.id.imageButton);
            btn.setEnabled(false);
            TextContador.setText("Terminou o tempo para pressionar alarme falso!");
            //sendDataGps();
            getGpsData();
        }

    }

    private void getGpsData() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Log.i(TAGG, "GPS START");
        locationListener = new LocationListener() {

            public void onLocationChanged(Location location) {
                Log.i(TAGG, "location change:" + location.toString());

                longitude = "" + location.getLongitude();
                latitude = "" + location.getLatitude();
//                Textlongitude.setText(longitude);
  //              Textlatitude.setText(latitude);
                sendDataGps();
                //Toast.makeText(getApplicationContext(), "Atualizado", Toast.LENGTH_SHORT).show();


                if (location.hasAccuracy()) {  // good enough?
                    // do something here with the new location data
                    //....
                    //

                    Log.i(TAGG, "GPS listener done");
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    locationManager.removeUpdates(this);    // don't forget this to save battery
                }
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.i(TAGG, provider + " status:" + status);
            }

            public void onProviderEnabled(String provider) {
                Log.i(TAGG, provider + " enabled");
            }

            public void onProviderDisabled(String provider) {
                Log.i(TAGG, provider + " disabled");
            }
        };
        Log.i(TAGG, "GPS listener started");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    public void onClickAlarmeFalso(View view){
        status = "0";
        flag = true;
        setContentView(R.layout.activity_main);
        Toast.makeText(getApplicationContext(), "Alarme Falso, nada será enviado", Toast.LENGTH_SHORT).show();

    }

   /* public void sendVolleyDataGps(View view){
        getGpsData();

        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "http://"+ ipServidorFamiliares+":5000/teste";

// Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        //String resposta  = response.substring(0,500);
                        Toast.makeText(getApplicationContext(), "Respo " + response, Toast.LENGTH_SHORT).show();
                        Log.d("RESPONSE", ""+ response);
                        longitude = "0";
                        latitude = "0";
                        TextContador.setText("15.0");
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Response","That didn't work!");
            }
        });
// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }*/

    public void resquestHttp(){
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "http://"+ip+":5000/galileo";
        StringRequest putRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response) {
                        // response
                        //String bkpstatus = status;

                        if (response.equals(status) == false) {

                            if (response.equals("1")) setContentView(R.layout.crise_detected);
                            else if (response.equals("2")) setContentView(R.layout.desmaio_detected);
                            else if (response.equals("3")) setContentView(R.layout.queda_detected);
                            else setContentView(R.layout.activity_main);
                            TextContador = (TextView) findViewById(R.id.contador);
                        }
                        status = response;

                        Toast.makeText(getApplicationContext(), "REPONSE: " + status, Toast.LENGTH_SHORT).show();
                        Log.d("Response", response);
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        Log.d("Error.Response", "Erro " + error.toString());
                    }
                }
        ) {

            @Override
            protected Map<String, String> getParams()
            {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("status", status);
                //     params.put("domain", "http://itsalif.info");

                return params;
            }

        };

        queue.add(putRequest);
    }

    public void sendDataGps(){

        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "http://"+ ipServidorFamiliares+":5000/teste";

// Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        //String resposta  = response.substring(0,500);
                        Toast.makeText(getApplicationContext(), "Coordenadas enviada com sucesso! ", Toast.LENGTH_SHORT).show();
                        Log.d("RESPONSE", ""+ response);
                        longitude = "0";
                        latitude = "0";
                        TextContador.setText("Tempo para pressionar alarme falso: 15");
                        status = "0";
                        setContentView(R.layout.activity_main);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Response","That didn't work!");
            }
        });
// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

}
