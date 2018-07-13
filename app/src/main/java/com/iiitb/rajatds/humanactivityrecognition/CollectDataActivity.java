package com.iiitb.rajatds.humanactivityrecognition;

import android.app.Dialog;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

public class CollectDataActivity extends AppCompatActivity {

    ListView listView;
    Button start,stop;
    Chronometer timer;
    TextView result;
    JSONArray dataArray=null;

    //Sensors used
    private SensorManager mSensorManager;
    private Sensor mSensorAcc,mSensorGyr;
    private SensorEventListener sensorEventListenerAcc,sensorEventListenerGyr;
    private File myExternalFile;
    private String filepath="TrainingData";
    private String fileName=null;

    static final int ACCELEROMETER=1;
    static final int GYROSCOPE=0;

    // Create a constant to convert nanoseconds to seconds.
    final float NS2S = 1.0f / 1000000000.0f;
    final float[] deltaRotationVector = new float[4];
    float timestamp=0;

    // In this example, alpha is calculated as t / (t + dT),
    // where t is the low-pass filter's time-constant and
    // dT is the event delivery rate.
    final float alpha = 0.8f;
    float gravity[]=new float[3];
    float linear_acceleration[]=new float[3];


    private  int activity=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collect_data);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout toolBarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        toolBarLayout.setTitle(getTitle());

        listView= (ListView) findViewById(R.id.activity);
        start= (Button) findViewById(R.id.startMonitoring);
        stop= (Button) findViewById(R.id.stopMonitoring);
        timer= (Chronometer) findViewById(R.id.timer);

        start.setEnabled(false);
        stop.setEnabled(false);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Long tsLong = System.currentTimeMillis()/1000;
                String ts = tsLong.toString();

                dataArray=new JSONArray();

                mSensorManager.registerListener(sensorEventListenerGyr, mSensorGyr, SensorManager.SENSOR_DELAY_NORMAL);
                mSensorManager.registerListener(sensorEventListenerAcc, mSensorAcc,SensorManager.SENSOR_DELAY_NORMAL);
                timer.setBase(0);
                timer.start();
                switch (activity) {
                    case 0://Walking
                        fileName = ts + "_Walking.json";
                        break;
                    case 1://Jogging
                        fileName = ts + "_Jogging.json";
                        break;
                    case 2://Sitting
                        fileName = ts + "_Sitting.json";
                        break;
                    case 3://UpStairs
                        fileName = ts + "_UpStairs.json";
                        break;
                    case 4://DownStairs
                        fileName = ts + "_DownStairs.json";
                        break;
                    case 5://Standing
                        fileName = ts + "_Standing.json";
                        break;

                }
                stop.setEnabled(true);
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSensorManager.unregisterListener(sensorEventListenerAcc);
                mSensorManager.unregisterListener(sensorEventListenerGyr);
                timer.stop();
                logData();
                dataArray=null;
                System.gc();
                stop.setEnabled(false);
                start.setEnabled(false);

            }
        });
        //sensor initialization
        mSensorManager= (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorAcc=mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorGyr=mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        sensorEventListenerAcc=new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                // Isolate the force of gravity with the low-pass filter.
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

                // Remove the gravity contribution with the high-pass filter.
                linear_acceleration[0] = event.values[0] - gravity[0];
                linear_acceleration[1] = event.values[1] - gravity[1];
                linear_acceleration[2] = event.values[2] - gravity[2];

                JSONObject data=new JSONObject();
                try {

                    data.put("ACC_LA_X",linear_acceleration[0]);
                    data.put("ACC_LA_Y",linear_acceleration[1]);
                    data.put("ACC_LA_Z",linear_acceleration[2]);
                    data.put("ACC_GRA_X",gravity[0]);
                    data.put("ACC_GRA_Y",gravity[1]);
                    data.put("ACC_GRA_Z",gravity[2]);
                    data.put("SENSOR","ACCELEROMETER");
                    data.put("TimeStamp",event.timestamp);
                    dataArray.put(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                System.out.println("Accuracy : " +accuracy);
            }
        };


        sensorEventListenerGyr=new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                JSONObject data=new JSONObject();
                try {
                    data.put("GYR_X",event.values[0]);
                    data.put("GYR_Y",event.values[1]);
                    data.put("GYR_Z",event.values[2]);
                    data.put("SENSOR","GYROSCOPE");
                    data.put("TimeStamp",event.timestamp);
                    dataArray.put(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                System.out.println("accuracy gyro: " +accuracy)
            }
        };

        String activities[]={"Walking","Jogging","Sitting","UpStairs","DownStairs","Standing"};
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,android.R.id.text1,activities);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                start.setEnabled(true);
                fileName=null;
                activity=position;
            }
        });
    }

    public void logData(){
        //write data to file
        myExternalFile = new File(getExternalFilesDir(filepath),fileName);
        try {
            if (isExternalStorageAvailable() || !isExternalStorageReadOnly()) {
                FileOutputStream fos = new FileOutputStream(myExternalFile,true);
                fos.write(dataArray.toString().getBytes());
                fos.close();
            }
        }
        catch(Exception e){
            System.out.print(e.toString());
        }
    }

    private static boolean isExternalStorageReadOnly() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    private static boolean isExternalStorageAvailable() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_collect_data, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
