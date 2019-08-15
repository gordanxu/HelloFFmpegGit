package com.gordan.baselibrary.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;


public class ImageUtils
{
    final static String TAG=ImageUtils.class.getSimpleName();
    /**
     * 水平方向模糊度
     */
    private static float hRadius = 15;
    /**
     * 竖直方向模糊度
     */
    private static float vRadius = 15;
    /**
     * 模糊迭代度
     */
    private static int iterations = 7;


    /***
     * 将Drawable转换为Bitmap
     *
     * 两者相互转换：https://blog.csdn.net/zw904448290/article/details/53068914/
     *
     * @param drawable
     * @return
     */
    public static Bitmap drawable2Bitmap(Drawable drawable) {
        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();
        Bitmap.Config config =
                drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                        : Bitmap.Config.RGB_565;
        Bitmap bitmap = Bitmap.createBitmap(w, h, config);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, w, h);
        drawable.draw(canvas);

        return bitmap;
    }

    /***
     * 将Bitmap模糊化（简称高斯模糊）
     *
     * 其实高斯模糊还有通过C/C++层处理的方法
     *
     * @param bmp
     * @return
     */
    public static Drawable BoxBlurFilter(Bitmap bmp) {
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        int[] inPixels = new int[width * height];
        int[] outPixels = new int[width * height];
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmp.getPixels(inPixels, 0, width, 0, 0, width, height);
        for (int i = 0; i < iterations; i++) {
            blur(inPixels, outPixels, width, height, hRadius);
            blur(outPixels, inPixels, height, width, vRadius);
        }
        blurFractional(inPixels, outPixels, width, height, hRadius);
        blurFractional(outPixels, inPixels, height, width, vRadius);
        bitmap.setPixels(inPixels, 0, width, 0, 0, width, height);
        Drawable drawable = new BitmapDrawable(bitmap);
        return drawable;
    }

    public static void blur(int[] in, int[] out, int width, int height,
                            float radius) {
        int widthMinus1 = width - 1;
        int r = (int) radius;
        int tableSize = 2 * r + 1;
        int divide[] = new int[256 * tableSize];

        for (int i = 0; i < 256 * tableSize; i++)
            divide[i] = i / tableSize;

        int inIndex = 0;

        for (int y = 0; y < height; y++) {
            int outIndex = y;
            int ta = 0, tr = 0, tg = 0, tb = 0;

            for (int i = -r; i <= r; i++) {
                int rgb = in[inIndex + clamp(i, 0, width - 1)];
                ta += (rgb >> 24) & 0xff;
                tr += (rgb >> 16) & 0xff;
                tg += (rgb >> 8) & 0xff;
                tb += rgb & 0xff;
            }

            for (int x = 0; x < width; x++) {
                out[outIndex] = (divide[ta] << 24) | (divide[tr] << 16)
                        | (divide[tg] << 8) | divide[tb];

                int i1 = x + r + 1;
                if (i1 > widthMinus1)
                    i1 = widthMinus1;
                int i2 = x - r;
                if (i2 < 0)
                    i2 = 0;
                int rgb1 = in[inIndex + i1];
                int rgb2 = in[inIndex + i2];

                ta += ((rgb1 >> 24) & 0xff) - ((rgb2 >> 24) & 0xff);
                tr += ((rgb1 & 0xff0000) - (rgb2 & 0xff0000)) >> 16;
                tg += ((rgb1 & 0xff00) - (rgb2 & 0xff00)) >> 8;
                tb += (rgb1 & 0xff) - (rgb2 & 0xff);
                outIndex += height;
            }
            inIndex += width;
        }
    }

    public static void blurFractional(int[] in, int[] out, int width,
                                      int height, float radius) {
        radius -= (int) radius;
        float f = 1.0f / (1 + 2 * radius);
        int inIndex = 0;

        for (int y = 0; y < height; y++) {
            int outIndex = y;

            out[outIndex] = in[0];
            outIndex += height;
            for (int x = 1; x < width - 1; x++) {
                int i = inIndex + x;
                int rgb1 = in[i - 1];
                int rgb2 = in[i];
                int rgb3 = in[i + 1];

                int a1 = (rgb1 >> 24) & 0xff;
                int r1 = (rgb1 >> 16) & 0xff;
                int g1 = (rgb1 >> 8) & 0xff;
                int b1 = rgb1 & 0xff;
                int a2 = (rgb2 >> 24) & 0xff;
                int r2 = (rgb2 >> 16) & 0xff;
                int g2 = (rgb2 >> 8) & 0xff;
                int b2 = rgb2 & 0xff;
                int a3 = (rgb3 >> 24) & 0xff;
                int r3 = (rgb3 >> 16) & 0xff;
                int g3 = (rgb3 >> 8) & 0xff;
                int b3 = rgb3 & 0xff;
                a1 = a2 + (int) ((a1 + a3) * radius);
                r1 = r2 + (int) ((r1 + r3) * radius);
                g1 = g2 + (int) ((g1 + g3) * radius);
                b1 = b2 + (int) ((b1 + b3) * radius);
                a1 *= f;
                r1 *= f;
                g1 *= f;
                b1 *= f;
                out[outIndex] = (a1 << 24) | (r1 << 16) | (g1 << 8) | b1;
                outIndex += height;
            }
            out[outIndex] = in[width - 1];
            inIndex += width;
        }
    }

    public static int clamp(int x, int a, int b) {
        return (x < a) ? a : (x > b) ? b : x;
    }

    public static Bitmap resizeImage(Bitmap bitmap, int rect, boolean isMax, boolean isZoomOut)
    {
        try
        {
            // load the origial Bitmap

            int width = bitmap.getWidth();

            int height = bitmap.getHeight();

            if (!isZoomOut && rect >= width && rect >= height)
            {
                return bitmap;
            }
            int newWidth = 0;
            int newHeight = 0;

            if (isMax || isZoomOut)
            {
                newWidth = width >= height ? rect : rect * width / height;
                newHeight = width <= height ? rect : rect * height / width;
            }
            else
            {
                newWidth = width <= height ? rect : rect * width / height;
                newHeight = width >= height ? rect : rect * height / width;
            }

            if (width >= height)
            {
                if (isMax)
                {
                    newWidth = rect;
                    newHeight = height * newWidth / width;
                }
                else
                {
                    if (isZoomOut)
                    {
                        newWidth = rect;
                        newHeight = height * newWidth / width;
                    }
                    else
                    {
                        newHeight = rect;
                        newWidth = width * newHeight / height;
                    }
                }
            }
            else
            {
                if (!isMax)
                {
                    newWidth = rect;
                    newHeight = height * newWidth / width;
                }
                else
                {
                    newHeight = rect;
                    newWidth = width * newHeight / height;
                }
            }

            // calculate the scale
            float scaleWidth = 0f;
            float scaleHeight = 0f;

            scaleWidth = ((float) newWidth) / width;

            scaleHeight = ((float) newHeight) / height;

            Matrix matrix = new Matrix();

            matrix.postScale(scaleWidth, scaleHeight);

            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width,

                    height, matrix, true);
        } catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }

        return bitmap;

    }

    /**
     * 压缩图片的size
     *
     * @param
     *
     * @param size
     *            尺寸
     * @return
     * @throws IOException
     */
    public static byte[] revitionImageSize(Bitmap bitmap, int maxRect, int size)
    {
        byte[] b = null;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try
        {
            bitmap = resizeImage(bitmap, maxRect, true, false);

            if (bitmap == null)
            {
                return null;
            }

            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, os);
            b = os.toByteArray();
            int options = 80;
            while (b.length > size)
            {
                os.reset();
                bitmap.compress(Bitmap.CompressFormat.JPEG, options, os);
                b = os.toByteArray();
                options -= 10;
            }
            os.flush();
            os.close();
            bitmap = BitmapFactory.decodeByteArray(new byte[0], 0, 0);
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
        return b;
    }

    /**
     * 压缩图片的size
     *
     * @param
     *
     * @param size
     *            尺寸
     * @return
     * @throws IOException
     */
    public static Bitmap revitionBitmap(Bitmap bitmap, int maxRect, int size)
    {
        byte[] b = null;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try
        {
            bitmap = resizeImage(bitmap, maxRect, true, false);

            if (bitmap == null)
            {
                return null;
            }

            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, os);
            b = os.toByteArray();
            int options = 80;
            while (b.length > size)
            {
                os.reset();
                bitmap.compress(Bitmap.CompressFormat.JPEG, options, os);
                b = os.toByteArray();
                options -= 10;
            }
            os.flush();
            os.close();

        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
        return bitmap;
    }

    /**
     * 获得圆角图片
     *
     * @param bitmap
     *            图片
     * @param roundPx
     *            角度
     * @return
     */
    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, float roundPx)
    {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Bitmap output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, w, h);
        final RectF rectF = new RectF(rect);
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    /**
     * 获取圆形图片对象
     *
     * @param source
     *            资源图片
     * @return
     */
    public static Bitmap getCircleBitmap(Bitmap source)
    {
        if (source == null)
        {
            return null;
        }

        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        int min = Math.min(sourceWidth, sourceHeight);

        Bitmap target = Bitmap.createBitmap(min, min, Bitmap.Config.ARGB_8888);
        /**
         * 产生一个同样大小的画布
         */
        Canvas canvas = new Canvas(target);
        /**
         * 首先绘制圆形
         */
        canvas.drawCircle(min / 2, min / 2, min / 2, paint);
        /**
         * 使用SRC_IN
         */
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        /**
         * 绘制图片
         */
        // canvas.drawBitmap(source, (sourceWidth - min) / 2,
        // (sourceHeight - min) / 2, paint);
        canvas.drawBitmap(source, 0, 0, paint);
        return target;
    }


    /**
     * 旋转图片
     *
     * @param angle
     * @param bitmap
     * @return Bitmap
     */
    public static Bitmap rotaingImageView(int angle, Bitmap bitmap)
    {
        if (angle == 0 || bitmap == null)
        {
            return bitmap;
        }
        // 旋转图片 动作
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        // 创建新的图片
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return resizedBitmap;
    }



    /**
     * 根据InputStream获取图片实际的宽度和高度
     *
     * @param imageStream
     * @return
     */
    public static ImageSize getImageSize(InputStream imageStream)
    {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(imageStream, null, options);
        return new ImageSize(options.outWidth, options.outHeight);
    }

    public static class ImageSize
    {
        int width;
        int height;

        public ImageSize()
        {
        }

        public ImageSize(int width, int height)
        {
            this.width = width;
            this.height = height;
        }

        @Override
        public String toString()
        {
            return "ImageSize{" +
                    "width=" + width +
                    ", height=" + height +
                    '}';
        }
    }

    public static int calculateInSampleSize(ImageSize srcSize, ImageSize targetSize)
    {
        // 源图片的宽度
        int width = srcSize.width;
        int height = srcSize.height;
        int inSampleSize = 1;

        int reqWidth = targetSize.width;
        int reqHeight = targetSize.height;

        if (width > reqWidth && height > reqHeight)
        {
            // 计算出实际宽度和目标宽度的比率
            int widthRatio = Math.round((float) width / (float) reqWidth);
            int heightRatio = Math.round((float) height / (float) reqHeight);
            inSampleSize = Math.max(widthRatio, heightRatio);
        }
        return inSampleSize;
    }

    /**
     * 根据ImageView获适当的压缩的宽和高
     *
     * @param view
     * @return
     */
    public static ImageSize getImageViewSize(View view)
    {

        ImageSize imageSize = new ImageSize();

        imageSize.width = getExpectWidth(view);
        imageSize.height = getExpectHeight(view);

        return imageSize;
    }

    /**
     * 根据view获得期望的高度
     *
     * @param view
     * @return
     */
    private static int getExpectHeight(View view)
    {
        int height = 0;
        if (view == null) return 0;

        final ViewGroup.LayoutParams params = view.getLayoutParams();
        //如果是WRAP_CONTENT，此时图片还没加载，getWidth根本无效
        if (params != null && params.height != ViewGroup.LayoutParams.WRAP_CONTENT)
        {
            height = view.getWidth(); // 获得实际的宽度
        }
        if (height <= 0 && params != null)
        {
            height = params.height; // 获得布局文件中的声明的宽度
        }

        if (height <= 0)
        {
            height = getImageViewFieldValue(view, "mMaxHeight");// 获得设置的最大的宽度
        }

        //如果宽度还是没有获取到，憋大招，使用屏幕的宽度
        if (height <= 0)
        {
            DisplayMetrics displayMetrics = view.getContext().getResources()
                    .getDisplayMetrics();
            height = displayMetrics.heightPixels;
        }

        return height;
    }

    /**
     * 根据view获得期望的宽度
     *
     * @param view
     * @return
     */
    private static int getExpectWidth(View view)
    {
        int width = 0;
        if (view == null) return 0;

        final ViewGroup.LayoutParams params = view.getLayoutParams();
        //如果是WRAP_CONTENT，此时图片还没加载，getWidth根本无效
        if (params != null && params.width != ViewGroup.LayoutParams.WRAP_CONTENT)
        {
            width = view.getWidth(); // 获得实际的宽度
        }
        if (width <= 0 && params != null)
        {
            width = params.width; // 获得布局文件中的声明的宽度
        }

        if (width <= 0)

        {
            width = getImageViewFieldValue(view, "mMaxWidth");// 获得设置的最大的宽度
        }
        //如果宽度还是没有获取到，憋大招，使用屏幕的宽度
        if (width <= 0)

        {
            DisplayMetrics displayMetrics = view.getContext().getResources()
                    .getDisplayMetrics();
            width = displayMetrics.widthPixels;
        }

        return width;
    }

    /**
     * 通过反射获取imageview的某个属性值
     *
     * @param object
     * @param fieldName
     * @return
     */
    private static int getImageViewFieldValue(Object object, String fieldName)
    {
        int value = 0;
        try
        {
            Field field = ImageView.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            int fieldValue = field.getInt(object);
            if (fieldValue > 0 && fieldValue < Integer.MAX_VALUE)
            {
                value = fieldValue;
            }
        } catch (Exception e)
        {
        }
        return value;
    }

    public static Bitmap getRotateBitmap(Bitmap b, float rotateDegree){
        Matrix matrix = new Matrix();
        matrix.postRotate((float)rotateDegree);
        Bitmap rotaBitmap = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, false);
        return rotaBitmap;
    }


    public static boolean saveBitmap2File(Bitmap b, File file) {

        LogUtils.i(TAG,"=====saveBitmap2File====",false);
        FileOutputStream fos=null;
        BufferedOutputStream bos=null;

        try {
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            b.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
            fos.close();
            LogUtils.i(TAG,"=====saveBitmap2File success====",false);
            return true;

        } catch (IOException e) {
            e.printStackTrace();
        }
        LogUtils.i(TAG,"=====saveBitmap2File failed====",false);
        return false;
    }

    public static boolean saveImageByte2File(byte[] image, File file) {
        LogUtils.i(TAG,"=====saveImageByte2File ====",false);
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(file);
            output.write(image);
            LogUtils.i(TAG,"=====saveImageByte2File success====",false);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != output) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        LogUtils.i(TAG,"=====saveImageByte2File success====",false);
        return false;
    }

}
