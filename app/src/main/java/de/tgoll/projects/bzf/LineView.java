package de.tgoll.projects.bzf;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class LineView extends View {

    private Paint paint;

    public LineView(Context context) { super(context); init();}

    public LineView(Context context, AttributeSet attrs) {
        super(context, attrs); init();

    }
    private void init() {
        paint = new Paint();
        paint.setColor(Color.GRAY);
        paint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setStrokeWidth(getHeight());
        canvas.drawLine(0,getHeight()/2, getWidth(), getHeight()/2, paint);
    }
}
