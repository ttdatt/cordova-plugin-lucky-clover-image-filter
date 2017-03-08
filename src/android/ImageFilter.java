
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
    private static Bitmap currentPreviewImage;
    private static Bitmap currentEditingImage;
    private static GPUImage editingGPUImage;
    private GLSurfaceView glSurfaceView;
    private static Size screenSize;

    private static Context context;

    public CallbackContext callbackContext;

    @Override
    protected void pluginInitialize() {
        super.pluginInitialize();

        context = this.cordova.getActivity().getApplicationContext();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        this.cordova.getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;
        screenSize = new Size(width, height);

        glSurfaceView = new GLSurfaceView(context);
        editingGPUImage = new GPUImage(context);
        editingGPUImage.setGLSurfaceView(glSurfaceView);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        this.callbackContext = callbackContext;

        if (action.equals("coolMethod")) {
            String message = args.getString(0);
//            this.coolMethod(message, callbackContext);
            return true;
        }
        if (action.equals("applyEffect")) {
            String path = args.getString(0);
            String filterType = args.getString(1);
            double compressQuality = args.getDouble(2);
            this.applyEffect(path, filterType, compressQuality, callbackContext);
        }
        return false;
    }

    private void validateInput(String path, String filterType) {

        if (!StringUtils.isEmpty(path) && !StringUtils.isEmpty(filterType)
                && !currentImagePath.equals(path)) {
            currentImagePath = path;
            currentEditingImage = BitmapFactory.decodeFile(path);
            editingGPUImage.setImage(currentEditingImage);

            float ratio = screenSize.getWidth() / currentEditingImage.getWidth();
            Size newSize = new Size(Math.round(currentEditingImage.getWidth() * ratio), Math.round(currentEditingImage.getHeight() * ratio));
            currentPreviewImage = Bitmap.createScaledBitmap(currentEditingImage, newSize.getWidth(), newSize.getHeight(), false);
        }
    }

    private void applyEffect(String path, String filterType, double compressQuality, CallbackContext callbackContext) {
        this.validateInput(path, filterType);

        Bitmap bmp = this.applyAgedEffect();
        this.processPicture(bmp, (float)compressQuality, JPEG);
    }

    private void applyEffectForReview(String path, String filterType, double compressQuality, CallbackContext callbackContext) {
        this.validateInput(path, filterType);

        Bitmap bmp = this.applyAgedEffect();
        this.processPicture(bmp, (float)compressQuality, JPEG);
    }

    private Bitmap applyAgedEffect() {
        GPUImageSaturationFilter saturationFilter = new GPUImageSaturationFilter(1.0f);
        saturationFilter.setSaturation(0.4072f);
        GPUImageBrightnessFilter brightnessFilter = new GPUImageBrightnessFilter();
        brightnessFilter.setBrightness(-0.1495f);
        GPUImageContrastFilter contrastFilter = new GPUImageContrastFilter();
        contrastFilter.setContrast(0.768f);
        GPUImageExposureFilter exposureFilter = new GPUImageExposureFilter();
        exposureFilter.setExposure(0.8866f);
        GPUImageRGBFilter rgbFilter = new GPUImageRGBFilter();
        rgbFilter.setRed(0.8222165f);
        rgbFilter.setGreen(0.6469f);
        rgbFilter.setBlue(0.5232f);

        GPUImageFilterGroup filterGroup = new GPUImageFilterGroup();
        filterGroup.addFilter(saturationFilter);
        filterGroup.addFilter(brightnessFilter);
        filterGroup.addFilter(contrastFilter);
        filterGroup.addFilter(exposureFilter);
        filterGroup.addFilter(rgbFilter);

        editingGPUImage.setFilter(filterGroup);
        return editingGPUImage.getBitmapWithFilterApplied();
    }

    public void failPicture(String err) {
        this.callbackContext.error(err);
    }

    public void processPicture(Bitmap bitmap, float compressQuality, int encodingType) {
        ByteArrayOutputStream jpeg_data = new ByteArrayOutputStream();
        CompressFormat compressFormat = encodingType == JPEG ?
                CompressFormat.JPEG :
                CompressFormat.PNG;

        try {
            if (bitmap.compress(compressFormat, Math.round(compressQuality * 100), jpeg_data)) {
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
}
