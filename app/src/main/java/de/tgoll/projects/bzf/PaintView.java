package de.tgoll.projects.bzf;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

public class PaintView extends View {

    private int eraserSize = 50;
    private Paint paint, eraser;

    private Bitmap bmp;
    private Canvas cache;

    private Path line;


    private PointF touch;

    private SharedPreferences settings;
    private boolean erasing;
    private boolean eraseflag;

    public PaintView (Context context, AttributeSet attrs) {
        super (context, attrs);

        settings = PreferenceManager.getDefaultSharedPreferences(context);
        touch = new PointF(-1,-1);
        paint = new Paint();
        paint.setColor(Util.lookupColor(context, R.attr.colorOnBackground));
        paint.setAntiAlias(true);
        paint.setStrokeWidth(2);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStyle(Paint.Style.STROKE);

        eraser = new Paint();
        eraser.setColor(Color.YELLOW);
        eraser.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        eraser.setStrokeCap(Paint.Cap.ROUND);
        eraser.setStyle(Paint.Style.STROKE);
        eraser.setStrokeWidth(convertDpToPixel(eraserSize, context));

        line = new Path();

    }

    public static float convertDpToPixel(float dp, Context context){
        Resources r = context.getResources();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        cache = new Canvas(bmp);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent (MotionEvent event) {
        boolean usesPen = settings.getBoolean(getContext().getString(R.string.settings_pen), true);

        touch = new PointF(event.getX(),event.getY());
        if (usesPen) eraseflag = event.getPressure() >= 0.9;
        else {
            int c = event.getPointerCount();
            if (c > 1) eraseflag = true;
            if (c == 0) eraseflag = false;
            else {  // On multitouch take the average point poisiton
                touch = new PointF();
                for (int i = 0; i < c; i++) touch.offset(event.getX(i), event.getY(i));
                touch.x /= c;
                touch.y /= c;
            }
        }
        //... if the second finger has been lifted, keep erasing


        if (eraseflag != erasing) {
            line.reset();
            erasing = eraseflag;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:  line.moveTo(touch.x, touch.y);  break;
            case MotionEvent.ACTION_MOVE:  line.lineTo(touch.x, touch.y);  break;
            case MotionEvent.ACTION_UP:    if (erasing){
                touch = new PointF(-1,-1);
                eraseflag = false;
            }
            default: return false;  // Let other views consume this event
        }

        invalidate();
        return true;
    }


    @Override
    public void onDraw(Canvas canvas) {
        cache.drawPath(line, erasing ? eraser : paint);
        canvas.drawBitmap(bmp, 0,0, null);
        if (touch.x >= 0 && erasing) canvas.drawCircle(touch.x, touch.y, eraserSize, paint);

        canvas.drawLine(0,0,getWidth(),0,paint);
    }

    public void clear() {
        line.reset();
        cache.drawColor(Color.TRANSPARENT, PorterDuff.Mode.MULTIPLY);
        invalidate();
    }
}
