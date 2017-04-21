package com.goldberg.blurdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private int radius = 20;//the radius of the Blur
    private float downscaleFactor = .12f;//图片缩小

    private RenderScript rs;
    private ScriptIntrinsicBlur blurScript;
    private Toolbar toolbar;
    private NestedScrollView nestedScrollView;
    private static final int SUCCESS = 1;
    private static final int SUCCESS_POP = 2;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Bitmap bitmap;
    private Bitmap bitmapPop;
    private Handler handler = new Handler() {//不要介意。。

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SUCCESS:
                    toolbar.setBackground(new BitmapDrawable(getResources(), bitmap));
                    break;
                case SUCCESS_POP:
                    imageView.setImageBitmap(bitmapPop);
                    break;
            }
        }
    };
    private int mScrollY;

    private Runnable runnable = new Runnable() {

        @Override

        public void run() {
            long time = System.currentTimeMillis();
            final int[] location = new int[2];
            toolbar.getLocationOnScreen(location);
            Bitmap bitmap1 = getDownscaledBitmapForView(nestedScrollView, new Rect(location[0], location[1] + mScrollY,
                    location[0] + toolbar.getWidth(),
                    location[1] + toolbar.getHeight() + mScrollY), downscaleFactor);
//            bitmap = FastBlurUtil.doBlur(bitmap1, 20, false);
            bitmap = blurBitmap(getApplicationContext(), bitmap1, rs,
                    blurScript, radius);
            handler.sendEmptyMessage(SUCCESS);
            Log.e("blurBitmap", System.currentTimeMillis() - time + "");
        }
    };

    private Runnable runnablePopWindow = new Runnable() {

        @Override

        public void run() {
            long time = System.currentTimeMillis();
            final int[] location = new int[2];
            imageView.getLocationOnScreen(location);
            Bitmap bitmap = getDownscaledBitmapForView(nestedScrollView, new Rect(location[0], location[1] + mScrollY,
                    location[0] + imageView.getWidth(),
                    location[1] + imageView.getHeight() + mScrollY), downscaleFactor);

            bitmapPop = blurBitmap(getApplicationContext(), bitmap, rs,
                    blurScript, radius);
            handler.sendEmptyMessage(SUCCESS_POP);
            Log.e("blurBitmap", System.currentTimeMillis() - time + "");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
    }

    private void initViews() {
        rs = RenderScript.create(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        }

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(null);
        nestedScrollView = (NestedScrollView) findViewById(R.id.nestedScrollView);
        toolbar.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                toolbar.post(new Runnable() {
                    @Override
                    public void run() {
                        int statusBarHeight = getStatusBarHeight(getApplicationContext());
                        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) toolbar.getLayoutParams();
                        params.height += statusBarHeight;
                        toolbar.setLayoutParams(params);
                        toolbar.setPadding(0, statusBarHeight, 0, 0);

                        final int[] location = new int[2];
                        toolbar.getLocationOnScreen(location);
                        Bitmap bitmap = getDownscaledBitmapForView(nestedScrollView, new Rect(location[0], location[1],
                                location[0] + toolbar.getWidth(),
                                location[1] + toolbar.getHeight()), downscaleFactor);

                        bitmap = blurBitmap(getApplicationContext(), bitmap, rs,
                                blurScript, radius);
                        toolbar.setBackground(new BitmapDrawable(getResources(), bitmap));
                    }
                });
                toolbar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        nestedScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                executorService.execute(runnable);
                if (popupWindow_more != null && popupWindow_more.isShowing()) {
                    executorService.execute(runnablePopWindow);
                }
                mScrollY = scrollY;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_add:
                popWindow();
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    private PopupWindow popupWindow_more;

    private ImageView imageView;

    private void popWindow() {
        if (popupWindow_more == null) {
            popupWindow_more = new PopupWindow();
            popupWindow_more.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
            popupWindow_more.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
            final View view = getLayoutInflater().inflate(R.layout.popup_add, null);
            imageView = (ImageView) view.findViewById(R.id.imageView);
            popupWindow_more.setContentView(view);
            popupWindow_more.setBackgroundDrawable(new ColorDrawable(0x00000000));
            popupWindow_more.setOutsideTouchable(true);
            popupWindow_more.setFocusable(true);
        }
        final int[] location = new int[2];
        imageView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                long time = System.currentTimeMillis();

                imageView.getLocationOnScreen(location);
                Bitmap bitmap = getDownscaledBitmapForView(nestedScrollView, new Rect(location[0], location[1] + mScrollY,
                        location[0] + imageView.getWidth(),
                        location[1] + imageView.getHeight() + mScrollY), downscaleFactor);

                bitmap = blurBitmap(getApplicationContext(), bitmap, rs,
                        blurScript, radius);
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) imageView.getLayoutParams();
                params.width = imageView.getWidth();
                params.height = imageView.getHeight();
                imageView.setLayoutParams(params);
                imageView.setImageBitmap(bitmap);

                imageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        if (popupWindow_more.isShowing()) {
            popupWindow_more.dismiss();
        } else {
            popupWindow_more.showAtLocation(toolbar, Gravity.CENTER, 0, 0);
        }


    }

    public static Bitmap blurBitmap(Context context, Bitmap bitmap, RenderScript rs, ScriptIntrinsicBlur blurScript, int radius) {

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
//            Let 's create an empty bitmap with the same size of the bitmap we want to blur
            Bitmap outBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_4444);
//            //Instantiate a new Renderscript
//            RenderScript rs = RenderScript.create(context);
////            Create an Intrinsic Blur Script using the Renderscript
//            ScriptIntrinsicBlur blurScript1 = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
            //Create the Allocations (in/out) with the Renderscript and the in/out bitmaps
            Allocation allIn = Allocation.createFromBitmap(rs, bitmap);
            Allocation allOut = Allocation.createFromBitmap(rs, outBitmap);
            //Set the radius of the blur
            blurScript.setRadius(radius);
            //Perform the Renderscript
            blurScript.setInput(allIn);
            blurScript.forEach(allOut);
            //Copy the final bitmap created by the out Allocation to the outBitmap
            allOut.copyTo(outBitmap);
            //recycle the original bitmap
            bitmap.recycle();
            //After finishing everything, we destroy the Renderscript.
//            rs.destroy();
            return outBitmap;
        } else {
            return bitmap;
        }
    }


    /**
     * Users a View reference to create a bitmap, and downscales it using the passed in factor.
     * Uses a Rect to crop the view into the bitmap.
     *
     * @return Bitmap made from view, downscaled by downscaleFactor.
     */
    public static Bitmap getDownscaledBitmapForView(View view, Rect crop, float downscaleFactor) {
//        View screenView = view.getRootView();

        int width = (int) (crop.width() * downscaleFactor);
        int height = (int) (crop.height() * downscaleFactor);

        if (view.getWidth() <= 0 || view.getHeight() <= 0 || width <= 0 || height <= 0) {
            return null;
        }

        float dx = -crop.left * downscaleFactor;
        float dy = -crop.top * downscaleFactor;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bitmap);
        Matrix matrix = new Matrix();
        matrix.preScale(downscaleFactor, downscaleFactor);
        matrix.postTranslate(dx, dy);
        canvas.setMatrix(matrix);
        view.draw(canvas);

        return bitmap;
    }

    public static int getStatusBarHeight(Context context) {
        Class<?> c;
        Object obj;
        Field field;
        int x, statusBarHeight = 0;
        try {
            c = Class.forName("com.android.internal.R$dimen");
            obj = c.newInstance();
            field = c.getField("status_bar_height");
            x = Integer.parseInt(field.get(obj).toString());
            statusBarHeight = context.getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return statusBarHeight;
    }

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return Math.round(dpValue * scale);
    }
}
