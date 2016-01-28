package main.java.com.vsu.smartvuetypef.util;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;
import main.java.com.vsu.smartvuetypef.util.FrameTransposeRunnable.TaskRunnableTransposeMethods;

/**
 * Created by Omi on 1/18/16.
 */
public class FrameTask implements TaskRunnableTransposeMethods {
    private static FrameManager sFrameManager;

    // The width and height of the decoded image
    private int mTargetHeight;
    private int mTargetWidth;


    //Field containing the Thread this task is running on.
    Thread mThreadThis;

    //Fields containing references to the two runnable objects that handle downloading and
    //decoding of the image.
    private Runnable mTransposeRunnable;

    // A buffer for containing the bytes that make up the image
    CameraBridgeViewBase.CvCameraViewFrame mFrameBuffer;

    // The decoded image
    private Mat mTransposedMat;

    // The Thread on which this task is currently running.
    private Thread mCurrentThread;
    /**
     * Creates an PhotoTask containing a download object and a decoder object.
     */
    FrameTask() {
        // Create the runnables
        mTransposeRunnable = new FrameTransposeRunnable(this);
        sFrameManager = FrameManager.getInstance();
    }

    /*void initializeTransposeTask(
            FrameManager frameManager,
            FrameView frameView,
            boolean cacheFlag)
    {
        // Sets this object's ThreadPool field to be the input argument
        sFrameManager = frameManager;

        // Gets the URL for the View
       // mImageURL = frameView.getLocation();

        // Instantiates the weak reference to the incoming view
        //mImageWeakRef = new WeakReference<FrameView>(frameView);

        // Sets the cache flag to the input argument
        //mCacheEnabled = cacheFlag;

        // Gets the width and height of the provided ImageView
        mTargetWidth = frameView.getWidth();
        mTargetHeight = frameView.getHeight();

    }*/

    void recycle() {

        // Deletes the weak reference to the imageView
        /*if ( null != mImageWeakRef ) {
            mImageWeakRef.clear();
            mImageWeakRef = null;
        }*/

        // Releases references to the byte buffer and the BitMap
        mFrameBuffer = null;
        mTransposedMat = null;
    }

    @Override
    public int getTargetWidth() {
        return mTargetWidth;
    }

    // Implements PhotoDownloadRunnable.getTargetHeight. Returns the global target height.
    @Override
    public int getTargetHeight() {
        return mTargetHeight;
    }

    void handleState(int state) {
        sFrameManager.handleState(this, state);
    }

    Mat getMat() {
        return mTransposedMat;
    }

    Runnable getFrameTransposeRunnable() {
        return mTransposeRunnable;
    }

    /*public FrameView getFrameView() {
        if ( null != mImageWeakRef ) {
            return mImageWeakRef.get();
        }
        return null;
    }*/

    public Thread getCurrentThread() {
        synchronized(sFrameManager) {
            return mCurrentThread;
        }
    }

    public void setCurrentThread(Thread thread) {
        synchronized(sFrameManager) {
            mCurrentThread = thread;
        }
    }

    @Override
    public void setImage(CameraBridgeViewBase.CvCameraViewFrame decodedImage) {
        mTransposedMat = decodedImage.rgba();
    }

    @Override
    public void setFrameTransposeThread(Thread currentThread) {
        setCurrentThread(currentThread);
    }

    /*
     * Implements PhotoDecodeRunnable.handleDecodeState(). Passes the decoding state to the
     * ThreadPool object.
     */
    @Override
    public void handleTransposeState(int state) {
        int outState;

        // Converts the decode state to the overall state.
        switch(state) {
            case FrameTransposeRunnable.TRANSPOSE_STATE_COMPLETED:
                outState = FrameManager.TASK_COMPLETE;
                break;
            case FrameTransposeRunnable.TRANSPOSE_STATE_FAILED:
                outState = FrameManager.DOWNLOAD_FAILED;
                break;
            default:
                outState = FrameManager.DECODE_STARTED;
                break;
        }

        // Passes the state to the ThreadPool object.
        handleState(outState);
    }


}
