package com.example.mis.sensor;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.example.mis.sensor.FFT;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.DataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements SensorEventListener
{
    // Reference: https://developer.android.com/guide/topics/sensors/sensors_overview#sensors-identify

    //example variables
    private double[] rndAccExamplevalues;
    private double[] freqCounts;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    int entryNumber = 0;
    List<Entry> xAxisEntries = new ArrayList<>();
    List<Entry> yAxisEntries = new ArrayList<>();
    List<Entry> zAxisEntries = new ArrayList<>();
    List<Entry> magnitudeEntries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // initiate and fill example array with random values
        rndAccExamplevalues = new double[64];
        randomFill(rndAccExamplevalues);
        new FFTAsynctask(64).execute(rndAccExamplevalues);
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event)
    {
        final float xAxis = event.values[0];
        final float yAxis = event.values[1];
        final float zAxis = event.values[2];

        // Reference: https://stackoverflow.com/questions/8565401/android-get-normalized-acceleration
        double magnitude = Math.sqrt((xAxis*xAxis) + (yAxis*yAxis) + (zAxis*zAxis));

        // Reference: https://github.com/PhilJay/MPAndroidChart
        LineChart chart = findViewById(R.id.accelerometerChart);
        xAxisEntries.add(new Entry(entryNumber, xAxis));
        yAxisEntries.add(new Entry(entryNumber, yAxis));
        zAxisEntries.add(new Entry(entryNumber, zAxis));
        magnitudeEntries.add(new Entry(entryNumber, ((float) magnitude)));

        LineDataSet dataSetX = new LineDataSet(xAxisEntries, "X-Axis"); // add entries to dataset
        LineDataSet dataSetY = new LineDataSet(yAxisEntries, "Y-Axis"); // add entries to dataset
        LineDataSet dataSetZ = new LineDataSet(zAxisEntries, "Z-Axis"); // add entries to dataset
        LineDataSet dataSetMagnitude = new LineDataSet(magnitudeEntries, "Magnitude"); // add entries to dataset

        // Reference: https://stackoverflow.com/questions/29578706/hide-point-views-from-mpchart-linechart
        dataSetX.setDrawCircles(false);
        dataSetY.setDrawCircles(false);
        dataSetZ.setDrawCircles(false);
        dataSetMagnitude.setDrawCircles(false);

        // styling
        dataSetX.setColor(Color.RED);
        dataSetX.setValueTextColor(Color.RED);
        dataSetY.setColor(Color.GREEN);
        dataSetY.setValueTextColor(Color.GREEN);
        dataSetZ.setColor(Color.BLUE);
        dataSetZ.setValueTextColor(Color.BLUE);
        dataSetMagnitude.setColor(Color.WHITE);
        dataSetMagnitude.setValueTextColor(Color.WHITE);

        // use the interface ILineDataSet
        List<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSetX);
        dataSets.add(dataSetY);
        dataSets.add(dataSetZ);
        dataSets.add(dataSetMagnitude);

        LineData lineData = new LineData(dataSets);

        chart.setData(lineData);
        chart.setBackgroundColor(Color.LTGRAY);

        // Reference: https://stackoverflow.com/questions/33280328/mpandroidchart-linechart-set-how-many-x-values-are-displayed-at-max
        //chart.setVisibleXRangeMaximum(10);

        chart.invalidate(); // refresh

        entryNumber++;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    /**
     * Implements the fft functionality as an async task
     * FFT(int n): constructor with fft length
     * fft(double[] x, double[] y)
     */
    private class FFTAsynctask extends AsyncTask<double[], Void, double[]> {

        private int wsize; //window size must be power of 2

        // constructor to set window size
        FFTAsynctask(int wsize) {
            this.wsize = wsize;
        }

        @Override
        protected double[] doInBackground(double[]... values) {


            double[] realPart = values[0].clone(); // actual acceleration values
            double[] imagPart = new double[wsize]; // init empty

            /**
             * Init the FFT class with given window size and run it with your input.
             * The fft() function overrides the realPart and imagPart arrays!
             */
            FFT fft = new FFT(wsize);
            fft.fft(realPart, imagPart);
            //init new double array for magnitude (e.g. frequency count)
            double[] magnitude = new double[wsize];


            //fill array with magnitude values of the distribution
            for (int i = 0; wsize > i ; i++) {
                magnitude[i] = Math.sqrt(Math.pow(realPart[i], 2) + Math.pow(imagPart[i], 2));
            }

            return magnitude;

        }

        @Override
        protected void onPostExecute(double[] values) {
            //hand over values to global variable after background task is finished
            freqCounts = values;
        }
    }

    /**
     * little helper function to fill example with random double values
     */
    public void randomFill(double[] array){
        Random rand = new Random();
        for(int i = 0; array.length > i; i++){
            array[i] = rand.nextDouble();
        }
    }

}
