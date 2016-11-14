package com.example.alok.musicalive;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.HeadphoneFence;
import com.google.android.gms.awareness.snapshot.DetectedActivityResult;
import com.google.android.gms.awareness.snapshot.HeadphoneStateResult;
import com.google.android.gms.awareness.snapshot.LocationResult;
import com.google.android.gms.awareness.snapshot.PlacesResult;
import com.google.android.gms.awareness.snapshot.WeatherResult;
import com.google.android.gms.awareness.state.HeadphoneState;
import com.google.android.gms.awareness.state.Weather;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceLikelihood;

import com.google.android.gms.location.places.PlaceTypes;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.PlaybackState;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        SpotifyPlayer.NotificationCallback, ConnectionStateCallback{
    private static final String TAG = "MainActivity";
    private MusicIntentReceiver myReceiver;

    // TODO: Replace with your client ID
    private static final String CLIENT_ID = "e432ba21b5314d9990de917dab38554a";
    // TODO: Replace with your redirect URI
    private static final String REDIRECT_URI = "http://localhost:8888/callback";

    private static final int REQUEST_CODE = 1337;
    private Player mPlayer;
    //protected Context mContext;
    boolean login = false;
    boolean pluggedOnce = false;
    PlaybackState pbs;
    boolean pbsb = false;

    String API_URL = "https://api.spotify.com/v1/search?q=\"";
    //A Default track in case we cannot retrieve playlist data from Spotify
    String finalValue = "spotify:track:2TpxZ7JUBn3uw46aR7qd6V";
    boolean flag = false;

    private static final String FENCE_RECEIVER_ACTION = "FENCE_RECEIVE";

    private HeadphoneFenceBroadcastReceiver fenceReceiver;
    private PendingIntent mFencePendingIntent;

    TextView headphone_status;
    TextView headphoneText;
    TextView activityText;
    TextView weatherText;
    TextView locationText;
    TextView placesText;
    Button skip;

    HashMap<Integer, String> weatherMap = new HashMap<Integer, String>();
    HashMap<String, String> activityMap = new HashMap<String, String>();
    int weatherStatus;
    String activityStatus;
    String temp = "moody";
    //String checker = "unknown";

    boolean mp = false;
    private GoogleApiClient mGoogleApiClient;
    private static final int MY_PERMISSION_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myReceiver = new MusicIntentReceiver();

        activityText = (TextView) findViewById(R.id.activity);
        weatherText = (TextView) findViewById(R.id.weather);
        locationText = (TextView) findViewById(R.id.location);
        placesText = (TextView) findViewById(R.id.places);
        //headphoneText = (TextView) findViewById(R.id.headphone);

        skip = (Button)findViewById(R.id.skip);
        skip.setVisibility(View.GONE);

        headphone_status = (TextView) findViewById(R.id.headphone_status);
        headphone_status.setText("Welcome to your personal context-aware music player.");

        weatherMap.put(1, "Sunny");
        weatherMap.put(2, "Cloudy");
        weatherMap.put(3, "Foggy");
        weatherMap.put(4, "Haze");
        weatherMap.put(5, "Icy");
        weatherMap.put(6, "Rain");
        weatherMap.put(7, "Snow");
        weatherMap.put(8, "Storm");
        weatherMap.put(9, "Windy");
        weatherMap.put(0, "Focus");

        activityMap.put("IN_VEHICLE", "driving");
        activityMap.put("ON_BICYCLE", "cycling");
        activityMap.put("ON_FOOT", "rock");
        activityMap.put("RUNNING", "running");
        activityMap.put("STILL", "moody");
        activityMap.put("TILTING", "party");
        activityMap.put("UNKNOWN", "unknown");
        activityMap.put("WALKING", "walking");

        //IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        //registerReceiver(myReceiver, filter);

        Context context = this;
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Awareness.API)
                .build();
        mGoogleApiClient.connect();

        if (ContextCompat.checkSelfPermission(
                MainActivity.this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSION_LOCATION
            );
            return;
        }

        initSnapshots();

        fenceReceiver = new HeadphoneFenceBroadcastReceiver();
        Intent intent = new Intent(FENCE_RECEIVER_ACTION);
        mFencePendingIntent = PendingIntent.getBroadcast(MainActivity.this,
                10001,
                intent,
                0);

        //mContext = this;

        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID,
                AuthenticationResponse.Type.TOKEN,
                REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "streaming", "playlist-read-private", "user-library-read", "user-top-read"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);

        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPlayer.skipToNext(null);
            }
        });
    }



    protected void initSnapshots(){

        //checker = temp;
        //ACTIVITY
        Awareness.SnapshotApi.getDetectedActivity(mGoogleApiClient)
                .setResultCallback(new ResultCallback<DetectedActivityResult>() {
                    @Override
                    public void onResult(@NonNull DetectedActivityResult detectedActivityResult) {
                        if (!detectedActivityResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Could not get the current activity.");
                            activityText.setText("Could not get the current activity.");
                            return;
                        }
                        ActivityRecognitionResult ar = detectedActivityResult.getActivityRecognitionResult();
                        DetectedActivity probableActivity = ar.getMostProbableActivity();

                        String kwAct = probableActivity.toString();
                        String[] kwAct2;
                        kwAct2 = kwAct.split("=");
                        kwAct2=kwAct2[1].split(",");

                        activityStatus = kwAct2[0];
                        Log.i(TAG, activityStatus);
                        activityText.setText(probableActivity.toString());
                    }
                });

        //HEADPHONES
        Awareness.SnapshotApi.getHeadphoneState(mGoogleApiClient)
                .setResultCallback(new ResultCallback<HeadphoneStateResult>() {
                    @Override
                    public void onResult(@NonNull HeadphoneStateResult headphoneStateResult) {
                        if (!headphoneStateResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Could not get headphone state.");
                            //headphoneText.setText("Could not get headphone state.");
                            return;
                        }
                        HeadphoneState headphoneState = headphoneStateResult.getHeadphoneState();
                        if (headphoneState.getState() == HeadphoneState.PLUGGED_IN) {
                            Log.i(TAG, "Headphones are plugged in.\n");
                            //headphoneText.setText("Headphones are plugged in.\n");
                        } else {
                            Log.i(TAG, "Headphones are NOT plugged in.\n");
                            //headphoneText.setText("Headphones are NOT plugged in.\n");
                        }
                    }
                });

        //PLACES
        Awareness.SnapshotApi.getPlaces(mGoogleApiClient)
                .setResultCallback(new ResultCallback<PlacesResult>() {
                    @Override
                    public void onResult(@NonNull PlacesResult placesResult) {
                        if (!placesResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Could not get places.");
                            placesText.setText("Could not get places.");
                            return;
                        }
                        List<PlaceLikelihood> placeLikelihoodList = placesResult.getPlaceLikelihoods();
                        // Show the top 1 possible location results.
                        if (placeLikelihoodList != null) {
                            for (int i = 0; i < 1 && i < placeLikelihoodList.size(); i++) {
                                PlaceLikelihood p = placeLikelihoodList.get(i);
                                //no mapping for placeType available
                                List<Integer> pType = p.getPlace().getPlaceTypes();
                                Log.i(TAG, p.getPlace().getName().toString() + ", likelihood: " + p.getLikelihood() + ", types: " +pType);
                                placesText.setText(p.getPlace().getName().toString() + " " + pType);
                            }
                        } else {
                            Log.e(TAG, "Place is null.");
                            placesText.setText("Place is null.");
                        }
                    }
                });

        //WEATHER
        Awareness.SnapshotApi.getWeather(mGoogleApiClient)
                .setResultCallback(new ResultCallback<WeatherResult>() {
                    @Override
                    public void onResult(@NonNull WeatherResult weatherResult) {
                        if (!weatherResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Could not get weather.");
                            weatherText.setText("Could not get weather.");
                            return;
                        }
                        Weather weather = weatherResult.getWeather();
                        //int[] condition = weather.getConditions();

                        String kwWtr = weather.toString();
                        String[] kwWtr2 = kwWtr.split("\\[");
                        kwWtr2 = kwWtr2[1].split("\\]");

                        Log.i(TAG, "Weather: " + kwWtr2[0]);

                        weatherStatus = Integer.parseInt(kwWtr2[0]);
                        weatherText.setText("Weather " + weatherMap.get(weatherStatus));
                        //Log.i(TAG, "Weather: " + condition);
                    }
                });

        //LOCATION
        Awareness.SnapshotApi.getLocation(mGoogleApiClient)
                .setResultCallback(new ResultCallback<LocationResult>() {
                    @Override
                    public void onResult(@NonNull LocationResult locationResult) {
                        if (!locationResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Could not get location.");
                            locationText.setText("Could not get location.");
                            return;
                        }
                        Location location = locationResult.getLocation();
                        Log.i(TAG, "Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude());
                        locationText.setText("Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude());
                    }
                });

        //Log.i(TAG, activityStatus + "now");
        if(activityStatus != null) {
            temp = activityMap.get(activityStatus);
        }

        if (temp != null && temp.equals("unknown")){
            temp = weatherMap.get(weatherStatus);
        }
        //Log.i("INFO", temp);
        AsyncTaskRunner runner = new AsyncTaskRunner();
        runner.execute(temp);
    }

    private void registerFences() {
        // Create a fence.
        AwarenessFence headphoneFence = HeadphoneFence.during(HeadphoneState.PLUGGED_IN);

        Awareness.FenceApi.updateFences(
                mGoogleApiClient,
                new FenceUpdateRequest.Builder()
                        .addFence("headphoneFenceKey", headphoneFence, mFencePendingIntent)
                        .build())
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Fence was successfully registered.");
                        } else {
                            Log.e(TAG, "Fence could not be registered: " + status);
                        }
                    }
                });
    }

    private void unregisterFence() {
        Awareness.FenceApi.updateFences(
                mGoogleApiClient,
                new FenceUpdateRequest.Builder()
                        .removeFence("headphoneFenceKey")
                        .build()).setResultCallback(new ResultCallbacks<Status>() {
            @Override
            public void onSuccess(@NonNull Status status) {
                Log.i(TAG, "Fence " + "headphoneFenceKey" + " successfully removed.");
            }

            @Override
            public void onFailure(@NonNull Status status) {
                Log.i(TAG, "Fence " + "headphoneFenceKey" + " could NOT be removed.");
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        initSnapshots();
        //IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        //registerReceiver(myReceiver, filter);
        registerFences();
        registerReceiver(fenceReceiver, new IntentFilter(FENCE_RECEIVER_ACTION));
    }

    @Override public void onResume() {
        //IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        //registerReceiver(myReceiver, filter);
        super.onResume();
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(myReceiver, filter);
        initSnapshots();
    }

    private class MusicIntentReceiver extends BroadcastReceiver {

        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);

                switch (state) {
                    case 0:
                        Log.d(TAG, "Headset is unplugged");
                        headphone_status.setText("Headphones unplugged");
                        //mp = true;
                        if(pluggedOnce) {
                            mPlayer.pause(null);
                        }
                        skip.setVisibility(View.GONE);
                        break;
                    case 1:
                        Log.d(TAG, "Headset is plugged");
                        headphone_status.setText("Headphones plugged in");
                        if(login) {
                            if (!pluggedOnce || flag) {
                                mPlayer.playUri(null, finalValue, 0, 0);
                                pluggedOnce = true;
                            } else {
                                mPlayer.resume(null);
                            }
                            skip.setVisibility(View.VISIBLE);
                        }
                        //pbsb = pbs.isPlaying;
                        //mPlayer.resume(null);
                        //openMusic(context);
                        break;
                    default:
                        Log.d(TAG, "I have no idea what the headset state is");
                }
            }
        }

        /*public void openMusic(Context context){

            AudioManager manager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);

            if(!manager.isMusicActive() && !mp) {
                Intent intent2 = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN,
                        Intent.CATEGORY_APP_MUSIC);
                //intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//Min SDK 15
                startActivity(intent2);
                mp = true;
            }
        }*/
    }

    private class AsyncTaskRunner extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            // Things to be done before execution of long running operation.
        }

        @Override
        protected String doInBackground(String... temp) {

            // Do some validation here
            //temp = "cloudy";

            //Log.i("INFO", temp[1]);

            try {
                URL url = new URL(API_URL + temp[0] + "\"&type=playlist&limit=1");
                Log.i("INFO", url.toString());
                //String url = API_URL + temp.toString() + "\"&type=playlist";

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    return stringBuilder.toString();
                }
                finally{
                    urlConnection.disconnect();
                }
            }
            catch(Exception e) {
                Log.e("ERROR", e.getMessage(), e);
                return null;
            }

        }

        @Override
        protected void onProgressUpdate(String... temp) {
            //finalResult.setText(text[0]);
            // Things to be done while execution of long running operation is in
            // progress. For example updating ProgessDialog
        }

        @Override
        protected void onPostExecute(String response) {
            // execution of result of Long time consuming operation
            //finalResult.setText(result);

            String change = finalValue;
            if(response == null || response.length() < 500) {
                response = "THERE WAS AN ERROR";
            }
            else {
                String[] value = response.split("spotify\\:user\\:");
                value = value[2].split("\"");
                Log.i("INFO", value[0]);

                finalValue = "spotify:user:" + value[0];
                if (!(change.equals(finalValue))){
                    flag = true;
                }
                Log.i("INFO", finalValue);
                //responseView.setText(response);
            }
        }
    }

        @Override public void onPause() {
        //unregisterReceiver(myReceiver);
        super.onPause();
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(myReceiver, filter);
        //unregisterReceiver(myReceiver);
    }


    /*protected void onRestart() {
        super.onRestart();
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(myReceiver, filter);
        registerFences();
        registerReceiver(fenceReceiver, new IntentFilter(FENCE_RECEIVER_ACTION));
    }*/

    @Override
    protected void onStop() {
        super.onStop();
        //unregisterReceiver(myReceiver);
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(myReceiver, filter);
        unregisterFence();
        unregisterReceiver(fenceReceiver);
    }

    class HeadphoneFenceBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            FenceState fenceState = FenceState.extract(intent);

            Log.d(TAG, "Fence Receiver Received");

            if (TextUtils.equals(fenceState.getFenceKey(), "headphoneFenceKey")) {
                switch (fenceState.getCurrentState()) {
                    case FenceState.TRUE:
                        Log.i(TAG, "Fence > Headphones are plugged in.");
                        break;
                    case FenceState.FALSE:
                        Log.i(TAG, "Fence > Headphones are NOT plugged in.");
                        break;
                    case FenceState.UNKNOWN:
                        Log.i(TAG, "Fence > The headphone fence is in an unknown state.");
                        break;
                }
            }

        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                Config playerConfig = new Config(this, response.getAccessToken(), CLIENT_ID);
                Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
                    @Override
                    public void onInitialized(SpotifyPlayer spotifyPlayer) {
                        mPlayer = spotifyPlayer;
                        mPlayer.addConnectionStateCallback(MainActivity.this);
                        mPlayer.addNotificationCallback(MainActivity.this);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
                    }
                });
            }
        }
    }

    @Override
    protected void onDestroy() {
        //Spotify.destroyPlayer(this);
        unregisterReceiver(myReceiver);
        super.onDestroy();
    }

    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
        Log.d("MainActivity", "Playback event received: " + playerEvent.name());
        switch (playerEvent) {
            // Handle event type as necessary
            default:
                break;
        }
    }

    @Override
    public void onPlaybackError(Error error) {
        Log.d("MainActivity", "Playback error received: " + error.name());
        switch (error) {
            // Handle error type as necessary
            default:
                break;
        }
    }

    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");
        login = true;
        //mPlayer.playUri(null, "spotify:track:2TpxZ7JUBn3uw46aR7qd6V", 0, 0);
    }

    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
    }

    @Override
    public void onLoginFailed(int i) {
        Log.d("MainActivity", "Login failed");
    }

    @Override
    public void onTemporaryError() {
        Log.d("MainActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }

}