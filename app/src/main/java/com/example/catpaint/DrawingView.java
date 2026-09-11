package com.example.catpaint;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public class DrawingView extends View {

    private Path drawPath;
    private Paint drawPaint, canvasPaint, textPaint;
    private int paintColor = Color.BLACK;
    private Canvas drawCanvas;
    private Bitmap canvasBitmap;
    private float brushSize = 20f;
    private OnDrawListener onDrawListener;

    public interface OnDrawListener {
        void onDrawFinished();
    }

    public void setOnDrawListener(OnDrawListener listener) {
        this.onDrawListener = listener;
    }

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupDrawing();
    }

    private void setupDrawing() {
        drawPath = new Path();
        drawPaint = new Paint();
        drawPaint.setColor(paintColor);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(brushSize);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
        canvasPaint = new Paint(Paint.DITHER_FLAG);
        
        textPaint = new Paint();
        textPaint.setColor(paintColor);
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(60f);
        textPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
        canvas.drawPath(drawPath, drawPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                drawPath.moveTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_MOVE:
                drawPath.lineTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_UP:
                drawCanvas.drawPath(drawPath, drawPaint);
                drawPath.reset();
                if (onDrawListener != null) onDrawListener.onDrawFinished();
                break;
            default:
                return false;
        }
        invalidate();
        return true;
    }

    public void setColor(int newColor) {
        invalidate();
        paintColor = newColor;
        drawPaint.setColor(paintColor);
        drawPaint.setXfermode(null);
    }

    public void setEraser(boolean isEraser) {
        if (isEraser) {
            drawPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        } else {
            drawPaint.setXfermode(null);
            drawPaint.setColor(paintColor);
        }
    }

    public void clearCanvas() {
        drawCanvas.drawColor(Color.WHITE, PorterDuff.Mode.CLEAR);
        invalidate();
    }

    public void drawImage(Bitmap bitmap) {
        if (bitmap == null) return;
        
        // Scale bitmap to fit canvas
        float scale = Math.min((float) getWidth() / bitmap.getWidth(), (float) getHeight() / bitmap.getHeight());
        int newWidth = (int) (bitmap.getWidth() * scale);
        int newHeight = (int) (bitmap.getHeight() * scale);
        
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        
        // Center the bitmap
        float left = (getWidth() - newWidth) / 2f;
        float top = (getHeight() - newHeight) / 2f;
        
        drawCanvas.drawBitmap(scaledBitmap, left, top, canvasPaint);
        invalidate();
    }

    public void drawText(String text) {
        if (text == null || text.isEmpty()) return;
        textPaint.setColor(paintColor);
        // Center text
        float x = (getWidth() - textPaint.measureText(text)) / 2f;
        float y = getHeight() / 2f;
        drawCanvas.drawText(text, x, y, textPaint);
        invalidate();
        if (onDrawListener != null) onDrawListener.onDrawFinished();
    }

    public Bitmap getBitmap() {
        return canvasBitmap;
    }
}