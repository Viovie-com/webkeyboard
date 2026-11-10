package com.viovie.webkeyboard;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import com.viovie.webkeyboard.activity.WidgetActivity;

public class WidgetProvider extends AppWidgetProvider {

    public WidgetProvider() {
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        RemoteViews view = new RemoteViews(context.getPackageName(),
                R.layout.widget);
        Intent intent = new Intent(context, WidgetActivity.class);
        int pendingIntentFlags = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingIntentFlags);

        view.setOnClickPendingIntent(R.id.widgeticon, pendingIntent);
        ComponentName componentName = new ComponentName(context.getPackageName(),
                WidgetProvider.class.getName());
        appWidgetManager.updateAppWidget(componentName, view);
    }

}
