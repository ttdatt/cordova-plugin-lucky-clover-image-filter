package cordova.plugin.sts.image.filter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import org.apache.commons.lang3.StringUtils;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

import android.graphics.Bitmap.*;
import android.util.DisplayMetrics;

import jp.co.cyberagent.android.gpuimage.*;

/**
 * Created by Dat Tran on 3/7/17.
 */

//private class GPUImageGLSurfaceView extends GLSurfaceView {
//    public GPUImageGLSurfaceView(Context context) {
//        super(context);
//    }
//
//    public GPUImageGLSurfaceView(Context context, AttributeSet attrs) {
//        super(context, attrs);
//    }
//
//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        if (mForceSize != null) {
//            super.onMeasure(MeasureSpec.makeMeasureSpec(mForceSize.width, MeasureSpec.EXACTLY),
//                    MeasureSpec.makeMeasureSpec(mForceSize.height, MeasureSpec.EXACTLY));
//        } else {
//            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        }
//    }
//}

class MySize {
    public MySize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    private int width;
    private int height;
}

public class ImageFilter extends CordovaPlugin {

    private static final int JPEG = 0;
    private static final int PNG = 1;

    private static final float NOT_AVAILABLE = -9999;

    private static String currentImagePath;
    private static String base64Image;
    private Bitmap currentPreviewImage;
    private Bitmap currentEditingImage;
    private Bitmap currentThumbnailImage;
//    private static GPUImage editingGPUImage;
//    private static GPUImage previewGPUImage;
//    private static GPUImage thumbnailGPUImage;

    //    private GLSurfaceView glSurfaceView;
    private static MySize screenSize;

    private Context context;

    public CallbackContext callbackContext;

    @Override
    protected void pluginInitialize() {
        super.pluginInitialize();

        context = this.cordova.getActivity();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        this.cordova.getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;
        screenSize = new MySize(width, height);

//        glSurfaceView = new GLSurfaceView(context);

//        editingGPUImage.setGLSurfaceView(glSurfaceView);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        this.callbackContext = callbackContext;

        if (action.equals("coolMethod")) {
            String message = args.getString(0);
//            this.coolMethod(message, callbackContext);
            return true;
        }
        if (action.equals("applyEffect")
                || action.equals("applyEffectForReview")
                || action.equals("applyEffectForThumbnail")) {
            String path = args.getString(0);
            String filterType = args.getString(1);
            double compressQuality = args.getDouble(2);
            int isBase64Image = args.getInt(3);

            if (action.equals("applyEffect"))
                this.applyEffect(path, filterType, compressQuality, isBase64Image, callbackContext);
            else if (action.equals("applyEffectForReview"))
                this.applyEffectForReview(path, filterType, compressQuality, isBase64Image, callbackContext);
            else if (action.equals("applyEffectForThumbnail"))
                this.applyEffectForThumbnail(path, filterType, compressQuality, isBase64Image, callbackContext);

            return true;
        }
        return false;
    }

    private void validateInput(String pathOrData, String filterType, int isBase64Image) {

        if (isBase64Image == 0) {
            if (!StringUtils.isEmpty(pathOrData) && !StringUtils.isEmpty(filterType)
                    && !pathOrData.equals(currentImagePath)) {

                currentImagePath = pathOrData;
                pathOrData = pathOrData.substring(7);

                currentEditingImage = BitmapFactory.decodeFile(pathOrData);

                float ratio = (float) screenSize.getWidth() / (float) currentEditingImage.getWidth();
                MySize newSize = new MySize(Math.round(currentEditingImage.getWidth() * ratio), Math.round(currentEditingImage.getHeight() * ratio));
                currentPreviewImage = Bitmap.createScaledBitmap(currentEditingImage, newSize.getWidth(), newSize.getHeight(), false);

                MySize thumbSize = new MySize(Math.round(currentPreviewImage.getWidth() * 0.2f), Math.round(currentPreviewImage.getHeight() * 0.2f));
                currentThumbnailImage = Bitmap.createScaledBitmap(currentPreviewImage, thumbSize.getWidth(), thumbSize.getHeight(), false);
            }
        } else if (isBase64Image == 1) {
            if (!StringUtils.isEmpty(pathOrData) && !StringUtils.isEmpty(filterType)
                    && !pathOrData.equals(base64Image)) {
                base64Image = pathOrData;

                currentEditingImage = base64ToBitmap(pathOrData);

                float ratio = (float) screenSize.getWidth() / (float) currentEditingImage.getWidth();
                MySize newSize = new MySize(Math.round(currentEditingImage.getWidth() * ratio), Math.round(currentEditingImage.getHeight() * ratio));
                currentPreviewImage = Bitmap.createScaledBitmap(currentEditingImage, newSize.getWidth(), newSize.getHeight(), false);

                MySize thumbSize = new MySize(Math.round(currentPreviewImage.getWidth() * 0.2f), Math.round(currentPreviewImage.getHeight() * 0.2f));
                currentThumbnailImage = Bitmap.createScaledBitmap(currentPreviewImage, thumbSize.getWidth(), thumbSize.getHeight(), false);
            }
        } else {
            this.callbackContext.error("something wrong while passing isBase64Image value");
        }
    }

    private Bitmap getThumbnailImage() {
        MySize thumbSize = new MySize(Math.round(currentPreviewImage.getWidth() * 0.2f), Math.round(currentPreviewImage.getHeight() * 0.2f));
        return Bitmap.createScaledBitmap(currentPreviewImage, thumbSize.getWidth(), thumbSize.getHeight(), false);
    }

    private void applyEffect(String pathOrData, final String filterType, final double compressQuality, int isBase64Image, CallbackContext callbackContext) {
        synchronized (this) {
            this.validateInput(pathOrData, filterType, isBase64Image);

            Bitmap bmp = null;
            GPUImage editingGPUImage = new GPUImage(context);
            editingGPUImage.setImage(currentEditingImage);

            if (filterType.equals("aged"))
                bmp = applyAgedEffect(editingGPUImage);
            else if (filterType.equals("blackWhite"))
                bmp = applyBlackWhiteEffect(editingGPUImage);
            else if (filterType.equals("cold"))
                bmp = applyColdEffect(editingGPUImage);
            else if (filterType.equals("rosy"))
                bmp = applyRosyEffect(editingGPUImage);
            else if (filterType.equals("intense"))
                bmp = applyIntenseEffect(editingGPUImage);
            else if (filterType.equals("warm"))
                bmp = applyWarmEffect(editingGPUImage);
            else if (filterType.equals("light"))
                bmp = applyLightEffect(editingGPUImage);

            processPicture(bmp, (float) compressQuality, JPEG, callbackContext);
        }
    }

    private void applyEffectForReview(String pathOrData, final String filterType, final double compressQuality, int isBase64Image, CallbackContext callbackContext) {
        synchronized (this) {
            this.validateInput(pathOrData, filterType, isBase64Image);

            Bitmap bmp = null;
            GPUImage previewGPUImage = new GPUImage(context);
            previewGPUImage.setImage(currentPreviewImage);

            if (filterType.equals("aged"))
                bmp = applyAgedEffect(previewGPUImage);
            else if (filterType.equals("blackWhite"))
                bmp = applyBlackWhiteEffect(previewGPUImage);
            else if (filterType.equals("cold"))
                bmp = applyColdEffect(previewGPUImage);
            else if (filterType.equals("rosy"))
                bmp = applyRosyEffect(previewGPUImage);
            else if (filterType.equals("intense"))
                bmp = applyIntenseEffect(previewGPUImage);
            else if (filterType.equals("warm"))
                bmp = applyWarmEffect(previewGPUImage);
            else if (filterType.equals("light"))
                bmp = applyLightEffect(previewGPUImage);

            processPicture(bmp, (float) compressQuality, JPEG, callbackContext);
        }
    }

    private void applyEffectForThumbnail(final String pathOrData, final String filterType, final double compressQuality, final int isBase64Image, final CallbackContext callbackContext) {
        validateInput(pathOrData, filterType, isBase64Image);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap thumb = getThumbnailImage();
                Bitmap bmp = null;
                GPUImage thumbnailGPUImage = new GPUImage(context);
                thumbnailGPUImage.setImage(thumb);

                if (filterType.equals("aged"))
                    bmp = applyAgedEffect(thumbnailGPUImage);
                else if (filterType.equals("blackWhite"))
                    bmp = applyBlackWhiteEffect(thumbnailGPUImage);
                else if (filterType.equals("cold"))
                    bmp = applyColdEffect(thumbnailGPUImage);
                else if (filterType.equals("rosy"))
                    bmp = applyRosyEffect(thumbnailGPUImage);
                else if (filterType.equals("intense"))
                    bmp = applyIntenseEffect(thumbnailGPUImage);
                else if (filterType.equals("warm"))
                    bmp = applyWarmEffect(thumbnailGPUImage);
                else if (filterType.equals("light"))
                    bmp = applyLightEffect(thumbnailGPUImage);

                processPicture(bmp, (float) compressQuality, JPEG, callbackContext);
            }
        }).start();
    }

    private Bitmap applyAgedEffect(GPUImage img) {
        return this.applyFilter(img, 0.00516f, 0.04124f, 0.8763f, 0.7474f, 0.1804f, 1.0103f,
                NOT_AVAILABLE, 0.7835f, 0.719f, 0.616f);
    }

    private Bitmap applyBlackWhiteEffect(GPUImage img) {
        return this.applyFilter(img, 0.0f, NOT_AVAILABLE, NOT_AVAILABLE, 1.2282f, 0.2062f, 0.268f,
                NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE);
    }

    private Bitmap applyRosyEffect(GPUImage img) {
        return this.applyFilter(img, 0.79897f, -0.164948f, 0.819588f, 0.881443f, 0.474227f, NOT_AVAILABLE,
                NOT_AVAILABLE, NOT_AVAILABLE, 0.822165f, 0.876289f);
    }

    private Bitmap applyIntenseEffect(GPUImage img) {
        return this.applyFilter(img, 2f, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, 0.12371f,
                NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE);
    }

    private Bitmap applyWarmEffect(GPUImage img) {
        return this.applyFilter(img, 1.2577f, -0.085f, 0.964f, 0.8763f, 0.4536f,
                NOT_AVAILABLE, NOT_AVAILABLE, 0.83f, 0.8092f, 0.7938f);
    }

    private Bitmap applyLightEffect(GPUImage img) {
        return this.applyFilter(img, 1.4484f, -0.0592f, 0.7629f, 0.7835f, 0.4124f, -0.0825f,
                NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE);
    }

    private Bitmap applyColdEffect(GPUImage img) {
        return this.applyFilter(img, 0.216495f, -0.134021f, 0.85567f, 1.061856f, 0.603093f, NOT_AVAILABLE,
                NOT_AVAILABLE, 0.708763f, 0.832474f, NOT_AVAILABLE);
    }

    private Bitmap applyFilter(GPUImage img, float saturation, float brightness, float contrast, float gamma, float exposure, float sharpen, float hue,
                               float red, float green, float blue) {

        GPUImageFilterGroup group = new GPUImageFilterGroup();

        if (saturation != NOT_AVAILABLE) {
            GPUImageSaturationFilter saturationFilter = new GPUImageSaturationFilter();
            saturationFilter.setSaturation(saturation);
            group.addFilter(saturationFilter);
        }
        if (brightness != NOT_AVAILABLE) {
            GPUImageBrightnessFilter brightnessFilter = new GPUImageBrightnessFilter();
            brightnessFilter.setBrightness(brightness);
            group.addFilter(brightnessFilter);
        }
        if (contrast != NOT_AVAILABLE) {
            GPUImageContrastFilter contrastFilter = new GPUImageContrastFilter();
            contrastFilter.setContrast(contrast);
            group.addFilter(contrastFilter);
        }
        if (gamma != NOT_AVAILABLE) {
            GPUImageGammaFilter gammaFilter = new GPUImageGammaFilter();
            gammaFilter.setGamma(gamma);
            group.addFilter(gammaFilter);
        }
        if (exposure != NOT_AVAILABLE) {
            GPUImageExposureFilter exposureFilter = new GPUImageExposureFilter();
            exposureFilter.setExposure(exposure);
            group.addFilter(exposureFilter);
        }
        if (sharpen != NOT_AVAILABLE) {
            GPUImageSharpenFilter sharpenFilter = new GPUImageSharpenFilter();
            sharpenFilter.setSharpness(sharpen);
            group.addFilter(sharpenFilter);
        }
        if (hue != NOT_AVAILABLE) {
            GPUImageHueFilter hueFilter = new GPUImageHueFilter();
            hueFilter.setHue(hue);
            group.addFilter(hueFilter);
        }
        if (red != NOT_AVAILABLE || green != NOT_AVAILABLE || blue != NOT_AVAILABLE) {
            GPUImageRGBFilter rgbFilter = new GPUImageRGBFilter();
            if (red != NOT_AVAILABLE)
                rgbFilter.setRed(red);
            if (green != NOT_AVAILABLE)
                rgbFilter.setGreen(green);
            if (blue != NOT_AVAILABLE)
                rgbFilter.setBlue(blue);
            group.addFilter(rgbFilter);
        }

        img.setFilter(group);
        return img.getBitmapWithFilterApplied();
    }

    public void failPicture(String err) {
        this.callbackContext.error(err);
    }

    private void processPicture(Bitmap bitmap, float compressQuality, int encodingType, CallbackContext callbackContext) {
        synchronized (this) {
            ByteArrayOutputStream jpeg_data = new ByteArrayOutputStream();
            CompressFormat compressFormat = encodingType == JPEG ?
                    CompressFormat.JPEG :
                    CompressFormat.PNG;

            try {
                if (bitmap.compress(compressFormat, Math.round(0.8f * 100), jpeg_data)) {
                    byte[] code = jpeg_data.toByteArray();
                    byte[] output = Base64.encode(code, Base64.NO_WRAP);
                    String js_out = new String(output);
                    callbackContext.success(js_out);
                    js_out = null;
                    output = null;
                    code = null;
                }
            } catch (Exception e) {
                this.failPicture("Error compressing image.");
            }
            jpeg_data = null;
        }
    }

    private Bitmap base64ToBitmap(String encodedImage) {
        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    }
}
