package com.dakotawhipple.vectorpreview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.VectorDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by dakota on 12/6/2015.
 */
public class Gauge extends RelativeLayout {
    private static final int ANGLE_START = 138; //Start of Gauge
    private static final int ANGLE_END = 45; //End of Gauge
    private static final int ANGLE_SWEEP = 360 - ANGLE_START + ANGLE_END; // Used to calculate angle of each value
    private static final int GAUGE_RADIUS = 325; //Width/2 of original drawable

    //Constants defined in xml
    int MAX_VALUE;
    int MIN_VALUE;
    int DEFAULT_VALUE;

    Context mContext;
    AttributeSet mAttrs;

    Button incButton;
    Button decButton;
    TextView valueTextView;

    double scale = 0.0; //Original drawable to new size (=0.0 for 650px)
    Bitmap markerBitmap;
    LayerDrawable gaugeLayer;
    VectorDrawable gaugeDrawable; //Background
    double gaugeCenterX;
    double gaugeCenterY;

    Marker marker1; //Used for Actual Value
    int marker1Value; //Draggable
    BitmapDrawable marker1Drawable; //Actual
    boolean holdingMarker1 = false;

    Marker marker2; //Used for Set and draggable value
    double marker2Value; //Actual
    VectorDrawable marker2Drawable; //Draggable

    public Gauge(Context context, AttributeSet attrs) {
        super(context, attrs);
        mAttrs = attrs;
        mContext = context;
        setWillNotDraw(false);

        //Get styled attributes
        TypedArray a = mContext.obtainStyledAttributes(mAttrs, R.styleable.Gauge);
        MIN_VALUE = a.getInteger(R.styleable.Gauge_value_min, 0);
        MAX_VALUE = a.getInteger(R.styleable.Gauge_value_max, 100);
        DEFAULT_VALUE = a.getInteger(R.styleable.Gauge_value_default, 90);
        a.recycle();

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.gauge, this, true);
        initViews();
    }

    private void initViews() {
        incButton = (Button) findViewById(R.id.incValue);
        decButton = (Button) findViewById(R.id.decValue);
        valueTextView = (TextView) findViewById(R.id.gaugeValue);

        gaugeDrawable = (VectorDrawable) mContext.getDrawable(R.drawable.gauge);
        this.setBackground(gaugeDrawable);

        marker1Drawable = (BitmapDrawable) mContext.getDrawable(R.drawable.marker);
        marker2Drawable = (VectorDrawable) mContext.getDrawable(R.drawable.marker_actual);

        //Create and add Markers
        marker1 = createMarker(marker1Drawable);
        marker2 = createMarker(marker2Drawable);


        addView(marker1);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        marker1Value = marker1.getMarkerValue();
        marker2Value = marker2.getMarkerValue();
        valueTextView.setText("" + marker1Value);

//        canvas.drawCircle(cx, cy, touchRadius, new Paint());

//        Log.v("gauge", "Width:" + canvas.getWidth());
//        Log.v("gauge", "Height:" + canvas.getHeight());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);

        gaugeCenterX = getWidth()/2;
        gaugeCenterY = getHeight()/2;
        scale = gaugeCenterX / GAUGE_RADIUS;

        //If width or height is defined, take smallest value
        if(heightMode == MeasureSpec.EXACTLY || widthMode == MeasureSpec.EXACTLY) {
            if (width < height && width != 0)
                super.onMeasure(widthMeasureSpec, widthMeasureSpec);
            else
                super.onMeasure(heightMeasureSpec, heightMeasureSpec);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            Log.v("Gauge", "Problem measuring gauge.");
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        double xPosition = event.getX();
        double yPosition = event.getY();
        int eventAction = event.getActionMasked();
//        Log.v("Gauge/Touch", "X:" + xPosition + ", Y:" + yPosition + ",eventaction:" + eventAction);

        if(eventAction == MotionEvent.ACTION_DOWN) {
            if(marker1.touchingMarker(xPosition, yPosition)) {
                marker1.moveMarker(xPosition, yPosition);
                marker1Value = marker1.getMarkerValue();
                holdingMarker1 = true;
            }
        } else if(eventAction == MotionEvent.ACTION_MOVE) {
            if(holdingMarker1) {
                marker1.moveMarker(xPosition, yPosition);
                marker1Value = marker1.getMarkerValue();
            } else {
//                Log.v("GAUGE/move", "Not moving marker");
            }
        } else if(eventAction == MotionEvent.ACTION_UP) {
            holdingMarker1 = false;
        }
        return true;
    }

    Marker createMarker(Drawable d) {
        Marker m = new Marker(mContext);
        m.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        m.setMarkerDrawable(d);
        m.moveMarker(DEFAULT_VALUE);
        return m;
    }

    public boolean inRange(int value) {
        return value < MAX_VALUE && value > MIN_VALUE;
    }

    private class Marker extends View {
        private final static int MARKER_RADIUS = 287; //touch radius of drawable
        private static final int TOUCH_DIST = 120; //Max touch distance

        double VALUE_ANGLE; //Angle of 1 value

        double TOUCH_RADIUS;

        Drawable mDrawable;
        double mAngle;
        double mCenterX;
        double mCenterY;
        int mValue;

        public Marker(Context context) {
            super(context);
            setWillNotDraw(false);
            VALUE_ANGLE = (double)(MAX_VALUE - MIN_VALUE) / ANGLE_SWEEP; //Angle of 1 value
            Log.v("marker", "value" + (MAX_VALUE - MIN_VALUE) + "angle:" + ANGLE_SWEEP + "; ValueAngle:" + VALUE_ANGLE);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            TOUCH_RADIUS = MARKER_RADIUS * scale; //drawable center * scaleratio
            updateMarkerPosition();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if(mValue == -1) {
                moveMarker(DEFAULT_VALUE);
                Log.v("marker", "Marker at default value.");
            }
            float gCX = (float) gaugeCenterX;
            float gCY = (float) gaugeCenterY;
            super.onDraw(canvas);
            canvas.rotate(getCanvasRotation(mAngle), gCX, gCY);
//            canvas.rotate(90, getWidth()/2, getHeight()/2);
            mDrawable.setBounds(0, 0, getRight(), getBottom());
            mDrawable.draw(canvas);
            Log.v("marker", "Draw angle: " + getCanvasRotation(mAngle) + ";");

        }

        public void moveMarker(double x, double y) {
            if(inTouchZone(x, y)) {
                mAngle = calculateAngle(x, y);
                mValue = calculateValue(mAngle);
//                Log.v("marker", "move x:" + Math.round(x) + "; y:" + Math.round(y) + "; angle:" + mAngle + "; value:" + mValue);
            }
            updateMarkerPosition();
        }

//        public void moveMarker(double angle) {
//            if(angle > ANGLE_SWEEP) {
//                mValue = MIN_VALUE;
//                mAngle = ANGLE_START;
//            } else {
//                mValue = calculateValue(angle);
//                mAngle = angle;
//            }
//            updateMarkerPosition();
//        }

        public void moveMarker(int value) {
            mValue = value;
            mAngle = calculateAngle(value);
            updateMarkerPosition();
        }

        //Calculate the angle of marker away from ANGLE_START
        private double calculateAngle(double x, double y) {
            double a = convertAngle(Math.toDegrees( Math.atan2(y - gaugeCenterY, x - gaugeCenterX)));
            if(inRange(a))
                return a;
            else {
                if(getCanvasRotation(a) > (ANGLE_START-ANGLE_END)/2 + ANGLE_END)
                    return 0;
                else
                    return ANGLE_SWEEP;
            }
        }

        private double calculateAngle(int value) {
            return (value - MIN_VALUE) * VALUE_ANGLE;
        }

        private double convertAngle(double angle) { //From circle angle to gauge angle
            if(angle < 360 && angle >= ANGLE_START)
                return angle - ANGLE_START;
            else
                return 360 - ANGLE_START + angle;
        }

        private int calculateValue(double angle) {
//            Log.v("marker", "Calculated value:" +  (VALUE_ANGLE * angle) + "; value angle: " + VALUE_ANGLE + "; angle:" + angle);
            return (int) (VALUE_ANGLE * angle) + MIN_VALUE;
        }

        private float getCanvasRotation(double angle) {
            return (float)((angle + ANGLE_START)%360);
        }

        private boolean inRange(double a) {
            return ((getCanvasRotation(a) > ANGLE_START && getCanvasRotation(a) < 360) || getCanvasRotation(a) < ANGLE_END);
        }

        public boolean touchingMarker(double x, double y) {
            updateMarkerPosition();
            double dist = Math.sqrt(Math.pow(x-mCenterX, 2) + Math.pow(y-mCenterY, 2));
            if((dist < (TOUCH_DIST*scale))) {
                Log.v("marker", "touching marker");
            }
            return dist < (TOUCH_DIST*scale);
        }

        private boolean inTouchZone(double x, double y) {
            double dist = Math.sqrt(Math.pow(x-gaugeCenterX, 2) + Math.pow(y-gaugeCenterY, 2));
            if(Math.abs(TOUCH_RADIUS-dist) < (TOUCH_DIST*scale)) {
                return true;
            } return false;
        }

        public void updateMarkerPosition() {
            mCenterX = Math.cos(Math.toRadians(getCanvasRotation(mAngle))) * TOUCH_RADIUS + gaugeCenterX;
            mCenterY = Math.sin(Math.toRadians(getCanvasRotation(mAngle))) * TOUCH_RADIUS + gaugeCenterY;
            invalidate();
//            Log.v("marker", "mCenterX:" + mCenterX + ";mCenterY:" + mCenterY + ";radius:" + MARKER_RADIUS*scale);
//            Log.v("marker", "gaugeCenterX:" + gaugeCenterX + ";gaugeCenterY:" + gaugeCenterY + ";canvasrotation:" + getCanvasRotation(mAngle));
        }

        public double getMarkerCenterX() {
            return mCenterX;
        }

        public double getMarkerCenterY() {
            return mCenterY;
        }

        public void setMarkerDrawable(Drawable d) {
            mDrawable = d;
        }

        public Drawable getMarkerDrawable() {
            return mDrawable;
        }

        public void setMarkerValue(int v) {
            if(inRange(v))
                mValue = v;
            else
                mValue = MIN_VALUE;
        }

        public int getMarkerValue() {
            return mValue;
        }
    }
}