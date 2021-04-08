package com.mx.provider;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.util.Log;

import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.ImageUtils;
import com.blankj.utilcode.util.ScreenUtils;
import com.mx.common.LogU;

import java.io.ByteArrayOutputStream;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;

public class ScreenShotHelper {

    private final String TAG = "ScreenShotHelper";

    private int mImageWidth;
    private int mImageHeight;
    private int mScreenDensity;

    public interface OnScreenShotListener {
        void onShotFinish(byte[] bitmap);
    }

    public static ImageReader mImageReader; // 比较特殊，需要其它类持有这个对象，才不会被回收。
    private OnScreenShotListener mOnScreenShotListener;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private final SoftReference<Context> mRefContext;

    public ScreenShotHelper(Context context, int resultCode, Intent data, OnScreenShotListener onScreenShotListener) {
        this.mOnScreenShotListener = onScreenShotListener;
        this.mRefContext = new SoftReference<Context>(context);
        getScreenBaseInfo();
        mMediaProjection = getMediaProjectionManager().getMediaProjection(resultCode, data);
//        mImageReader = ImageReader.newInstance(getScreenWidth(), getScreenHeight(), PixelFormat.RGBA_8888, 1);
        mImageReader = ImageReader.newInstance(mImageWidth, mImageHeight, PixelFormat.RGBA_8888, 2);
    }

    /**
     * 获取屏幕相关数据
     */
    private void getScreenBaseInfo() {
        mImageWidth = (int) (ScreenUtils.getScreenWidth()) ;//* Config.IMAGE_SCALE);
        mImageHeight = (int) (ScreenUtils.getScreenHeight());// * Config.IMAGE_SCALE);
        mScreenDensity = ScreenUtils.getScreenDensityDpi();
    }

    /**
     * 开始截屏
     */
    public void startScreenShot() {
        createVirtualDisplay();
        mImageReader.setOnImageAvailableListener(new ImageAvailableListener(), null);
    }


    private MediaProjectionManager getMediaProjectionManager() {
        return (MediaProjectionManager) getContext().getSystemService(
                Context.MEDIA_PROJECTION_SERVICE);
    }

    private void createVirtualDisplay() {
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("screen-mirror",
                mImageWidth,
                mImageHeight,
                mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mImageReader.getSurface(), null, null
        );
    }

    private Context getContext() {
        return mRefContext.get();
    }


    /**
     * 屏幕发生变化时截取
     */
    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
        @Override
        public void onImageAvailable(ImageReader reader) {
            try (Image image = reader.acquireLatestImage()) {
                if (image != null) {
                    int width = image.getWidth();
                    int height = image.getHeight();
                    final Image.Plane[] planes = image.getPlanes();
                    final ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * width;
                    Log.e("Global","image avaiable w: "+width+" h: "+height+ "  rowStride: "+rowStride+" rowpadding: "+rowPadding+" pixelStride: "+pixelStride);
                    // Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
                    Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_4444);
                    bitmap.copyPixelsFromBuffer(buffer);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);

                    // todo 可以在这里处理Bitmap
//                    bitmap = ImageUtils.compressByQuality(bitmap,5);

                    if (mOnScreenShotListener != null) {
                        if (bitmap != null)
                            Log.e("Global","encode start");
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 25, baos);
                        byte[] by = baos.toByteArray();
                            Log.e("Global","encode end");
                            mOnScreenShotListener.onShotFinish(by);
                        /**byte[] bytes = new byte[buffer.remaining()];
                        buffer.get(bytes);
//                        byte[] bb = getBytesFromImageReader(reader);
                        mOnScreenShotListener.onShotFinish(bytes);*/
                    }
                    image.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public static byte[] bytebuffer2ByteArray(ByteBuffer buffer) {
        //重置 limit 和postion 值
//        buffer.flip();
        //获取buffer中有效大小
        int len=buffer.limit() - buffer.position();

        byte [] bytes=new byte[len];

        for (int i = 0; i < bytes.length; i++) {
            bytes[i]=buffer.get();

        }

        return bytes;
    }
    // Method taken from this answer:
    // https://stackoverflow.com/questions/44022062/converting-yuv-420-888-to-jpeg-and-saving-file-results-distorted-image

    public static byte[] Yuv420ImageToNv21(Image image){
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[width * height * 2/8];
        byte[] rowData = new byte[planes[0].getRowStride()];

        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    channelOffset = width * height + 1;
                    outputStride = 2;
                    break;
                case 2:
                    channelOffset = width * height;
                    outputStride = 2;
                    break;
            }

            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();

            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
        }
        return data;
    }
    /**
     * 从ImageReader中获取byte[]数据
     */
    public static byte[] getBytesFromImageReader(ImageReader imageReader) {
        try (Image image = imageReader.acquireNextImage()) {
            final Image.Plane[] planes = image.getPlanes();
            ByteBuffer b0 = planes[0].getBuffer();
            ByteBuffer b1 = planes[1].getBuffer();
            ByteBuffer b2 = planes[2].getBuffer();
            int y = b0.remaining(), u = y >> 2, v = u;
            byte[] bytes = new byte[y + u + v];
            if(b1.remaining() > u) { // y420sp
                b0.get(bytes, 0, b0.remaining());
                b1.get(bytes, y, b1.remaining()); // uv
            } else { // y420p
                b0.get(bytes, 0, b0.remaining());
                b1.get(bytes, y, b1.remaining()); // u
                b2.get(bytes, y + u, b2.remaining()); // v
            }
            return bytes;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}