package com.iss.android.wearable.datalayer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Euler on 10/15/2015.
 */
public class SyncAlarm extends BroadcastReceiver
{

    boolean serverSync = false;

    @Override
    public void onReceive(Context context, Intent intent)
    {
        /*PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
        wl.acquire();
        wl.release();*/

        if (DataSyncService.itself != null) {

            if (!DataSyncService.itself.serverSync)
                DataSyncService.itself.RequestDataFromWatch();
            else
                DataSyncService.itself.ShareDataWithServer();

            DataSyncService.itself.serverSync = !DataSyncService.itself.serverSync;
        }

    }

    // Wakes up the system every now and then. Costs battery
    public void SetAlarm(Context context)
    {
        AlarmManager am =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, SyncAlarm.class);
        intent.setAction("com.example.android.wearable.datalayer.ALARM");
        PendingIntent broadcast = PendingIntent.getBroadcast(context, 0, intent, 0);
        //am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 1 * 10 , broadcast); // Millisec * Second * Minute
        // With setInexactRepeating(), you have to use one of the AlarmManager interval
        // constants--in this case, AlarmManager.INTERVAL_DAY.
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),
                AlarmManager.INTERVAL_HOUR, broadcast); // AlarmManager.INTERVAL_HOUR


    }

    public void CancelAlarm(Context context)
    {
        Intent intent = new Intent(context, SyncAlarm.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }
}