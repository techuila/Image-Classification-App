package com.example.ibato.camera;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ExifInterface;
import android.media.Image;
import android.media.ImageReader;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.renderscript.Sampler;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.ibato.LoginActivity;
import com.example.ibato.MainActivity;
import com.example.ibato.R;
import com.example.ibato.interfaces.ICallback;
import com.example.ibato.interfaces.IMainActivity;
import com.example.ibato.models.Model;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;


import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static com.example.ibato.Utils.Utils.getDatabase;

public class Camera2Fragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "Camera2Fragment";
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String FRAGMENT_DIALOG = "dialog";

    /** Time it takes for icons to fade (in milliseconds) */
    private static final int ICON_FADE_DURATION  = 400;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /** The current state of camera state for taking pictures.
     * @see #mCaptureCallback */
    private int mState = STATE_PREVIEW;

    /** Camera state: Showing camera preview. */
    private static final int STATE_PREVIEW = 0;

    /** Camera state: Waiting for the focus to be locked. */
    private static final int STATE_WAITING_LOCK = 1;

    /** Camera state: Waiting for the exposure to be precapture state. */
    private static final int STATE_WAITING_PRECAPTURE = 2;

    /** Camera state: Waiting for the exposure state to be something other than precapture. */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    /** Camera state: Picture was taken. */
    private static final int STATE_PICTURE_TAKEN = 4;

    /** States for the flash */
    private static final int FLASH_STATE_OFF = 0;
    private static final int FLASH_STATE_ON = 1;
    private static final int FLASH_STATE_AUTO = 2;


    //vars
    /** A {@link Semaphore} to prevent the app from exiting before closing the camera. */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /** A {@link CameraCaptureSession } for camera preview. */
    private CameraCaptureSession mCaptureSession;

    /** A reference to the opened {@link CameraDevice}. */
    private CameraDevice mCameraDevice;

    /** ID of the current {@link CameraDevice}. */
    private String mCameraId;

    /** The {@link android.util.Size} of camera preview. */
    private Size mPreviewSize;

    /** Orientation of the camera sensor */
    private int mSensorOrientation;

    /** An {@link ScalingTextureView} for camera preview. */
    private ScalingTextureView mTextureView;

    /** {@link CaptureRequest.Builder} for the camera preview */
    private CaptureRequest.Builder mPreviewRequestBuilder;

    /** {@link CaptureRequest} generated by {@link #mPreviewRequestBuilder} */
    private CaptureRequest mPreviewRequest;

    /** An additional thread for running tasks that shouldn't block the UI. */
    private HandlerThread mBackgroundThread;

    /** A {@link Handler} for running tasks in the background. */
    private Handler mBackgroundHandler;

    /**
     * An {@link ImageReader} that handles still image capture.
     */
    private ImageReader mImageReader;

    /** Max preview width that is guaranteed by Camera2 API */
    private int MAX_PREVIEW_WIDTH = 1920;

    /** Max preview height that is guaranteed by Camera2 API */
    private int MAX_PREVIEW_HEIGHT = 1080;

    private int SCREEN_WIDTH = 0;

    private int SCREEN_HEIGHT = 0;

    private float ASPECT_RATIO_ERROR_RANGE = 0.1f;

    private Image mCapturedImage;

    private boolean mIsImageAvailable = false;

    private IMainActivity mIMainActivity;

    private Bitmap mCapturedBitmap;

    private Uri mCapturedUri;

    private BackgroundImageRotater mBackgroundImageRotater;

    private boolean mIsDrawingEnabled = false;

    boolean mIsCurrentlyDrawing = false;

    private int mFlashState = 0;

    private boolean mFlashSupported;

    private String imageName = "";

    private Boolean mAutoFocusSupported = false;


    // presets for rgb conversion
    private static final int RESULTS_TO_SHOW = 3;
    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128.0f;

    // input image dimensions for the Inception Model
    private int DIM_IMG_SIZE_X = 224;
    private int DIM_IMG_SIZE_Y = 224;
    private int DIM_PIXEL_SIZE = 3;

    // holds the selected image data as bytes
    private ByteBuffer imgData = null;
    // options for model interpreter
    private final Interpreter.Options tfliteOptions = new Interpreter.Options();
    // tflite graph
    private Interpreter tflite;
    // holds all the possible labels for model
    private List<String> labelList;
    private float[][] labelProbArray = null;
    // array that holds the labels with the highest probabilities
    private String[] topLables = null;
    // array that holds the highest probabilities
    private String[] topConfidence = null;
    // int array to hold image data
    private int[] intValues;
    // model location
    private String chosen = "model.tflite";
    private Boolean isEdible = false;

    // priority queue that will hold the top results from the CNN
    private PriorityQueue<Map.Entry<String, Float>> sortedLabels =
            new PriorityQueue<>(
                    RESULTS_TO_SHOW,
                    new Comparator<Map.Entry<String, Float>>() {
                        @Override
                        public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
                            return (o1.getValue()).compareTo(o2.getValue());
                        }
                    });

    //widgets
    private RelativeLayout mStillshotContainer, mFlashContainer, mSwitchOrientationContainer, mMenuContainer,
            mCaptureBtnContainer, mCloseStillshotContainer, mSaveContainer, mCardContent;
    private DrawableImageView mStillshotImageView;
    private ImageButton mFlashIcon, mInfoButton;
    private FloatingActionButton mMainButton;
    private CardView mCardView;
    private TextView mVegName, mDescr;
    private ProgressBar progressBar;
    private ImageView mStatus;

    private StorageReference mStorageRef;
    private DatabaseReference mDatabaseRef, mVegRef;

    public static Camera2Fragment newInstance(){
        return new Camera2Fragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera2, container, false);


        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated: created view.");
        View mMainActivityView = getActivity().findViewById(R.id.content_main);
        mMainButton = mMainActivityView.findViewById(R.id.main_button);
//
//        mMainActivityView.findViewById(R.id.main_button).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (mIMainActivity.getFragment().getTag() == getString(R.string.fragment_camera2)) {
//                    if(!mIsImageAvailable){
//                        Log.d(TAG, "onClick: taking picture.");
//                        takePicture();
//                    } else {
//                        saveCapturedStillshotToDisk();
//                    }
//                } else {
//                    mIMainActivity.startCamera2();
//                }
//            }
//        });

//        view.findViewById(R.id.save_stillshot).setOnClickListener(this);
//        view.findViewById(R.id.switch_orientation).setOnClickListener(this);

        mCardView = view.findViewById(R.id.classify_card);
        mVegName = view.findViewById(R.id.veg_name_txt);
        mDescr = view.findViewById(R.id.desc_txt);
        mStatus = view.findViewById(R.id.status_img);

        mInfoButton = view.findViewById(R.id.info_button);
        mFlashIcon = view.findViewById(R.id.flash_toggle);
        mFlashContainer = view.findViewById(R.id.flash_container);
//        mMenuContainer = view.findViewById(R.id.menu_container);
//        mSaveContainer = view.findViewById(R.id.save_container);
        mCloseStillshotContainer = view.findViewById(R.id.close_stillshot_view);
        mStillshotImageView = view.findViewById(R.id.stillshot_imageview);
        mStillshotContainer = view.findViewById(R.id.stillshot_container);
        mFlashContainer = view.findViewById(R.id.flash_container);
        mCardContent = view.findViewById(R.id.card_content);
        progressBar = view.findViewById(R.id.loading);
//        mSwitchOrientationContainer = view.findViewById(R.id.switch_orientation_container);
//        mCaptureBtnContainer = view.findViewById(R.id.capture_button_container);
        mTextureView = view.findViewById(R.id.texture);

        mInfoButton.setOnClickListener(this);
        mFlashIcon.setOnClickListener(this);
//        mMenuContainer.setOnClickListener(this);
        mCloseStillshotContainer.setOnClickListener(this);

        /* Firebase Initializations */
        mStorageRef = FirebaseStorage.getInstance().getReference("uploads");
        mDatabaseRef = getDatabase().getReference("uploads");
        mVegRef = getDatabase().getReference("vegetables");

        setMaxSizes();
        resetIconVisibilities();
    }

    private void initializeTflite() {
        // initialize array that holds image data
        intValues = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];

        //initilize graph and labels
        try{
            tflite = new Interpreter(loadModelFile(), tfliteOptions);
            labelList = loadLabelList();
        } catch (Exception ex){
            ex.printStackTrace();
        }

        imgData = ByteBuffer.allocateDirect(4 * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE);
        imgData.order(ByteOrder.nativeOrder());

        // initialize probabilities array. The datatypes that array holds depends if the input data needs to be quantized or not
        labelProbArray = new float[1][labelList.size()];

        // initialize array to hold top labels
        topLables = new String[RESULTS_TO_SHOW];
        // initialize array to hold top probabilities
        topConfidence = new String[RESULTS_TO_SHOW];
    }

    // loads tflite grapg from file
    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = getContext().getAssets().openFd(chosen);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    // loads the labels from the label txt file in assets into a string array
    private List<String> loadLabelList() throws IOException {
        List<String> labelList = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(getContext().getAssets().open("labels.txt")));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    public void captureTriggered() {
        takePicture();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
//            case R.id.main_button: {
//                if(!mIsImageAvailable){
//                    Log.d(TAG, "onClick: taking picture.");
//                    takePicture();
//                } else {
//                    saveCapturedStillshotToDisk();
//                }
//                break;
//            }

//            case R.id.switch_orientation: {
//                Log.d(TAG, "onClick: switching camera orientation.");
//                toggleCameraDisplayOrientation();
//                break;
//            }

            case R.id.close_stillshot_view: {
                hideStillshotContainer();
                break;
            }

            case R.id.info_button: {
                mIMainActivity.showTutorial();
                break;
            }
//
//            case R.id.save_stillshot: {
//                saveCapturedStillshotToDisk();
//                break;
//            }

//            case R.id.menu_container: {
//                mIMainActivity.openDrawer(false);
//                break;
//            }

            case R.id.flash_toggle: {
                if(!mIsImageAvailable){
                    toggleFlashState();
                }
                break;
            }
        }
    }

    private void toggleFlashState(){
        if(mFlashState == FLASH_STATE_OFF){
            mFlashState = FLASH_STATE_ON;
        }
        else if(mFlashState == FLASH_STATE_ON){
            mFlashState = FLASH_STATE_AUTO;
        }
        else if(mFlashState == FLASH_STATE_AUTO){
            mFlashState = FLASH_STATE_OFF;
        }
        setFlashIcon();
    }

    private void setFlashIcon(){
        if(mFlashState == FLASH_STATE_OFF){
            Glide.with(getActivity())
                    .load(R.drawable.ic_flash_off)
                    .into(mFlashIcon);
        }
        else if(mFlashState == FLASH_STATE_ON){
            Glide.with(getActivity())
                    .load(R.drawable.ic_flash_on)
                    .into(mFlashIcon);
        }
        else if(mFlashState == FLASH_STATE_AUTO){
            Glide.with(getActivity())
                    .load(R.drawable.ic_flash_auto)
                    .into(mFlashIcon);
        }
        setAutoFlash(mPreviewRequestBuilder);
    }

    private void hideStillshotContainer(){
        mIMainActivity.showStatusBar();
        if(mIsImageAvailable){
            mMainButton.setImageResource(R.drawable.capture2);

            mIsImageAvailable = false;
            mCapturedBitmap = null;
            mStillshotImageView.setImageBitmap(null);

            mIsDrawingEnabled = false;
            mStillshotImageView.reset();
            mStillshotImageView.setImageBitmap(null);

            resetIconVisibilities();

            mTextureView.resetScale();

            reopenCamera();
        }
    }


    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (mFlashSupported) {
            if(mFlashState == FLASH_STATE_OFF){
                requestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF);
            }
            else if(mFlashState == FLASH_STATE_ON){
                requestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_SINGLE);
            }
            else if(mFlashState == FLASH_STATE_AUTO){
                requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            }
        }
    }


    public void dragStickerStarted(){
        if(mStillshotImageView.mSelectedStickerIndex != -1){
//            mSaveContainer.animate().alpha(0.0f).setDuration(ICON_FADE_DURATION);
            mCloseStillshotContainer.animate().alpha(0.0f).setDuration(ICON_FADE_DURATION);

            // show the trash can container
        }
    }

    public void dragStickerStopped(){
        if(mStillshotImageView.mSelectedStickerIndex == -1){
//            mSaveContainer.animate().alpha(1.0f).setDuration(0);
            mCloseStillshotContainer.animate().alpha(1.0f).setDuration(0);

            // hide the trash can container
        }
    }

    private void saveCapturedStillshotToDisk(){
        if(mIsImageAvailable){
            Log.d(TAG, "saveCapturedStillshotToDisk: saving image to disk.");

            final ICallback callback = new ICallback() {
                @Override
                public void done(Exception e) {
                    if(e == null){
                        Log.d(TAG, "onImageSavedCallback: image saved!");
//                        showSnackBar("Image saved", Snackbar.LENGTH_SHORT);
                    }
                    else{
                        Log.d(TAG, "onImageSavedCallback: error saving image: " + e.getMessage());
                        showSnackBar("Error saving image", Snackbar.LENGTH_SHORT);
                    }
                }
            };

            if(mCapturedImage != null){
                Log.d(TAG, "saveCapturedStillshotToDisk: saving to disk.");

                mStillshotImageView.invalidate();
                Bitmap bitmap = Bitmap.createBitmap(mStillshotImageView.getDrawingCache());

                ImageSaver imageSaver = new ImageSaver(
                        bitmap,
                        getActivity().getExternalFilesDir(null),
                        callback
                );

                mBackgroundHandler.post(imageSaver);
                uploadFile();
            }
        }
    }

    public static boolean isNetworkAvailable(Context con) {
        try {
            ConnectivityManager cm = (ConnectivityManager) con
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();

            if (networkInfo != null && networkInfo.isConnected()) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void uploadFile() {
        mIMainActivity.showProgressDialog(true);

        if (isNetworkAvailable(this.getContext())) {
            final StorageReference image = mStorageRef.child(mCapturedUri.getLastPathSegment());

            image.putFile(mCapturedUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(final UploadTask.TaskSnapshot taskSnapshot) {
                    image.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Log.d("tag", "onSuccess: Uploaded Image URl is " + uri.toString());

                            Model model = new Model(mIMainActivity.getUserID(), uri.toString(), topLables[2], "Carrots can be eaten something chu chu", isEdible);
                            String uploadId = mDatabaseRef.push().getKey();
                            mDatabaseRef.child(mIMainActivity.getUserID()).child(uploadId).setValue(model);

                            mIMainActivity.showProgressDialog(false);
                            hideStillshotContainer();
                        }
                    });

    //                showSnackBar("Image Is Uploaded.", Snackbar.LENGTH_SHORT);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    mIMainActivity.showProgressDialog(false);
                    showSnackBar("Upload Failled.", Snackbar.LENGTH_SHORT);
                }
            });
        } else {
            showSnackBar("You are offline, please try again later.", Snackbar.LENGTH_LONG);
            mIMainActivity.showProgressDialog(false);
        }
    }

    private void resetIconVisibilities(){
        mStillshotContainer.setVisibility(View.INVISIBLE);

        mFlashContainer.setVisibility(View.VISIBLE);
//        mMenuContainer.setVisibility(View.VISIBLE);
//        mSwitchOrientationContainer.setVisibility(View.VISIBLE);
//        mCaptureBtnContainer.setVisibility(View.VISIBLE);

    }

    /**
     * Initiate a still image capture.
     */
    private void takePicture()  {
            lockFocus();
    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
    private void lockFocus() {
        try {
            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);

            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK;

            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
//            captureStillPicture();

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            Log.d(TAG, "onSurfaceTextureAvailable: w: " + width + ", h: " + height);
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            Log.d(TAG, "onSurfaceTextureSizeChanged: w: " + width + ", h: " + height);
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            closeCamera();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };


    public void reopenCamera() {
        Log.d(TAG, "reopenCamera: called.");
        if (mTextureView.isAvailable()) {
            Log.d(TAG, "reopenCamera: a surface is available.");
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            Log.d(TAG, "reopenCamera: no surface is available.");
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }

    }

    private void openCamera(int width, int height) {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }

        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            switch (mState) {
                case STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is working normally.
                    break;
                }
                case STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    else if(afState == CaptureResult.CONTROL_AF_STATE_INACTIVE){
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
        }

    };

    /**
     * Run the precapture sequence for capturing a still image. This method should be called when
     * we get a response in {@link #mCaptureCallback} from {@link #lockFocus()}.
     */
    private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Capture a still picture. This method should be called when we get a response in
     * {@link #mCaptureCallback} from both {@link #lockFocus()}.
     */
    private void captureStillPicture() {
        Log.d(TAG, "captureStillPicture: capturing picture.");
        try {

            final Activity activity = getActivity();
            if (null == activity || null == mCameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            setAutoFlash(captureBuilder);

            // Orientation
            // Rotate the image from screen orientation to image orientation
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    unlockFocus();
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.d(TAG, "onImageAvailable: called.");

            if(!mIsImageAvailable){
                mCapturedImage = reader.acquireLatestImage();

                Log.d(TAG, "onImageAvailable: captured image width: " + mCapturedImage.getWidth());
                Log.d(TAG, "onImageAvailable: captured image height: " + mCapturedImage.getHeight());

                saveTempImageToStorage();

                final Activity activity = getActivity();
                if(activity != null){
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            Glide.with(activity)
                                    .load(mCapturedImage)
                                    .into(mStillshotImageView);

                            showStillshotContainer();
                        }
                    });
                }
            }

        }
    };

    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation The screen rotation.
     * @return The JPEG orientation (one of 0, 90, 270, and 360)
     */
    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    private void unlockFocus() {
        try {
            // Reset the auto-focus trigger
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            setAutoFlash(mPreviewRequestBuilder);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            Log.d(TAG, "onError: " + error);
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }
    };

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);


            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;

                            try {
                                // Auto focus should be continuous for camera preview.
                                // Most new-ish phones can auto focus
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                                // Flash is automatically enabled when necessary.
                                setAutoFlash(mPreviewRequestBuilder);

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            showSnackBar("Failed", Snackbar.LENGTH_LONG);
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /** Closes the current {@link CameraDevice}. */
    private void closeCamera() {

        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /** Starts a background thread and its {@link Handler}. */
    private void startBackgroundThread() {
        if(mBackgroundThread == null){
            Log.d(TAG, "startBackgroundThread: called.");
            mBackgroundThread = new HandlerThread("CameraBackground");
            mBackgroundThread.start();
            mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        }
    }

    /** Stops the background thread and its {@link Handler}. */
    private void stopBackgroundThread() {
        if(mBackgroundThread != null){
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume: called.");
        super.onResume();

        startBackgroundThread();

        if(mIsImageAvailable){
            mIMainActivity.hideStatusBar();
        }
        else{
            mIMainActivity.showStatusBar();

            // When the screen is turned off and turned back on, the SurfaceTexture is already
            // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
            // a camera and start preview from here (otherwise, we wait until the surface is ready in
            // the SurfaceTextureListener).
            reopenCamera();
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        if(mBackgroundImageRotater != null){
            mBackgroundImageRotater.cancel(true);
        }
        super.onPause();
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private void setUpCameraOutputs(int width, int height) {
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);

        try {
            Log.d(TAG, "setUpCameraOutputs: called.");
            if (!mIMainActivity.isCameraBackFacing() && !mIMainActivity.isCameraFrontFacing()) {
                Log.d(TAG, "setUpCameraOutputs: finding camera id's.");
                findCameraIds();
            }

            CameraCharacteristics characteristics
                    = manager.getCameraCharacteristics(mCameraId);

            Log.d(TAG, "setUpCameraOutputs: camera id: " + mCameraId);

            int[] afAvailableModes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);

            if (afAvailableModes.length == 0 || (afAvailableModes.length == 1
                    && afAvailableModes[0] == CameraMetadata.CONTROL_AF_MODE_OFF)) {
                mAutoFocusSupported = false;
            } else {
                mAutoFocusSupported = true;
            }


            StreamConfigurationMap map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;

            Size largest = null;
            float screenAspectRatio = (float)SCREEN_WIDTH / (float)SCREEN_HEIGHT;
            List<Size> sizes = new ArrayList<>();
            for( Size size : Arrays.asList(map.getOutputSizes(ImageFormat.JPEG))){

                float temp = (float)size.getWidth() / (float)size.getHeight();

                Log.d(TAG, "setUpCameraOutputs: temp: " + temp);
                Log.d(TAG, "setUpCameraOutputs: w: " + size.getWidth() + ", h: " + size.getHeight());

                if(temp > (screenAspectRatio - screenAspectRatio * ASPECT_RATIO_ERROR_RANGE )
                        && temp < (screenAspectRatio + screenAspectRatio * ASPECT_RATIO_ERROR_RANGE)){
                    sizes.add(size);
                    Log.d(TAG, "setUpCameraOutputs: found a valid size: w: " + size.getWidth() + ", h: " + size.getHeight());
                }

            }
            if(sizes.size() > 0){
                largest = Collections.max(
                        sizes,
                        new Utility.CompareSizesByArea());
                Log.d(TAG, "setUpCameraOutputs: largest width: " + largest.getWidth());
                Log.d(TAG, "setUpCameraOutputs: largest height: " + largest.getHeight());
            }

            // Find out if we need to swap dimension to get the preview size relative to sensor
            // coordinate.
            int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            //noinspection ConstantConditions
            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            boolean swappedDimensions = false;
            switch (displayRotation) {
                case Surface.ROTATION_0:
                case Surface.ROTATION_180:
                    if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                        swappedDimensions = true;
                    }
                    break;
                case Surface.ROTATION_90:
                case Surface.ROTATION_270:
                    if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                        swappedDimensions = true;
                    }
                    break;
                default:
                    Log.e(TAG, "Display rotation is invalid: " + displayRotation);
            }

            Point displaySize = new Point();
            activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
            int rotatedPreviewWidth = width;
            int rotatedPreviewHeight = height;
            int maxPreviewWidth = displaySize.x;
            int maxPreviewHeight = displaySize.y;

            if (swappedDimensions) {
                rotatedPreviewWidth = height;
                rotatedPreviewHeight = width;
                maxPreviewWidth = displaySize.y;
                maxPreviewHeight = displaySize.x;
            }

            if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                maxPreviewWidth = MAX_PREVIEW_WIDTH;
            }

            if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                maxPreviewHeight = MAX_PREVIEW_HEIGHT;
            }

            Log.d(TAG, "setUpCameraOutputs: max preview width: " + maxPreviewWidth);
            Log.d(TAG, "setUpCameraOutputs: max preview height: " + maxPreviewHeight);


            mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                    ImageFormat.JPEG, /*maxImages*/2);
            mImageReader.setOnImageAvailableListener(
                    mOnImageAvailableListener, mBackgroundHandler);


            mPreviewSize = Utility.chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                    maxPreviewHeight, largest);


            Log.d(TAG, "setUpCameraOutputs: preview width: " + mPreviewSize.getWidth());
            Log.d(TAG, "setUpCameraOutputs: preview height: " + mPreviewSize.getHeight());

            // We fit the aspect ratio of TextureView to the size of preview we picked.
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTextureView.setAspectRatio(
                        mPreviewSize.getWidth(), mPreviewSize.getHeight(), SCREEN_WIDTH, SCREEN_HEIGHT);
            } else {
                mTextureView.setAspectRatio(
                        mPreviewSize.getHeight(), mPreviewSize.getWidth(), SCREEN_HEIGHT, SCREEN_WIDTH);
            }


            Log.d(TAG, "setUpCameraOutputs: cameraId: " + mCameraId);

            // Check if the flash is supported.
            Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            mFlashSupported = available == null ? false : available;

        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance(getString(R.string.camera_error))
                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }

    }

    private void setMaxSizes(){
        Point displaySize = new Point();
        getActivity().getWindowManager().getDefaultDisplay().getSize(displaySize);
        SCREEN_HEIGHT = displaySize.x;
        SCREEN_WIDTH = displaySize.y;

        Log.d(TAG, "setMaxSizes: screen width:" + SCREEN_WIDTH);
        Log.d(TAG, "setMaxSizes: screen height: " + SCREEN_HEIGHT);
    }


    private void findCameraIds(){

        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);

        try {
            for (String cameraId : manager.getCameraIdList()) {
                Log.d(TAG, "findCameraIds: CAMERA ID: " + cameraId);
                if (cameraId == null) continue;
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                int facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing == CameraCharacteristics.LENS_FACING_FRONT){
                    mIMainActivity.setFrontCameraId(cameraId);
                }
                else if (facing == CameraCharacteristics.LENS_FACING_BACK){
                    mIMainActivity.setBackCameraId(cameraId);
                }
            }
            mIMainActivity.setCameraBackFacing();
            mCameraId = mIMainActivity.getBackCameraId();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void toggleCameraDisplayOrientation(){
        if(mCameraId.equals(mIMainActivity.getBackCameraId())){
            mCameraId = mIMainActivity.getFrontCameraId();
            mIMainActivity.setCameraFrontFacing();
            closeCamera();
            reopenCamera();
            Log.d(TAG, "toggleCameraDisplayOrientation: switching to front-facing camera.");
        }
        else if(mCameraId.equals(mIMainActivity.getFrontCameraId())){
            mCameraId = mIMainActivity.getBackCameraId();
            mIMainActivity.setCameraBackFacing();
            closeCamera();
            reopenCamera();
            Log.d(TAG, "toggleCameraDisplayOrientation: switching to back-facing camera.");
        }
        else{
            Log.d(TAG, "toggleCameraDisplayOrientation: error.");
        }
    }
    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        Log.d(TAG, "configureTransform: viewWidth: " + viewWidth + ", viewHeight: " + viewHeight);
        Log.d(TAG, "configureTransform: previewWidth: " + mPreviewSize.getWidth() + ", previewHeight: " + mPreviewSize.getHeight());
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            Log.d(TAG, "configureTransform: rotating from 90 or 270");
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            Log.d(TAG, "configureTransform: rotating 180.");
            matrix.postRotate(180, centerX, centerY);
        }


        float screenAspectRatio = (float)SCREEN_WIDTH / (float)SCREEN_HEIGHT;
        float previewAspectRatio = (float)mPreviewSize.getWidth() / (float)mPreviewSize.getHeight();
        String roundedScreenAspectRatio = String.format("%.2f", screenAspectRatio);
        String roundedPreviewAspectRatio = String.format("%.2f", previewAspectRatio);
        if(!roundedPreviewAspectRatio.equals(roundedScreenAspectRatio) ){

            float scaleFactor = (screenAspectRatio / previewAspectRatio);
            Log.d(TAG, "configureTransform: scale factor: " + scaleFactor);

            float heightCorrection = (((float)SCREEN_HEIGHT * scaleFactor) - (float)SCREEN_HEIGHT) / 2;

            matrix.postScale(scaleFactor, 1);
            matrix.postTranslate(-heightCorrection, 0);
        }

        mTextureView.setTransform(matrix);
    }




    private void requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    private void saveTempImageToStorage(){

        Log.d(TAG, "saveTempImageToStorage: saving temp image to disk.");
        final ICallback callback = new ICallback() {
            @Override
            public void done(Exception e) {
                if(e == null){
                    Log.d(TAG, "onImageSavedCallback: image saved!");

                    mBackgroundImageRotater = new BackgroundImageRotater(getActivity());
                    mBackgroundImageRotater.execute();
                    mIsImageAvailable = true;
                    mCapturedImage.close();

                }
                else{
                    Log.d(TAG, "onImageSavedCallback: error saving image: " + e.getMessage());
                    showSnackBar("Error displaying image", Snackbar.LENGTH_SHORT);
                }
            }
        };

        ImageSaver imageSaver = new ImageSaver(
                mCapturedImage,
                getActivity().getExternalFilesDir(null),
                callback
        );
        mBackgroundHandler.post(imageSaver);
    }

    private void displayCapturedImage(){
        Log.d(TAG, "displayCapturedImage: displaying stillshot image.");
        final Activity activity = getActivity();
        if(activity != null){
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    RequestOptions options = new RequestOptions()
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .centerCrop();

                    int bitmapWidth = mCapturedBitmap.getWidth();
                    int bitmapHeight = mCapturedBitmap.getHeight();

                    Log.d(TAG, "run: captured image width: " + bitmapWidth);
                    Log.d(TAG, "run: captured image height: " + bitmapHeight);


                    int focusX = (int)(mTextureView.mFocusX);
                    int focusY = (int)(mTextureView.mFocusY);
                    Log.d(TAG, "run: focusX: " + focusX);
                    Log.d(TAG, "run: focusY: " + focusY);


                    int maxWidth = mTextureView.getWidth();
                    int maxHeight = mTextureView.getHeight();
                    Log.d(TAG, "run: initial maxWidth: " + maxWidth);
                    Log.d(TAG, "run: initial maxHeight: " + maxHeight);

                    float bitmapHeightScaleFactor = (float)bitmapHeight / (float)maxHeight;
                    float bitmapWidthScaleFactor = (float)bitmapWidth / (float)maxWidth;
                    Log.d(TAG, "run: bitmap width scale factor: " + bitmapWidthScaleFactor);
                    Log.d(TAG, "run: bitmap height scale factor: " + bitmapHeightScaleFactor);

                    int actualWidth = (int)(maxWidth * (1 / mTextureView.mScaleFactorX));
                    int actualHeight = (int)(maxHeight * (1 / mTextureView.mScaleFactorY));
                    Log.d(TAG, "run: actual width: " + actualWidth);
                    Log.d(TAG, "run: actual height: " + actualHeight);


                    int scaledWidth = (int)(actualWidth * bitmapWidthScaleFactor);
                    int scaledHeight = (int)(actualHeight * bitmapHeightScaleFactor);
                    Log.d(TAG, "run: scaled width: " + scaledWidth);
                    Log.d(TAG, "run: scaled height: " + scaledHeight);

                    focusX *= bitmapWidthScaleFactor;
                    focusY *= bitmapHeightScaleFactor;

                    Bitmap background = null;
                    background = Bitmap.createBitmap(
                            mCapturedBitmap,
                            focusX,
                            focusY,
                            scaledWidth,
                            scaledHeight
                    );

                    classify(background);

                    Glide.with(activity)
                            .setDefaultRequestOptions(options)
                            .load(background)
                            .into(mStillshotImageView);

                    showStillshotContainer();
                }
            });
        }
    }

    private void showStillshotContainer(){
        mMainButton.setImageResource(R.drawable.save);

        mStillshotContainer.setVisibility(View.VISIBLE);
        mFlashContainer.setVisibility(View.INVISIBLE);
//        mMenuContainer.setVisibility(View.INVISIBLE);
//        mSwitchOrientationContainer.setVisibility(View.INVISIBLE);
//        mCaptureBtnContainer.setVisibility(View.INVISIBLE);

        mIMainActivity.hideStatusBar();
        closeCamera();
    }



    /**
     *  WARNING!
     *  Can cause memory leaks! To prevent this the object is a global and CANCEL is being called
     *  in "OnPause".
     */
    private class BackgroundImageRotater extends AsyncTask<Void, Integer, Integer>{

        Activity mActivity;

        public BackgroundImageRotater(Activity activity) {
            mActivity = activity;
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            Log.d(TAG, "doInBackground: adjusting image for display...");
            SimpleDateFormat s = new SimpleDateFormat("ddMMyyyyhhmmss");
            imageName = s.format(new Date());

            File file = new File(mActivity.getExternalFilesDir(null), "temp_image.jpg");
            File newFile = new File(mActivity.getExternalFilesDir(null),  imageName+ ".jpg");
            file.renameTo(newFile);
            final Uri tempImageUri = Uri.fromFile(newFile);
            Log.d(TAG, Uri.fromFile(file) + " ");
            mCapturedUri = Uri.fromFile(newFile);

            Bitmap bitmap = null;
            try {
                ExifInterface exif = new ExifInterface(tempImageUri.getPath());
                bitmap = MediaStore.Images.Media.getBitmap(mActivity.getContentResolver(), tempImageUri);
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                mCapturedBitmap = rotateBitmap(bitmap, orientation);
            } catch (IOException e) {
                e.printStackTrace();
                return 0;
            }
            return 1;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            if(integer == 1){
                displayCapturedImage();
            }
            else{
                showSnackBar("Error preparing image", Snackbar.LENGTH_SHORT);
            }
        }
    }

    private void classify(Bitmap bitmap_orig) {
        mCardContent.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        initializeTflite();

        Log.d(TAG, "DONE CAPUTE!");
        // get current bitmap from imageView
//        Bitmap bitmap_orig = mBitmap;
        // resize the bitmap to the required input size to the CNN
        Bitmap bitmap = getResizedBitmap(bitmap_orig, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y);
        // convert bitmap to byte array
        convertBitmapToByteBuffer(bitmap);
        // pass byte data to the graph
        tflite.run(imgData, labelProbArray);
        // display the results
        printTopKLabels();
    }

    // print the top labels and respective confidences
    private void printTopKLabels() {
        // add all results to priority queue
        for (int i = 0; i < labelList.size(); ++i) {
            sortedLabels.add(new AbstractMap.SimpleEntry<>(labelList.get(i), labelProbArray[0][i]));

            if (sortedLabels.size() > RESULTS_TO_SHOW) {
                sortedLabels.poll();
            }
        }

        // get top results from priority queue
        final int size = sortedLabels.size();
        String res = "";
        for (int i = 0; i < size; ++i) {
            Map.Entry<String, Float> label = sortedLabels.poll();
            topLables[i] = label.getKey();
            topConfidence[i] = String.format("%.0f%%",label.getValue()*100);
            res += topLables[i] + " : " + topConfidence[i] + "\n";
        }

        System.out.println("=========");
        System.out.println(res);

        mVegRef.orderByChild("name").equalTo(topLables[2]).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (Integer.parseInt(topConfidence[2].substring(0, topConfidence[2].length() - 1)) < -300) {
                    topLables[2] = "Unknown";
                    mStatus.setImageResource(R.drawable.ic_help_outline_black_24dp);
                    mVegName.setText("Unknown");
                    mDescr.setText("Subject was not found on the vegetable list, please take a photo again");
                    mDescr.setVisibility(View.VISIBLE);
                } else {
                    if (dataSnapshot.exists()) {
                        mStatus.setImageResource(R.drawable.ic_check_black_24dp);
                        mDescr.setVisibility(View.INVISIBLE);
                        isEdible = true;
                    } else {
                        mStatus.setImageResource(R.drawable.ic_close_black_24dp);
                        mDescr.setVisibility(View.INVISIBLE);
                        isEdible = false;
                    }
                }
                mVegName.setText(topLables[2]);
                mCardContent.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                mCardContent.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }
        });

        // set the corresponding textviews with the results
//        label1.setText("1. "+topLables[2]);
//        label2.setText("2. "+topLables[1]);
//        label3.setText("3. "+topLables[0]);
//        Confidence1.setText(topConfidence[2]);
//        Confidence2.setText(topConfidence[1]);
//        Confidence3.setText(topConfidence[0]);
//        new MaterialAlertDialogBuilder(getContext(), R.style.custom_material_theme)
//            .setTitle("Result")
//            .setMessage(res)
//            .setNegativeButton("Cancel", null )
//            .setPositiveButton("Continue", null)
//            .show();
    }

    // resizes bitmap to given dimensions
    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();

        System.out.println("!!!");
        System.out.println(width);
        System.out.println(height);
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(
            bm,
            0,
                0,
            width,
            height,
            matrix,
      false);
        return resizedBitmap;
    }

    // converts bitmap to byte array which is passed in the tflite graph
    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (imgData == null) {
            return;
        }
        imgData.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        // loop through all pixels
        int pixel = 0;
        for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                final int val = intValues[pixel++];
                // get rgb values from intValues where each int holds the rgb values for a pixel.
                // if quantized, convert each rgb value to a byte, otherwise to a float
                imgData.putFloat((((val >> 16) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                imgData.putFloat((((val >> 8) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                imgData.putFloat((((val) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
            }
        }
    }

    private Bitmap rotateBitmap(Bitmap bitmap, int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_TRANSPOSE:
                Log.d(TAG, "rotateBitmap: transpose");
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_NORMAL:
                Log.d(TAG, "rotateBitmap: normal.");
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                Log.d(TAG, "rotateBitmap: flip horizontal");
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                Log.d(TAG, "rotateBitmap: rotate 180");
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                Log.d(TAG, "rotateBitmap: rotate vertical");
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                Log.d(TAG, "rotateBitmap: rotate 90");
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                Log.d(TAG, "rotateBitmap: transverse");
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                Log.d(TAG, "rotateBitmap: rotate 270");
                matrix.setRotate(-90);
                break;
        }
        try {
            if (mIMainActivity.isCameraFrontFacing()) {
                Log.d(TAG, "rotateBitmap: MIRRORING IMAGE.");
                matrix.postScale(-1.0f, 1.0f);
            }

            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();

            return bmRotated;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Saves a JPEG {@link Image} into the specified {@link File}.
     */
    private static class ImageSaver implements Runnable {

        /** The file we save the image into. */
        private final File mFile;

        /** Original image that was captured */
        private Image mImage;

        private ICallback mCallback;

        private Bitmap mBitmap;

        ImageSaver(Bitmap bitmap, File file, ICallback callback) {
            mBitmap = bitmap;
            mFile = file;
            mCallback = callback;
        }

        ImageSaver(Image image, File file, ICallback callback) {
            mImage = image;
            mFile = file;
            mCallback = callback;
        }

        @Override
        public void run() {

            if(mImage != null){
                ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                FileOutputStream output = null;
                try {
                    File file = new File(mFile, "temp_image.jpg");
                    output = new FileOutputStream(file);
                    output.write(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                    mCallback.done(e);
                } finally {
                    mImage.close();
                    if (null != output) {
                        try {
                            output.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    mCallback.done(null);
                }
            }
            else if(mBitmap != null){
                ByteArrayOutputStream stream = null;
                byte[] imageByteArray = null;
                stream = new ByteArrayOutputStream();
                mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                imageByteArray = stream.toByteArray();

                SimpleDateFormat s = new SimpleDateFormat("ddMMyyyyhhmmss");
                String format = s.format(new Date());
                File file = new File(mFile, "image_" + format + ".jpg");

                // save the mirrored byte array
                FileOutputStream output = null;
                try {
                    output = new FileOutputStream(file);
                    output.write(imageByteArray);
                } catch (IOException e) {
                    mCallback.done(e);
                    e.printStackTrace();
                } finally {
                    if (null != output) {
                        try {
                            output.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        mCallback.done(null);
                    }
                }
            }
        }
    }


    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showSnackBar(final String text, final int length) {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    View view = activity.findViewById(android.R.id.content).getRootView();
                    Snackbar.make(view, text, length).show();
                }
            });
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try{
            mIMainActivity = (IMainActivity) getActivity();
        }catch (ClassCastException e){
            Log.e(TAG, "onAttach: ClassCastException: " + e.getMessage() );
        }
    }

    /**
     * Shows an error message dialog.
     */
    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }

    }

    /**
     * Shows OK/Cancel confirmation dialog about camera permission.
     */
    public static class ConfirmationDialog extends DialogFragment {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.request_permission)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            parent.requestPermissions(new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CAMERA_PERMISSION);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Activity activity = parent.getActivity();
                                    if (activity != null) {
                                        activity.finish();
                                    }
                                }
                            })
                    .create();
        }
    }
}
