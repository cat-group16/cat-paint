package com.example.catpaint;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class NewAppWidget extends AppWidgetProvider {

    public static final String ACTION_CLEAR = "com.example.catpaint.ACTION_CLEAR";
    public static final String ACTION_SAVE = "com.example.catpaint.ACTION_SAVE";

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.new_app_widget);

        // Open App Intent
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_preview, pendingIntent);

        // Clear Intent (Broadcast)
        Intent clearIntent = new Intent(context, NewAppWidget.class);
        clearIntent.setAction(ACTION_CLEAR);
        PendingIntent clearPendingIntent = PendingIntent.getBroadcast(context, 1, clearIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_btn_clear, clearPendingIntent);

        // Save Intent (Broadcast)
        Intent saveIntent = new Intent(context, NewAppWidget.class);
        saveIntent.setAction(ACTION_SAVE);
        PendingIntent savePendingIntent = PendingIntent.getBroadcast(context, 2, saveIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_btn_save, savePendingIntent);

        // Update preview if cache exists
        File cacheFile = new File(context.getCacheDir(), "current_drawing.png");
        if (cacheFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(cacheFile.getAbsolutePath());
            views.setImageViewBitmap(R.id.widget_preview, bitmap);
        } else {
            views.setImageViewResource(R.id.widget_preview, R.drawable.ic_cat_decoration);
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (ACTION_CLEAR.equals(intent.getAction())) {
            File cacheFile = new File(context.getCacheDir(), "current_drawing.png");
            if (cacheFile.exists()) cacheFile.delete();
            
            // Trigger app update if running (optional, but good for sync)
            Intent refreshIntent = new Intent("com.example.catpaint.REFRESH");
            context.sendBroadcast(refreshIntent);

            updateAllWidgets(context);
            Toast.makeText(context, "Tela pulita dal widget 🐾", Toast.LENGTH_SHORT).show();
        } else if (ACTION_SAVE.equals(intent.getAction())) {
            saveFromCache(context);
        }
    }

    private void updateAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, NewAppWidget.class));
        for (int id : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, id);
        }
    }

    private void saveFromCache(Context context) {
        File cacheFile = new File(context.getCacheDir(), "current_drawing.png");
        if (!cacheFile.exists()) {
            Toast.makeText(context, "Nulla da salvare!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Bitmap bitmap = BitmapFactory.decodeFile(cacheFile.getAbsolutePath());
            String fileName = "CatPaint_Widget_" + System.currentTimeMillis() + ".png";
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CatPaint");

            Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                OutputStream os = context.getContentResolver().openOutputStream(uri);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                os.close();
                Toast.makeText(context, "Salvato dalla Home! 🖼️", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(context, "Errore salvataggio", Toast.LENGTH_SHORT).show();
        }
    }
}