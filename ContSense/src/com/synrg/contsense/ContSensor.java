package com.synrg.contsense;

import android.os.Bundle;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class ContSensor extends Activity {
	NotificationCompat.Builder mBuilder;
	NotificationManager mNotifyManager;
	String TAG = "ContSensor";
	Button start, stop;
	int ID = 101;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cont_sensor);
		
		start=(Button)findViewById(R.id.btnStart);
        stop=(Button)findViewById(R.id.btnStop);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
            	startSensing();
            	}
            });
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
            	stopSensing();}
            }); 
	   
	}
	
	private void toggleService(){
        Intent intent=new Intent(getApplicationContext(), SensingService.class);
        // Try to stop the service if it is already running
        intent.addCategory(SensingService.TAG);
        if(!stopService(intent)){
            startService(intent);
        }
    }
	
	public void startSensing(){
		showNotification();
		toggleService();
		this.finish();
	}
	
	private void stopSensing(){
		Intent intent=new Intent(getApplicationContext(), SensingService.class);
		intent.addCategory(SensingService.TAG);
		stopService(intent);
		mNotifyManager =
		        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotifyManager.cancel(ID);
		
	}
	
	public void showNotification(){
		mBuilder = new NotificationCompat.Builder(this);
		mBuilder.setContentTitle("ContSensor")
		    .setContentText("Sensing in progress")
		    .setSmallIcon(R.drawable.notification_icon);
		Intent resultIntent = new Intent(this, ContSensor.class);
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		stackBuilder.addParentStack(ContSensor.class);
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent =
		        stackBuilder.getPendingIntent(
		            0,
		            PendingIntent.FLAG_UPDATE_CURRENT
		        );
		mBuilder.setContentIntent(resultPendingIntent);
		mNotifyManager =
		        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotifyManager.notify(ID, mBuilder.build());
	
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_cont_sensor, menu);
		return true;
	}

}
