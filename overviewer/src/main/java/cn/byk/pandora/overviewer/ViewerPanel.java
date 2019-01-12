package cn.byk.pandora.overviewer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Deque;

/**
 * Renders your view hierarchy as an interactive 3D visualization of layers.
 * <p>
 * Interactions supported:
 * <ul>
 * <li>Single touch: controls the rotation of the model.</li>
 * <li>Two finger pinch: Adjust zoom.</li>
 * </ul>
 *
 * @author Created by JakeWharton; Modify by Byk on 2019/1/8
 */
public class ViewerPanel extends FrameLayout {

    private static final String TAG = ViewerPanel.class.getSimpleName();

    private static final int TRACKING_UNKNOWN = 0;
    private static final int TRACKING_VERTICALLY = 1;
    private static final int TRACKING_HORIZONTALLY = -1;
    private static final int TRACKING_ALL = 2;
    private static final int ROTATION_MAX = 60;
    private static final int ROTATION_MIN = -ROTATION_MAX;
    private static final int ROTATION_DEFAULT_X = -10;
    private static final int ROTATION_DEFAULT_Y = 15;
    private static final float ZOOM_DEFAULT = 0.6f;
    private static final float ZOOM_MIN = 0.33f;
    private static final float ZOOM_MAX = 2f;
    private static final int SPACING_DEFAULT = 25;
    private static final int SPACING_MIN = 10;
    private static final int SPACING_MAX = 200;
    private static final int TEXT_COLOR = 0xFF000000;
    private static final int CHROME_COLOR = 0xFF888888;
    private static final int CHROME_SHADOW_COLOR = 0xFF000000;
    private static final int TEXT_OFFSET_DP = 2;
    private static final int TEXT_SIZE_DP = 15;
    private static final int BORDER_WIDTH = 1;
    private static final int CHILD_COUNT_ESTIMATION = 25;
    private static final boolean DEBUG = false;

    private static final int SLOT_TOUCH_RATIO = 2;

    private static void log(String message, Object... args) {
        Log.d(TAG, String.format(message, args));
    }

    private static class LayeredView {

        View view;
        int layer;

        void set(View view, int layer) {
            this.view = view;
            this.layer = layer;
        }

        void clear() {
            view = null;
            layer = -1;
        }
    }

    private final Rect viewBoundsRect = new Rect();
    private final Paint viewBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint viewTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Camera camera = new Camera();
    private final Matrix matrix = new Matrix();
    private final int[] location = new int[2];
    private final BitSet visibilities = new BitSet(CHILD_COUNT_ESTIMATION);
    private final SparseArray<String> idNames = new SparseArray<>();
    private final Deque<LayeredView> layeredViewQueue = new ArrayDeque<>();
    private final Pool<LayeredView> layeredViewPool = new Pool<LayeredView>(CHILD_COUNT_ESTIMATION) {
        @Override
        protected LayeredView newObject() {
            return new LayeredView();
        }
    };

    private final Resources res;
    private final float density;
    private final float slop;
    private final float textOffset;
    private final float textSize;

    private boolean enabled;
    private boolean drawViews = true;
    private boolean drawIds;

    private int pointerOne = MotionEvent.INVALID_POINTER_ID;
    private float lastOneX;
    private float lastOneY;
    private int pointerTwo = MotionEvent.INVALID_POINTER_ID;
    private float lastTwoX;
    private float lastTwoY;
    private int multiTouchTracking = TRACKING_UNKNOWN;

    private float rotationY = ROTATION_DEFAULT_Y;
    private float rotationX = ROTATION_DEFAULT_X;
    private float zoom = ZOOM_DEFAULT;
    private float spacing = SPACING_DEFAULT;

    private int alarmLayer = 3;
    private int textColor;
    private int chromeColor;
    private int chromeAlarmColor;
    private int chromeShadowColor;

    public ViewerPanel(Context context) {
        this(context, null);
    }

    public ViewerPanel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ViewerPanel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        res = context.getResources();
        density = context.getResources()
                         .getDisplayMetrics().density;
        slop = ViewConfiguration.get(context)
                                .getScaledTouchSlop();

        textSize = TEXT_SIZE_DP * density;
        textOffset = TEXT_OFFSET_DP * density;

        setTextColor(TEXT_COLOR);
        viewTextPaint.setStyle(Paint.Style.STROKE);
        viewTextPaint.setTextSize(textSize);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            viewTextPaint.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
        }

        setChromeColor(CHROME_COLOR);
        viewBorderPaint.setStyle(Paint.Style.STROKE);
        viewBorderPaint.setTextSize(textSize);
        viewBorderPaint.setStrokeWidth(BORDER_WIDTH * density);
        setChromeShadowColor(CHROME_SHADOW_COLOR);
    }

    public ViewerPanel setAlarmLayer(int index) {
        if (alarmLayer != index) {
            alarmLayer = index;
            invalidate();
        }
        return this;
    }

    public int getAlarmLayer() {
        return alarmLayer;
    }

    /** Set the view Text color. */
    public ViewerPanel setTextColor(int color) {
        if (textColor != color) {
            viewTextPaint.setColor(color);
            textColor = color;
            invalidate();
        }
        return this;
    }

    /** Get the view Text color. */
    public int getTextColor() {
        return textColor;
    }

    /** Set the view border chrome color. */
    public ViewerPanel setChromeColor(int color) {
        if (chromeColor != color) {
            viewBorderPaint.setColor(color);
            chromeColor = color;
            invalidate();
        }
        return this;
    }

    /** Get the view border chrome color. */
    public int getChromeColor() {
        return chromeColor;
    }

    /** Set the view border chrome alarm color. */
    public ViewerPanel setChromeAlarmColor(int color) {
        if (chromeAlarmColor != color) {
            chromeAlarmColor = color;
            invalidate();
        }
        return this;
    }

    /** Get the view border chrome alarm color. */
    public int getChromeAlarmColor() {
        return chromeAlarmColor;
    }

    /** Set the view border chrome shadow color. */
    public ViewerPanel setChromeShadowColor(int color) {
        if (chromeShadowColor != color) {
            viewBorderPaint.setShadowLayer(1, -1, 1, color);
            chromeShadowColor = color;
            invalidate();
        }
        return this;
    }

    /** Get the view border chrome shadow color. */
    public int getChromeShadowColor() {
        return chromeShadowColor;
    }

    /** Set whether or not the 3D view layer interaction is enabled. */
    public ViewerPanel setLayerInteractionEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            setWillNotDraw(!enabled);
            invalidate();
        }
        return this;
    }

    /** Returns true when 3D view layer interaction is enabled. */
    public boolean isLayerInteractionEnabled() {
        return enabled;
    }

    /** Set whether the view layers draw their contents. When false, only wireframes are shown. */
    public ViewerPanel setDrawViews(boolean drawViews) {
        if (this.drawViews != drawViews) {
            this.drawViews = drawViews;
            invalidate();
        }
        return this;
    }

    /** Returns true when view layers draw their contents. */
    public boolean isDrawingViews() {
        return drawViews;
    }

    /** Set whether the view layers draw their IDs. */
    public ViewerPanel setDrawIds(boolean drawIds) {
        if (this.drawIds != drawIds) {
            this.drawIds = drawIds;
            invalidate();
        }
        return this;
    }

    /** Returns true when view layers draw their IDs. */
    public boolean isDrawingIds() {
        return drawIds;
    }

    public void setSpacing(int progress) {
        spacing = SPACING_DEFAULT + progress * SPACING_MAX / getWidth();
        spacing = Math.min(Math.max(spacing, SPACING_MIN), SPACING_MAX);
        invalidate();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return enabled || super.onInterceptTouchEvent(ev);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!enabled) {
            return super.onTouchEvent(event);
        }

        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int index = (action == MotionEvent.ACTION_DOWN) ? 0 : event.getActionIndex();
                if (pointerOne == MotionEvent.INVALID_POINTER_ID) {
                    pointerOne = event.getPointerId(index);
                    lastOneX = event.getX(index);
                    lastOneY = event.getY(index);
                    if (DEBUG) {
                        log("Got pointer 1!  id: %s  x: %s  y: %s", pointerOne, lastOneY, lastOneY);
                    }
                } else if (pointerTwo == MotionEvent.INVALID_POINTER_ID) {
                    pointerTwo = event.getPointerId(index);
                    lastTwoX = event.getX(index);
                    lastTwoY = event.getY(index);
                    if (DEBUG) {
                        log("Got pointer 2!  id: %s  x: %s  y: %s", pointerTwo, lastTwoY, lastTwoY);
                    }
                } else {
                    if (DEBUG) {
                        log("Ignoring additional pointer.  id: %s", event.getPointerId(index));
                    }
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (pointerTwo == MotionEvent.INVALID_POINTER_ID) {
                    // Single pointer controlling 3D rotation.
                    for (int i = 0, count = event.getPointerCount(); i < count; i++) {
                        if (pointerOne == event.getPointerId(i)) {
                            float eventX = event.getX(i);
                            float eventY = event.getY(i);
                            float dx = eventX - lastOneX;
                            float dy = eventY - lastOneY;
                            float drx = 90 * (dx / getWidth());
                            float dry = 90 * (-dy / getHeight());
                            // An 'x' delta affects 'y' rotation and vise versa.
                            rotationY = Math.min(Math.max(rotationY + drx, ROTATION_MIN), ROTATION_MAX);
                            rotationX = Math.min(Math.max(rotationX + dry, ROTATION_MIN), ROTATION_MAX);
                            if (DEBUG) {
                                log("Single pointer moved (%s, %s) affecting rotation (%s, %s).", dx, dy, drx, dry);
                            }

                            lastOneX = eventX;
                            lastOneY = eventY;

                            invalidate();
                        }
                    }
                } else {
                    // We know there's two pointers and we only care about pointerOne and pointerTwo
                    int pointerOneIndex = event.findPointerIndex(pointerOne);
                    int pointerTwoIndex = event.findPointerIndex(pointerTwo);

                    float xOne = event.getX(pointerOneIndex);
                    float yOne = event.getY(pointerOneIndex);
                    float xTwo = event.getX(pointerTwoIndex);
                    float yTwo = event.getY(pointerTwoIndex);

                    float dxOne = xOne - lastOneX;
                    float dyOne = yOne - lastOneY;
                    float dxTwo = xTwo - lastTwoX;
                    float dyTwo = yTwo - lastTwoY;

                    if (multiTouchTracking == TRACKING_UNKNOWN) {
                        float adx = Math.abs(dxOne) + Math.abs(dxTwo);
                        float ady = Math.abs(dyOne) + Math.abs(dyTwo);

                        if (adx > slop * SLOT_TOUCH_RATIO || ady > slop * SLOT_TOUCH_RATIO) {
//                            if (adx > ady) {
//                                // Left/right movement wins. Track horizontal.
//                                multiTouchTracking = TRACKING_HORIZONTALLY;
//                            } else {
//                                // Up/down movement wins. Track vertical.
//                                multiTouchTracking = TRACKING_VERTICALLY;
//                            }
                            multiTouchTracking = TRACKING_ALL;
                        }
                    }

                    if (multiTouchTracking == TRACKING_ALL) {
                        if (yOne >= yTwo) {
                            zoom += dyOne / getHeight() - dyTwo / getHeight();
                        } else {
                            zoom += dyTwo / getHeight() - dyOne / getHeight();
                        }

                        zoom = Math.min(Math.max(zoom, ZOOM_MIN), ZOOM_MAX);
                        invalidate();
                    } else if (multiTouchTracking == TRACKING_VERTICALLY) {
                        if (yOne >= yTwo) {
                            zoom += dyOne / getHeight() - dyTwo / getHeight();
                        } else {
                            zoom += dyTwo / getHeight() - dyOne / getHeight();
                        }

                        zoom = Math.min(Math.max(zoom, ZOOM_MIN), ZOOM_MAX);
                        invalidate();
                    } else if (multiTouchTracking == TRACKING_HORIZONTALLY) {
                        if (xOne >= xTwo) {
                            spacing += (dxOne / getWidth() * SPACING_MAX) - (dxTwo / getWidth() * SPACING_MAX);
                        } else {
                            spacing += (dxTwo / getWidth() * SPACING_MAX) - (dxOne / getWidth() * SPACING_MAX);
                        }

                        spacing = Math.min(Math.max(spacing, SPACING_MIN), SPACING_MAX);
                        invalidate();
                    }

                    if (multiTouchTracking != TRACKING_UNKNOWN) {
                        lastOneX = xOne;
                        lastOneY = yOne;
                        lastTwoX = xTwo;
                        lastTwoY = yTwo;
                    }
                }
                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                int index = (action != MotionEvent.ACTION_POINTER_UP) ? 0 : event.getActionIndex();
                int pointerId = event.getPointerId(index);
                if (pointerOne == pointerId) {
                    // Shift pointer two (real or invalid) up to pointer one.
                    pointerOne = pointerTwo;
                    lastOneX = lastTwoX;
                    lastOneY = lastTwoY;
                    if (DEBUG) {
                        log("Promoting pointer 2 (%s) to pointer 1.", pointerTwo);
                    }
                    // Clear pointer two and tracking.
                    pointerTwo = MotionEvent.INVALID_POINTER_ID;
                    multiTouchTracking = TRACKING_UNKNOWN;
                } else if (pointerTwo == pointerId) {
                    if (DEBUG) {
                        log("Lost pointer 2 (%s).", pointerTwo);
                    }
                    pointerTwo = MotionEvent.INVALID_POINTER_ID;
                    multiTouchTracking = TRACKING_UNKNOWN;
                }
                break;
            }
            default:
                break;
        }

        return true;
    }

    @Override
    public void draw(Canvas canvas) {
        if (!enabled) {
            super.draw(canvas);
            return;
        }

        getLocationInWindow(location);
        float x = location[0];
        float y = location[1];

        int saveCount = canvas.save();

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;

        camera.save();
        camera.rotate(rotationX, rotationY, 0);
        camera.getMatrix(matrix);
        camera.restore();

        matrix.preTranslate(-cx, -cy);
        matrix.postTranslate(cx, cy);
        canvas.concat(matrix);
        canvas.scale(zoom, zoom, cx, cy);

        if (!layeredViewQueue.isEmpty()) {
            throw new AssertionError("View queue is not empty.");
        }

        // We don't want to be rendered so seed the queue with our children.
        for (int i = 0, count = getChildCount(); i < count; i++) {
            LayeredView layeredView = layeredViewPool.obtain();
            layeredView.set(getChildAt(i), 0);
            layeredViewQueue.add(layeredView);
        }

        while (!layeredViewQueue.isEmpty()) {
            LayeredView layeredView = layeredViewQueue.removeFirst();
            View view = layeredView.view;
            int layer = layeredView.layer;

            // Restore the object to the pool for use later.
            layeredView.clear();
            layeredViewPool.restore(layeredView);

            // Hide any visible children.
            if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                visibilities.clear();
                for (int i = 0, count = viewGroup.getChildCount(); i < count; i++) {
                    View child = viewGroup.getChildAt(i);
                    //noinspection ConstantConditions
                    if (child.getVisibility() == VISIBLE) {
                        visibilities.set(i);
                        child.setVisibility(INVISIBLE);
                    }
                }
            }

            int viewSaveCount = canvas.save();

            // Scale the layer index translation by the rotation amount.
            float translateShowX = rotationY / ROTATION_MAX;
            float translateShowY = rotationX / ROTATION_MAX;
            float tx = layer * spacing * density * translateShowX;
            float ty = layer * spacing * density * translateShowY;
            canvas.translate(tx, -ty);

            view.getLocationInWindow(location);
            canvas.translate(location[0] - x, location[1] - y);

            viewBoundsRect.set(0, 0, view.getWidth(), view.getHeight());
            viewBorderPaint.setColor((layer > alarmLayer) ? chromeAlarmColor : chromeColor);
            canvas.drawRect(viewBoundsRect, viewBorderPaint);

            if (drawViews) {
                view.draw(canvas);
            }

            if (drawIds) {
                int id = view.getId();
                if (id != NO_ID) {
                    canvas.drawText(nameForId(id), textOffset, textSize, viewTextPaint);
                }
            }

            canvas.restoreToCount(viewSaveCount);

            // Restore any hidden children and queue them for later drawing.
            if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                for (int i = 0, count = viewGroup.getChildCount(); i < count; i++) {
                    if (visibilities.get(i)) {
                        View child = viewGroup.getChildAt(i);
                        //noinspection ConstantConditions
                        child.setVisibility(VISIBLE);
                        LayeredView childLayeredView = layeredViewPool.obtain();
                        childLayeredView.set(child, layer + 1);
                        layeredViewQueue.add(childLayeredView);
                    }
                }
            }
        }

        canvas.restoreToCount(saveCount);
    }

    private String nameForId(int id) {
        String name = idNames.get(id);
        if (name == null) {
            try {
                name = res.getResourceEntryName(id);
            } catch (Resources.NotFoundException e) {
                name = String.format("0x%8x", id);
            }
            idNames.put(id, name);
        }
        return name;
    }

    private static abstract class Pool<T> {

        private final Deque<T> pool;

        Pool(int initialSize) {
            pool = new ArrayDeque<>(initialSize);
            for (int i = 0; i < initialSize; i++) {
                pool.addLast(newObject());
            }
        }

        T obtain() {
            return pool.isEmpty() ? newObject() : pool.removeLast();
        }

        void restore(T instance) {
            pool.addLast(instance);
        }

        protected abstract T newObject();
    }
}
