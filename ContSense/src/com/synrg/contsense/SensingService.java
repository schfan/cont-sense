package com.synrg.contsense;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

public class SensingService extends Service implements SensorEventListener{
	private long initialTime;
	public static final String TAG = "SensingService";
	private SensorManager sensorManager;
	private float[] mGravs = new float[3];
	private float[] mGeoMags = new float[3];
	private float R[] = new float[16];
	private float I[] = new float[16];
	static String file1,file2,file3;
	static FileOutputStream fout1, fout2, fout3;
	public static File FILEPATH;

	private static  int RECORDER_BPP = 16;
	private static  String AUDIO_RECORDER_FILE_EXT_WAV;
	private static  String AUDIO_RECORDER_TEMP_FILE;
	private static  int RECORDER_SAMPLERATE = 44100;
	private static  int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
	private static  int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

	private AudioRecord recorder = null;
	private int bufferSize = 0;
	private Thread recordingThread = null;
	private boolean isRecording = false;

	public volatile Thread pauseThread;
	public static boolean alive;

	@Override
	public IBinder onBind(Intent i) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		initialTime = System.currentTimeMillis();
		Log.e(TAG, "onCreate");

		FILEPATH = new File(Environment.getExternalStorageDirectory().getPath()+"/TeddySensing/");
		FILEPATH.mkdirs();
		Log.e(TAG, "Path is :" +FILEPATH);
		alive = true;
		prepareFiles();


	}
	public void prepareFiles(){
		//create a new file for gyro
		sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),  SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_FASTEST);

		try {
			Log.e("Log","Write");
			file2 = FILEPATH+"/"+getCurrentTimeStamp()+"-gyro.txt";
			fout2 = new FileOutputStream(file2,true);
		} catch (IOException e) {
			Log.e("IOError",e.toString());
		}
		//create a new file for accelorometer
		try {
			Log.e("Log","Write");
			file1 = FILEPATH+"/"+getCurrentTimeStamp()+"-accl.txt";
			fout1 = new FileOutputStream(file1,true);
		} catch (IOException e) {
			Log.e("IOError",e.toString());
		}
		//create a new file for comp
		try {
			file3 = FILEPATH+"/"+getCurrentTimeStamp()+"-comp.txt";
			fout3 = new FileOutputStream(file3,true);
		} catch (IOException e) {
			Log.e("IOError",e.toString());
		}

		//set up recorder
/*		bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING);
		AUDIO_RECORDER_FILE_EXT_WAV = FILEPATH+"/"+getCurrentTimeStamp()+".wav";
		AUDIO_RECORDER_TEMP_FILE = FILEPATH+"/"+getCurrentTimeStamp()+"-temp.raw"; 			
		startRecording();*/
	}
	@Override
	public void onSensorChanged(SensorEvent event) {
		float[] values_accel = new float[3];
		float[] values_gyro = new float[3];
		long actualTime = System.currentTimeMillis();
		//long pastTime = actualTime - initialTime;
		long pastTime = initialTime;
		String string;

		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			values_accel = event.values;
			for (int i=0;i<3;i++) mGravs[i] = event.values[i];
			float x = values_accel[0]; //Acceleration force along the x axis (including gravity).
			float y = values_accel[1]; //Acceleration force along the y axis (including gravity).
			float z = values_accel[2]; //Acceleration force along the z axis (including gravity).
			try {
				string = ""+pastTime+" "+x+" "+y+" "+z+"\n";
				fout1.write(string.getBytes());
				fout1.flush();
			} catch (IOException e) {
				Log.e("IOError",e.toString());
			}
		} 

		if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
			values_gyro = event.values;	
			float x = values_gyro[0]; //Rate of rotation around the x axis.
			float y = values_gyro[1]; //Rate of rotation around the y axis.
			float z = values_gyro[2]; //Rate of rotation around the z axis.
			try {
				string = ""+pastTime+" "+x+" "+y+" "+z+"\n";
				fout2.write(string.getBytes());
				fout2.flush();
			} catch (IOException e) {
				Log.e("IOError",e.toString());
			}

		} 

		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			for (int i=0;i<3;i++) mGeoMags[i] = event.values[i];
			boolean success = SensorManager.getRotationMatrix(R, null, mGravs, mGeoMags);
			if (success) {
				float orientation[] = new float[3];
				SensorManager.getOrientation(R, orientation);
				final float rad2deg = (float)(180.0f/Math.PI);
				float azimuth = orientation[0]*rad2deg; // azimuth: rotation around the Z axis. 
				float pitch = orientation[1]*rad2deg; // pitch, rotation around the X axis. 
				float roll = orientation[2]*rad2deg; // roll, rotation around the Y axis. 
				float incl = SensorManager.getInclination(I);
				//	Log.e("^_^","Compass readings: "+azimuth+" "+pitch+" "+roll);
				try {
					string = ""+pastTime+" "+azimuth+" "+pitch+" "+roll+"\n";
					fout3.write(string.getBytes());
					fout3.flush();
				} catch (IOException e) {	
					Log.e("IOError",e.toString());
				}
			}
		}

	}

	@Override
	public void onDestroy() {
		Log.e(TAG, "onDestroy");
		alive=false;
		finishFiles();
		pauseThread = null;

	}

	public void finishFiles(){
		sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
		sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));		
		sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));	

		try{
			//	fout2.flush();
			fout2.close();
			//	fout1.flush();
			fout1.close();
			//	fout3.flush();
			fout3.close();
		}catch(IOException e){
			Log.e("IOException","Error on closing files");
		}

	//	stopRecording();
	}

	@Override
	public void onStart(Intent intent, int startid) {
		Log.e(TAG, "onStart");
		if(pauseThread == null){
			pauseThread = new Thread(new Runnable() {
				public void run() {
					while(alive){
						try {
							Thread.sleep(60000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						finishFiles();
						prepareFiles();
					}
					finishFiles();
				}
			});
			pauseThread.start();
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	public static String getCurrentTimeStamp() {
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		Date now = new Date();
		String strDate = sdfDate.format(now);
		return strDate;
	}

	private void startRecording(){
		recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
				RECORDER_SAMPLERATE, RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING, bufferSize);

		recorder.startRecording();

		isRecording = true;

		recordingThread = new Thread(new Runnable() {

			@Override
			public void run() {
				writeAudioDataToFile();
			}
		},"AudioRecorder Thread");

		recordingThread.start();
	}

	private void writeAudioDataToFile(){
		byte data[] = new byte[bufferSize];
		String filename = AUDIO_RECORDER_TEMP_FILE;
		FileOutputStream os = null;

		try {
			os = new FileOutputStream(filename);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		int read = 0;

		if(null != os){
			while(isRecording){
				read = recorder.read(data, 0, bufferSize);

				if(AudioRecord.ERROR_INVALID_OPERATION != read){
					try {
						os.write(data);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			try {
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void stopRecording(){
		if(null != recorder){
			isRecording = false;

			recorder.stop();
			recorder.release();

			recorder = null;
			recordingThread = null;
		}

		copyWaveFile(AUDIO_RECORDER_TEMP_FILE,AUDIO_RECORDER_FILE_EXT_WAV);
		deleteTempFile();
	}

	private void deleteTempFile() {
		File file = new File(AUDIO_RECORDER_TEMP_FILE);

		file.delete();
	}

	private void copyWaveFile(String inFilename,String outFilename){
		FileInputStream in = null;
		FileOutputStream out = null;
		long totalAudioLen = 0;
		long totalDataLen = totalAudioLen + 36;
		long longSampleRate = RECORDER_SAMPLERATE;
		int channels = 2;
		long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;

		byte[] data = new byte[bufferSize];

		try {
			in = new FileInputStream(inFilename);
			out = new FileOutputStream(outFilename);
			totalAudioLen = in.getChannel().size();
			totalDataLen = totalAudioLen + 36;

			Log.e(TAG,"File size: " + totalDataLen);

			WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
					longSampleRate, channels, byteRate);

			while(in.read(data) != -1){
				out.write(data);
			}

			in.close();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void WriteWaveFileHeader(
			FileOutputStream out, long totalAudioLen,
			long totalDataLen, long longSampleRate, int channels,
			long byteRate) throws IOException {

		byte[] header = new byte[44];

		header[0] = 'R';  // RIFF/WAVE header
		header[1] = 'I';
		header[2] = 'F';
		header[3] = 'F';
		header[4] = (byte) (totalDataLen & 0xff);
		header[5] = (byte) ((totalDataLen >> 8) & 0xff);
		header[6] = (byte) ((totalDataLen >> 16) & 0xff);
		header[7] = (byte) ((totalDataLen >> 24) & 0xff);
		header[8] = 'W';
		header[9] = 'A';
		header[10] = 'V';
		header[11] = 'E';
		header[12] = 'f';  // 'fmt ' chunk
		header[13] = 'm';
		header[14] = 't';
		header[15] = ' ';
		header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
		header[17] = 0;
		header[18] = 0;
		header[19] = 0;
		header[20] = 1;  // format = 1
		header[21] = 0;
		header[22] = (byte) channels;
		header[23] = 0;
		header[24] = (byte) (longSampleRate & 0xff);
		header[25] = (byte) ((longSampleRate >> 8) & 0xff);
		header[26] = (byte) ((longSampleRate >> 16) & 0xff);
		header[27] = (byte) ((longSampleRate >> 24) & 0xff);
		header[28] = (byte) (byteRate & 0xff);
		header[29] = (byte) ((byteRate >> 8) & 0xff);
		header[30] = (byte) ((byteRate >> 16) & 0xff);
		header[31] = (byte) ((byteRate >> 24) & 0xff);
		header[32] = (byte) (2 * 16 / 8);  // block align
		header[33] = 0;
		header[34] = (byte) RECORDER_BPP;  // bits per sample
		header[35] = 0;
		header[36] = 'd';
		header[37] = 'a';
		header[38] = 't';
		header[39] = 'a';
		header[40] = (byte) (totalAudioLen & 0xff);
		header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
		header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
		header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

		out.write(header, 0, 44);
	}

}