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
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener
{
    // Reference: https://developer.android.com/guide/topics/sensors/sensors_overview#sensors-identify
    private double[] freqCounts;
    private double[] fftValues;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private int accelerometerCaptureRate = 1;
    private int accelerometerCaptureRateInMicroSeconds;
    private int winSize = 32;
    private TextView accelerometerRateTextView;
    private TextView windowSizeTextView;
    private LineGraphSeries<DataPoint> fftSeries = new LineGraphSeries<>();
    private GraphView fftGraph;
    int entryNumber = 0;
    int arrayNumber = 0;
    int initFFTBar = 5;
    double maxFreq;
    List<Entry> xAxisEntries = new ArrayList<>();
    List<Entry> yAxisEntries = new ArrayList<>();
    List<Entry> zAxisEntries = new ArrayList<>();
    List<Entry> magnitudeEntries = new ArrayList<>();
    SeekBar seekBarFFT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Reference: https://www.android-examples.com/increase-decrease-number-value-on-seekbar-android-within-define-range/
        accelerometerRateTextView = findViewById(R.id.AccelerometerRateTextView);
        SeekBar accelerometerSeekBar = findViewById(R.id.seekBarChart);
        accelerometerSeekBar.setMax(5);

        accelerometerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b)
            {
                if(i < 1) {i = 1; }
                accelerometerCaptureRate = i;
                accelerometerRateTextView.setText(String.format(Locale.getDefault(),"%d%s", accelerometerCaptureRate, " Seconds"));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                Log.i("SeekBar Event", "onStopTrackingTouch called");
                accelerometerCaptureRateInMicroSeconds = accelerometerCaptureRate * 100000;
                mSensorManager.unregisterListener(MainActivity.this);
                mSensorManager.registerListener(MainActivity.this, mAccelerometer, accelerometerCaptureRateInMicroSeconds);
            }
        });

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (mAccelerometer != null)
        {
            accelerometerCaptureRateInMicroSeconds = accelerometerCaptureRate * 100000;
            mSensorManager.registerListener(this, mAccelerometer, accelerometerCaptureRateInMicroSeconds);
        }

        windowSizeTextView = findViewById(R.id.windowSizeTextView);
        seekBarFFT = findViewById(R.id.seekBarFFT);
        seekBarFFT.setMax(10);
        seekBarFFT.setProgress(initFFTBar);
        windowSizeTextView.setText("Window Size: " + winSize);

        seekBarFFT.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                initFFTBar = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(initFFTBar < 1)
                {
                    initFFTBar = 1;
                }
                winSize = (int) Math.pow(2, initFFTBar);
                fftValues = new double[winSize];
                windowSizeTextView.setText("Window Size: " + winSize);

            }
        });

        // Reference: http://www.android-graphview.org/
        GraphView fftGraph = findViewById(R.id.fftGraph);
        fftGraph.setTitle("Magnitude Frequency");
        fftSeries.setColor(Color.YELLOW);
        fftSeries.setDrawDataPoints(true);
        fftSeries.setDataPointsRadius(1);
        fftSeries.setThickness(3);
        fftGraph.getViewport().setBackgroundColor(Color.LTGRAY);
        fftGraph.addSeries(fftSeries);

        fftValues = new double[winSize];// Assign array size
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event)
    {
        // Cleanup arrays so they don't eat up memory
        cleanupDataLists();

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

        //apply FFT to magnitude
        if (arrayNumber < winSize)
        {
            fftValues[arrayNumber] = magnitude; //Add values to the array used for FFT
        }
        else if (arrayNumber == winSize)
        {
            new FFTAsynctask(winSize).execute(fftValues); // Run FFTAsynctask when the array has enough data
        }
        else if (arrayNumber > winSize)
        {
            // Reset array to send new data according to the window size
            arrayNumber = 0;
            fftValues = new double[winSize];
        }

        arrayNumber++;
        entryNumber++;
    }

    private void cleanupDataLists()
    {
        if (xAxisEntries.size() > 100)
        {
            Log.i("cleaning x Array", "x size is:" + xAxisEntries.size());
            for(int i = 0; i < 10; i++)
            {
                xAxisEntries.remove(i);
            }
        }

        if (yAxisEntries.size() > 100)
        {
            Log.i("cleaning y Array", "y size is:" + yAxisEntries.size());
            for(int i = 0; i < 10; i++)
            {
                yAxisEntries.remove(i);
            }
        }

        if (zAxisEntries.size() > 100)
        {
            Log.i("cleaning z Array", "z size is:" + zAxisEntries.size());
            for(int i = 0; i < 10; i++)
            {
                zAxisEntries.remove(i);
            }
        }

        if (magnitudeEntries.size() > 100)
        {
            Log.i("cleaning mag Array", "mag size is:" + magnitudeEntries.size());
            for(int i = 0; i < 10; i++)
            {
                magnitudeEntries.remove(i);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        accelerometerCaptureRateInMicroSeconds = accelerometerCaptureRate * 100000;
        mSensorManager.registerListener(this, mAccelerometer, accelerometerCaptureRateInMicroSeconds);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    public void resetValues(View view) {
        maxFreq = 0.0;
        Toast.makeText(getApplicationContext(), "Max Value: " + maxFreq, Toast.LENGTH_SHORT).show();
    }

    public void showValues(View view) {
        Toast.makeText(getApplicationContext(), "Max Value: " + maxFreq, Toast.LENGTH_SHORT).show();
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

            DataPoint[] fftPlotValues = new DataPoint[winSize];
            for (int i = 0; i < winSize; i++) {
                fftPlotValues[i] = new DataPoint(i, freqCounts[i]);
            }
            if (maxFreq <= fftSeries.getHighestValueY()){
                maxFreq = fftSeries.getHighestValueY();
            }
            fftSeries.resetData(fftPlotValues);
        }
    }
}