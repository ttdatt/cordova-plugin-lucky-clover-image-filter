package cordova.plugin.sts.image.filter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
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
import android.util.Size;


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

public class ImageFilter extends CordovaPlugin {

    private static final int JPEG = 0;                  // Take a picture of type JPEG
    private static final int PNG = 1;                   // Take a picture of type PNG

    private static String currentImagePath;
    private static String base64Image;
    private static Bitmap currentPreviewImage;
    private static Bitmap currentEditingImage;
    private static GPUImage editingGPUImage;
    private static GPUImage previewGPUImage;

    private GLSurfaceView glSurfaceView;
    private static Size screenSize;

    private static Context context;

    public CallbackContext callbackContext;

    @Override
    protected void pluginInitialize() {
        super.pluginInitialize();

        context = this.cordova.getActivity();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        this.cordova.getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;
        screenSize = new Size(width, height);

        glSurfaceView = new GLSurfaceView(context);
        editingGPUImage = new GPUImage(context);
        previewGPUImage = new GPUImage(context);
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
        if (action.equals("applyEffect") || action.equals("applyEffectForReview")) {
            String path = args.getString(0);
            String filterType = args.getString(1);
            double compressQuality = args.getDouble(2);
            boolean isBase64Image = args.getBoolean(3);

            if (action.equals("applyEffect"))
                this.applyEffect(path, filterType, compressQuality, isBase64Image, callbackContext);
            else if (action.equals("applyEffectForReview"))
                this.applyEffectForReview(path, filterType, compressQuality, isBase64Image, callbackContext);
        }
        return false;
    }

    private void validateInput(String pathOrData, String filterType, boolean isBase64Image) {

        if (!isBase64Image) {
            if (!StringUtils.isEmpty(pathOrData) && !StringUtils.isEmpty(filterType)
                    && !pathOrData.equals(currentImagePath)) {

                pathOrData = pathOrData.substring(7);
                currentImagePath = pathOrData;

                currentEditingImage = BitmapFactory.decodeFile(pathOrData);
                editingGPUImage.setImage(currentEditingImage);

                float ratio = screenSize.getWidth() / currentEditingImage.getWidth();
                Size newSize = new Size(Math.round(currentEditingImage.getWidth() * ratio), Math.round(currentEditingImage.getHeight() * ratio));
                currentPreviewImage = Bitmap.createScaledBitmap(currentEditingImage, newSize.getWidth(), newSize.getHeight(), false);
                previewGPUImage.setImage(currentPreviewImage);
            }
        } else {
            if (!StringUtils.isEmpty(pathOrData) && !StringUtils.isEmpty(filterType)
                    && !pathOrData.equals(base64Image)) {
                base64Image = pathOrData;

                currentEditingImage = base64ToBitmap(pathOrData);
                editingGPUImage.setImage(currentEditingImage);

                float ratio = screenSize.getWidth() / currentEditingImage.getWidth();
                Size newSize = new Size(Math.round(currentEditingImage.getWidth() * ratio), Math.round(currentEditingImage.getHeight() * ratio));
                currentPreviewImage = Bitmap.createScaledBitmap(currentEditingImage, newSize.getWidth(), newSize.getHeight(), false);
                previewGPUImage.setImage(currentPreviewImage);
            }
        }
    }

    private void applyEffect(String pathOrData, final String filterType, final double compressQuality, boolean isBase64Image, CallbackContext callbackContext) {
        this.validateInput(pathOrData, filterType, isBase64Image);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap bmp = null;
                if (filterType.equals("aged"))
                    bmp = applyAgedEffect(editingGPUImage);

                processPicture(bmp, (float) compressQuality, JPEG);
            }
        }).start();
    }

    private void applyEffectForReview(String pathOrData, final String filterType, final double compressQuality, boolean isBase64Image, CallbackContext callbackContext) {
        this.validateInput(pathOrData, filterType, isBase64Image);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap bmp = null;
                if (filterType.equals("aged"))
                    bmp = applyAgedEffect(previewGPUImage);

                processPicture(bmp, (float) compressQuality, JPEG);
            }
        }).start();
    }

    private Bitmap applyAgedEffect(GPUImage bmp) {
        GPUImageSaturationFilter saturationFilter = new GPUImageSaturationFilter();
        saturationFilter.setSaturation(1.26288f);
        GPUImageGammaFilter gammaFilter = new GPUImageGammaFilter();
        gammaFilter.setGamma(1.14433f);
        GPUImageContrastFilter contrastFilter = new GPUImageContrastFilter();
        contrastFilter.setContrast(1.2577f);
        GPUImageExposureFilter exposureFilter = new GPUImageExposureFilter();
        exposureFilter.setExposure(0.4381f);
        GPUImageHueFilter hueFilter = new GPUImageHueFilter();
        hueFilter.setHue(0.2758f);
        GPUImageSharpenFilter sharpen = new GPUImageSharpenFilter();
        sharpen.setSharpness(-0.8453f);

        GPUImageFilterGroup filterGroup = new GPUImageFilterGroup();
        filterGroup.addFilter(saturationFilter);
        filterGroup.addFilter(gammaFilter);
        filterGroup.addFilter(contrastFilter);
        filterGroup.addFilter(exposureFilter);
        filterGroup.addFilter(hueFilter);
        filterGroup.addFilter(sharpen);

        bmp.setFilter(filterGroup);
        return bmp.getBitmapWithFilterApplied();
    }

    public void failPicture(String err) {
        this.callbackContext.error(err);
    }

    private void processPicture(Bitmap bitmap, float compressQuality, int encodingType) {
        ByteArrayOutputStream jpeg_data = new ByteArrayOutputStream();
        CompressFormat compressFormat = encodingType == JPEG ?
                CompressFormat.JPEG :
                CompressFormat.PNG;

        try {
            if (bitmap.compress(compressFormat, Math.round(0.8f * 100), jpeg_data)) {
                byte[] code = jpeg_data.toByteArray();
                byte[] output = Base64.encode(code, Base64.NO_WRAP);
                String js_out = new String(output);
                this.callbackContext.success(js_out);
                js_out = null;
                output = null;
                code = null;
            }
        } catch (Exception e) {
            this.failPicture("Error compressing image.");
        }
        jpeg_data = null;
    }

    private Bitmap base64ToBitmap(String encodedImage) {
        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    }
}
