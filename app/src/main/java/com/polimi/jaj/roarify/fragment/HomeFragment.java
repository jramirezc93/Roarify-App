package com.polimi.jaj.roarify.fragment;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;


import com.facebook.Profile;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.polimi.jaj.roarify.R;
import com.polimi.jaj.roarify.activity.SettingsActivity;
import com.polimi.jaj.roarify.adapter.CustomAdapter;
import com.polimi.jaj.roarify.activity.MessageActivity;
import com.polimi.jaj.roarify.model.Message;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;


/**
 * Created by jorgeramirezcarrasco on 4/1/17.
 */

public class HomeFragment extends Fragment implements OnMapReadyCallback,GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener{


    /* Google Maps parameters */
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private String mLastUpdateTime;
    private boolean mRequestingLocationUpdates;
    private LocationRequest mLocationRequest;
    private GoogleMap map;
    private LatLng myLocation;


    /* Parameters needed for the dialog fragments */

    private View dialogViewMessage;
    private LayoutInflater inflaterMessage;
    private AlertDialog.Builder builderMessage;
    private AlertDialog alertMessage;
    private SwipeRefreshLayout swipeContainer;
    String textPost;

    /* Server Connection parameters */
    private Double lat;
    private Double lon;
    List<Message> dataMessages = new ArrayList<Message>();
    List<Message> dataMessagesDraw = new ArrayList<Message>();
    private static final String ORIGINAL
            = "ÁáÉéÍíÓóÚúÑñÜü";
    private static final String REPLACEMENT
            = "AaEeIiOoUuNnUu";
    DateFormat format = new SimpleDateFormat("d MMM yyyy HH:mm:ss", Locale.ENGLISH);




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment

        final View view = inflater.inflate(R.layout.fragment_home, container, false);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {



        /* Google Api Client Connection */
        buildGoogleApiClient();

        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }

        /* GMap Setup */
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

         /*Layout Setup */

        swipeContainer = (SwipeRefreshLayout) getActivity().findViewById(R.id.swipeContainer);

        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new GetNearMessages().execute();
            }
        });
        swipeContainer.setColorSchemeResources(R.color.colorPrimary);


        FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertMessage.show();
            }
        });


        /* Setup of the dialog fragment when clicking on the '+' button */
        builderMessage = new AlertDialog.Builder(getActivity());
        builderMessage.setPositiveButton("Roar!", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                EditText editText = (EditText) dialogViewMessage.findViewById(R.id.new_message);
                textPost = editText.getText().toString();
                new PostMessage().execute();
                ((EditText) dialogViewMessage.findViewById(R.id.new_message)).setText("");
            }
        });
        builderMessage.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        inflaterMessage = getActivity().getLayoutInflater();
        dialogViewMessage = inflaterMessage.inflate(R.layout.message_dialog, null);
        builderMessage.setView(dialogViewMessage);
        builderMessage.setTitle("New message");
        alertMessage = builderMessage.create();

    }




    /**
     * Google Maps methods
     */

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        if (ContextCompat.checkSelfPermission((getActivity()), android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);

        }
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick ( final Marker marker){
                Intent mIntent = new Intent(getActivity(), MessageActivity.class);
                mIntent.putExtra("idMessage", marker.getTag().toString());
                mIntent.putExtra("currentLat",mLastLocation.getLatitude());
                mIntent.putExtra("currentLon",mLastLocation.getLongitude());
                mIntent.putExtra("latitudeMessage",marker.getPosition().latitude);
                mIntent.putExtra("longitudeMessage",marker.getPosition().longitude);
                startActivity(mIntent);
                return true;
            }
        });
    }

    public void drawMarker() {
        map.clear();
        for (Message m : dataMessagesDraw) {
                map.addMarker(new MarkerOptions().position(new LatLng(m.getLatitude(), m.getLongitude())).title(m.getText()).snippet(m.getUserName()).icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))).setTag(m.getMessageId());
            }
    }


    /**
     * Server Connection methods
     */

    private class PostMessage extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {

            HttpPost post = new HttpPost("https://1-dot-roarify-server.appspot.com/postMessage");
            List<NameValuePair> pairs = new ArrayList<NameValuePair>();

            pairs.add(new BasicNameValuePair("userId", Profile.getCurrentProfile().getId()));

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
            boolean anonymPref = sharedPref.getBoolean(SettingsActivity.KEY_PREF_ANONYM,false);
            if (anonymPref) {
                pairs.add(new BasicNameValuePair("userName", "Anonymous"));
            }
            else {
                pairs.add(new BasicNameValuePair("userName", stripAccents(Profile.getCurrentProfile().getName())));
            }

            pairs.add(new BasicNameValuePair("time", mLastUpdateTime.toString()));
            pairs.add(new BasicNameValuePair("text", stripAccents(textPost)));
            pairs.add(new BasicNameValuePair("lat", String.valueOf(mLastLocation.getLatitude())));
            pairs.add(new BasicNameValuePair("long", String.valueOf(mLastLocation.getLongitude())));
            pairs.add(new BasicNameValuePair("isParent", "true"));
            pairs.add(new BasicNameValuePair("parentId", ""));

            try {
                post.setEntity(new UrlEncodedFormEntity(pairs));
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try {
                HttpClient client = new DefaultHttpClient();
                HttpResponse response = client.execute(post);
                if (response.getStatusLine().getStatusCode() == 200) {
                    return true;
                } else {
                    return false;
                }
            } catch (ClientProtocolException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return false;
            }

        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(Boolean result) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    new GetNearMessages().execute(); // run on UI thread to avoid inconsistencies between adapter and listview
                }
            });
        }

    }


    private class GetNearMessages extends AsyncTask<Void, Message, Boolean> {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        Integer distancePref = Integer.parseInt(sharedPref.getString(SettingsActivity.KEY_PREF_DISTANCE,"0"));
        Integer timePref = Integer.parseInt(sharedPref.getString(SettingsActivity.KEY_PREF_TIME,"0"));
        Calendar calendar = Calendar.getInstance();
        Date limitDate;


        @Override
        protected Boolean doInBackground(Void... params) {
            List<NameValuePair> pairs = new ArrayList<NameValuePair>();

            if(mLastLocation != null) {

                lat = mLastLocation.getLatitude();
                lon = mLastLocation.getLongitude();
            }


            pairs.add(new BasicNameValuePair("lat", "" + lat));
            pairs.add(new BasicNameValuePair("long", "" + lon));


            String paramsString = URLEncodedUtils.format(pairs, "UTF-8");
            HttpGet get = new HttpGet("http://1-dot-roarify-server.appspot.com/getNearMessages" + "?" + paramsString);

            try {
                HttpClient client = new DefaultHttpClient();
                HttpResponse response = client.execute(get);
                HttpEntity entity = response.getEntity();
                BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent(), "iso-8859-1"), 8);

                String jsonResponse = reader.readLine();

                Gson gson = new Gson();
                TypeToken<List<Message>> token = new TypeToken<List<Message>>() {
                };
                List<Message> messagesList = gson.fromJson(jsonResponse, token.getType());
                if (messagesList != null) {
                    publishProgress(null);
                    for (Message a : messagesList) {
                        publishProgress(a);
                    }

                }

            } catch (ClientProtocolException e) {
                e.printStackTrace();
                showToastedWarning();
            } catch (IOException e) {
                e.printStackTrace();
                showToastedWarning();
            }


            return null;
        }

        private void showToastedWarning() {
            try {
                (getActivity()).runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText((getActivity()), R.string.load_messages_fail, Toast.LENGTH_SHORT).show();
                    }
                });
                Thread.sleep(300);
            } catch (InterruptedException e2) {
                e2.printStackTrace();
            }
        }


        @Override
        protected void onPreExecute() {
            dataMessages.clear();
            dataMessagesDraw.clear();
            calendar.add(Calendar.HOUR, -timePref);
            limitDate = calendar.getTime();
            new GetLocationTask().execute();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            swipeContainer.setRefreshing(false);
            dataMessagesDraw=dataMessages;
            LoadMessages(dataMessagesDraw);
            drawMarker();
        }

        @Override
        protected void onProgressUpdate(Message... values) {
            // TODO Auto-generated method stub
            if (values == null) {

            } else {
                Message message = new Message(values[0].getMessageId(), values[0].getUserId(), values[0].getUserName(), values[0].getText(), values[0].getTime(), values[0].getLatitude(), values[0].getLongitude(),values[0].getIsParent(),values[0].getParentId(), null);
                Location locationMessage = new Location("Roarify");
                Integer distance = getDistanceToMessage(locationMessage, message);
                try {
                    Date messageDate = format.parse(message.getTime());
                    if ((distancePref == 0 || distance < distancePref) && (timePref == 0 || messageDate.after(limitDate))) {
                        message.setDistance(getDistanceToMessage(locationMessage, message).toString());
                        dataMessages.add(message);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }

    }
    public static String stripAccents(String str) {
        if (str == null) {
            return null;
        }
        char[] array = str.toCharArray();
        for (int index = 0; index < array.length; index++) {
            int pos = ORIGINAL.indexOf(array[index]);
            if (pos > -1) {
                array[index] = REPLACEMENT.charAt(pos);
            }
        }
        return new String(array);
    }
    public void LoadMessages(final List<Message> dataMessagesDraw) {

        Collections.sort(dataMessagesDraw, new Comparator<Message>() {
            @Override
            public int compare(Message o1, Message o2) {
                return Integer.valueOf(o1.getDistance()).compareTo(Integer.valueOf(o2.getDistance()));
            }
        });

        ListView comments = (ListView) getActivity().findViewById(R.id.comments);
        CustomAdapter customAdapter = new CustomAdapter(getActivity(), R.layout.row, dataMessagesDraw);
        comments.setAdapter(customAdapter);

        comments.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                Message message = (Message) parent.getItemAtPosition(position);

                Intent mIntent = new Intent(getActivity() ,MessageActivity.class);
                mIntent.putExtra("idMessage", message.getMessageId());
                mIntent.putExtra("currentLat",mLastLocation.getLatitude());
                mIntent.putExtra("currentLon",mLastLocation.getLongitude());
                mIntent.putExtra("latitudeMessage",message.getLatitude());
                mIntent.putExtra("longitudeMessage",message.getLongitude());
                startActivity(mIntent);

            }
        });
    }


    /**
     * Google Play Services Methods
     */

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    public void onStart() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }

        super.onStart();
    }

    public void onStop() {

            if (mGoogleApiClient != null) {
                mGoogleApiClient.disconnect();
            }
        super.onStop();
    }


    @Override
    public void onConnected(Bundle connectionHint) {

        if (ContextCompat.checkSelfPermission((getActivity()), android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
                        /* Obtain the last Location */
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            /* Obtain the last date */
            mLastUpdateTime = format.format(new Date());
             /* Allows Location Updates */
            mRequestingLocationUpdates = true;
            mLocationRequest = new LocationRequest();
            if (mRequestingLocationUpdates) {
                startLocationUpdates();
            }

        }
        if (mLastLocation != null) {
            /* Convert Location */
            myLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 13));
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(getActivity(), "Connection suspended", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(getActivity(), "Failed to connect...", Toast.LENGTH_SHORT).show();
    }

    /* Method that start the location updates */
    protected void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        }

    }

    /* Method that is called when Location is changed */
    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        mLastUpdateTime = format.format(new Date());
        myLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
    }

    @Override
    public void onPause() {
        super.onPause();
            if (mGoogleApiClient != null) {

                if (mGoogleApiClient.isConnected()) {
                    stopLocationUpdates();
                }
            }
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission((getActivity()), android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (mGoogleApiClient != null) {


                if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
                    startLocationUpdates();
                }
                new GetNearMessages().execute();

            }

        }

    }

    public Integer getDistanceToMessage(Location locationMessage, Message message){
        locationMessage.setLatitude(message.getLatitude());
        locationMessage.setLongitude(message.getLongitude());

        return Math.round(mLastLocation.distanceTo(locationMessage));
    }

    private class GetLocationTask extends AsyncTask<Void, Void, Void>{
        ProgressDialog asyncDialog = new ProgressDialog(getActivity());


        @Override
        protected void onPreExecute() {
            asyncDialog.setMessage("Obtaining your location");
            asyncDialog.show();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            if (ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                while(mLastLocation==null) {
                    mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                /* Obtain the last date */
                    mLastUpdateTime = format.format(new Date());
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            asyncDialog.dismiss();

            super.onPostExecute(result);
        }

    }

}

