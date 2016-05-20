package com.example.birouska.trackme;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaScannerConnection;
import android.media.SoundPool;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static android.media.AudioManager.*;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {


    /*
    *
    * implementando comunicação com http: get
    * */

    EditText etResponse;
    TextView tvIsConnected;

    public static String GET(String url){
        InputStream inputStream = null;
        String result = "";
        try {

            // create HttpClient
            HttpClient httpclient = new DefaultHttpClient();

            // make GET request to the given URL
            HttpResponse httpResponse = httpclient.execute(new HttpGet(url));

            // receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();

            // convert inputstream to string
            if(inputStream != null)
                result = convertInputStreamToString(inputStream);
            else
                result = "Did not work!";

        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        return result;
    }

    private static String convertInputStreamToString(InputStream inputStream) throws IOException{
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;

    }

    public boolean isConnected(){
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else
            return false;
    }

    private class HttpAsyncTaskPOST extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            return POST(urls[0], urls[1]);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(getBaseContext(), "Received!", Toast.LENGTH_LONG).show();
            try {

                if(result == "")
                    writeToFile("Can not save the object json in server!");
                else
                    writeToFile(result);

            } catch (Exception e) {
                // TODO Auto-generated catch block

                e.printStackTrace();
            }
        }
    }

    private class HttpAsyncTaskGET extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            return GET(urls[0]);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(getBaseContext(), "Received!", Toast.LENGTH_LONG).show();
            try {
                JSONArray lstLocations = new JSONArray(result);

                String str = "";

                str += "Qtd localizacoes = "+ lstLocations.length();

                for (int i = 0; i < lstLocations.length(); i++) {

                    JSONObject row = lstLocations.getJSONObject(i);
                    str += "\n--------\n";
                    str += "Registro: " + (i+1);
                    str += "\n--------\n";
                    str += "ID USER: "+ row.getString("id_user");
                    str += "\n--------\n";
                    str += "cel_number: "+ row.getString("cel_number");
                    str += "\n--------\n";
                    str += "longitude: "+ row.getString("longitude");
                    str += "\n--------\n";
                    str += "latitude: "+ row.getString("latitude");
                    str += "\n--------\n";
                    str += "dt_captura: "+ lstLocations.getJSONObject(0).getString("dt_captura");
                    str += "\n--------\n";
                    str += "dt_received: "+ lstLocations.getJSONObject(0).getString("dt_received");

                }

                etResponse.setText("");
                etResponse.setText(str);
                //etResponse.setText(json.toString(1));

            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /*
    * fim
     */
    int DELAY = 1000;
    CheckBox ShotCheckBox;
    Button StartBtn, CancelBtn;
    TextView random;
    EditText EtTime;
    Timer timer;
    MyTask task;
    int sound_id;
    SoundPool soundPool;

    GPSTracker gps;

    private static final String TAG = MainActivity.class.getName();
    private static final String FILENAME = "GPSPosition.txt";

    class MyTask extends TimerTask {
        @Override
        public void run() {

            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    //Task code goes here
                    try {
                        traceLocation();

                        // call AsynTask to perform network operation on separate thread
                        //new HttpAsyncTask().execute("http://hmkcode.appspot.com/rest/controller/get.json");
                        //new HttpAsyncTaskGET().execute("http://10.0.2.2:8080/EncontreMeAPI/v1/locations/1");
                        new HttpAsyncTaskGET().execute("https://encontremeapi.herokuapp.com/v1/locations/2");

                        /* fim get http get*/

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public  class MyOnItemSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent,
                                   View view, int pos, long id) {
            Toast.makeText(parent.getContext(), "Item is " +
                    parent.getItemAtPosition(pos).toString(), Toast.LENGTH_LONG).show();

            DELAY = Integer.parseInt(parent.getItemAtPosition(pos).toString()) * 60 * 1000;
        }

        public void onNothingSelected(AdapterView parent) {
            // Do nothing.
        }
    }


    public static String POST(String url, String Json){
        InputStream inputStream = null;
        String result = "";
        try {

            // 1. create HttpClient
            HttpClient httpclient = new DefaultHttpClient();

            // 2. make POST request to the given URL
            HttpPost httpPost = new HttpPost(url);


            // ** Alternative way to convert Person object to JSON string usin Jackson Lib
            // ObjectMapper mapper = new ObjectMapper();
            // json = mapper.writeValueAsString(person);

            // 5. set json to StringEntity
            StringEntity se = new StringEntity(Json);

            // 6. set httpPost Entity
            httpPost.setEntity(se);

            // 7. Set some headers to inform server about the type of the content
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");

            // 8. Execute POST request to the given URL
            HttpResponse httpResponse = httpclient.execute(httpPost);

            // 9. receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();

            // 10. convert inputstream to string
            if(inputStream != null)
                result = convertInputStreamToString(inputStream);
            else
                result = "Did not work!";

        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        // 11. return result
        return result;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StartBtn = (Button) findViewById(R.id.start);
        CancelBtn = (Button) findViewById(R.id.cancel);

        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.minutes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new MyOnItemSelectedListener());

        StartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {

                if (timer != null) {
                    timer.cancel();
                }
                timer = new Timer();
                task = new MyTask();

                //delay 1000ms
                timer.schedule(task, 1000, DELAY);
                Toast.makeText(MainActivity.this, "task repeated with delay " + DELAY, Toast.LENGTH_LONG).show();

                /*
                * testando http get
                * */

                // get reference to the views
                etResponse = (EditText) findViewById(R.id.etResponse);
                tvIsConnected = (TextView) findViewById(R.id.tvIsConnected);

                // check if you are connected or not
                if(isConnected()){
                    tvIsConnected.setBackgroundColor(0xFF00CC00);
                    tvIsConnected.setText("You are conncted");
                }
                else{
                    tvIsConnected.setText("You are NOT conncted");
                }

            }
        });

        CancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (timer != null) {
                    timer.cancel();
                    timer = null;
                }
            }
        });

    }

    private void traceLocation() throws JSONException {
        gps = new GPSTracker(MainActivity.this);

        if(gps.canGetLocation())
        {
            double latitude = gps.getLatitude();
            double longitude = gps.getLongitude();

            Toast.makeText(getApplicationContext(), "Your Location is -\nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();

            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            Date date = new Date();
            System.out.println(dateFormat.format(date));

            String textToSaveString = "Latitude: " + latitude + " - Longitude: " + longitude + " - TimeStamp: " + dateFormat.format(date);

            writeToFile(textToSaveString);

            TelephonyManager tm = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
            String cel_number = tm.getLine1Number();


            // 3. build jsonObject
            JSONObject jsonObject = new JSONObject();
            jsonObject.accumulate("id_user", 1);
            jsonObject.accumulate("cel_number", cel_number);
            jsonObject.accumulate("longitude", gps.getLongitude());
            jsonObject.accumulate("latitude", gps.getLatitude());
            jsonObject.accumulate("dt_captura", dateFormat.format(date));

            //new HttpAsyncTaskPOST().execute("http://10.0.2.2:8080/EncontreMeAPI/v1/locations/add",jsonObject.toString());
            new HttpAsyncTaskPOST().execute("https://encontremeapi.herokuapp.com/v1/locations/add",jsonObject.toString());

        }
        else{
            gps.showSettingsAlert();
        }
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    private void writeToFile(String data) {
        try
        {
            // Creates a trace file in the primary external storage space of the
            // current application.
            // If the file does not exists, it is created.
            File traceFile = new File(((Context)this).getExternalFilesDir(null), FILENAME);
            if (!traceFile.exists())
                traceFile.createNewFile();
            // Adds a line to the trace file
            BufferedWriter writer = new BufferedWriter(new FileWriter(traceFile, true /*append*/));
            writer.newLine();
            writer.write(data);
            writer.close();
            // Refresh the data so it can seen when the device is plugged in a
            // computer. You may have to unplug and replug the device to see the
            // latest changes. This is not necessary if the user should not modify
            // the files.
            MediaScannerConnection.scanFile((Context)(this),
                    new String[] { traceFile.toString() },
                    null,
                    null);

        }
        catch (IOException e) {
            Log.e(TAG, "File write failed: " + e.toString());
        }

    }


}
