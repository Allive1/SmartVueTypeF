package main.java.com.vsu.smartvuetypef.util;

import android.util.Log;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

/**
 * Created by Omi on 1/18/16.
 */
public class FrameTransposeRunnable implements Runnable {

    // Limits the number of times the decoder tries to process an image
    private static final int NUMBER_OF_TRANSPOSE_TRIES = 2;

    // Tells the Runnable to pause for a certain number of milliseconds
    private static final long SLEEP_TIME_MILLISECONDS = 250;

    // Sets the log tag
    private static final String LOG_TAG = "FrameTransposeRunnable";

    // Constants for indicating the state of the decode
    static final int TRANSPOSE_STATE_FAILED = -1;
    static final int TRANSPOSE_STATE_STARTED = 0;
    static final int TRANSPOSE_STATE_COMPLETED = 1;

    // Defines a field that contains the calling object of type PhotoTask.
    final TaskRunnableTransposeMethods mFrameTask;

    interface TaskRunnableTransposeMethods {

        /**
         * Sets the Thread that this instance is running on
         * @param currentThread the current Thread
         */
        void setFrameTransposeThread(Thread currentThread);

        /**
         * Returns the current contents of the download buffer
         * @return The byte array downloaded from the URL in the last read
         */
        //byte[] getByteBuffer();

        /**
         * Sets the actions for each state of the PhotoTask instance.
         * @param state The state being handled.
         */
        void handleTransposeState(int state);

        /**
         * Returns the desired width of the image, based on the ImageView being created.
         * @return The target width
         */
        int getTargetWidth();

        /**
         * Returns the desired height of the image, based on the ImageView being created.
         * @return The target height.
         */
        int getTargetHeight();

        /**
         * Sets the Bitmap for the ImageView being displayed.
         * @param image
         */
        void setImage(CameraBridgeViewBase.CvCameraViewFrame image);
    }

    FrameTransposeRunnable(TaskRunnableTransposeMethods downloadTask) {
        mFrameTask = downloadTask;
    }

    @Override
    public void run() {

        /*
         * Stores the current Thread in the the PhotoTask instance, so that the instance
         * can interrupt the Thread.
         */
        mFrameTask.setFrameTransposeThread(Thread.currentThread());

        /*
         * Gets the image cache buffer object from the PhotoTask instance. This makes the
         * to both PhotoDownloadRunnable and PhotoTask.
         */
        //byte[] imageBuffer = mFrameTask.getByteBuffer();
        //Mat matBuffer = mFrameTask.getMatBuffer();

        // Defines the Bitmap object that this thread will create
        CameraBridgeViewBase.CvCameraViewFrame returnMat = null;

        /*
         * A try block that decodes a downloaded image.
         *
         */
        try {

            /*
             * Calls the PhotoTask implementation of {@link #handleDecodeState} to
             * set the state of the download
             */
            mFrameTask.handleTransposeState(TRANSPOSE_STATE_STARTED);

            // Sets up options for creating a Bitmap object from the
            // downloaded image.
            //MatFactory.Options bitmapOptions = new BitmapFactory.Options();

            /*
             * Sets the desired image height and width based on the
             * ImageView being created.
             */
            int targetWidth = mFrameTask.getTargetWidth();
            int targetHeight = mFrameTask.getTargetHeight();

            // Before continuing, checks to see that the Thread hasn't
            // been interrupted
            if (Thread.interrupted()) {

                return;
            }

            /*
             * Even if the decoder doesn't set a Bitmap, this flag tells
             * the decoder to return the calculated bounds.
             */
            //bitmapOptions.inJustDecodeBounds = true;

            /*
             * First pass of decoding to get scaling and sampling
             * parameters from the image
             */
            //BitmapFactory
              //      .decodeByteArray(imageBuffer, 0, imageBuffer.length, bitmapOptions);

            /*
             * Sets horizontal and vertical scaling factors so that the
             * image is expanded or compressed from its actual size to
             * the size of the target ImageView
             */
            //int hScale = bitmapOptions.outHeight / targetHeight;
            //int hScale = .outHeight / targetHeight;
            //int wScale = bitmapOptions.outWidth / targetWidth;

            /*
             * Sets the sample size to be larger of the horizontal or
             * vertical scale factor
             */
            //
            //int sampleSize = Math.max(hScale, wScale);

            /*
             * If either of the scaling factors is > 1, the image's
             * actual dimension is larger that the available dimension.
             * This means that the BitmapFactory must compress the image
             * by the larger of the scaling factors. Setting
             * inSampleSize accomplishes this.
             */
           /* if (sampleSize > 1) {
                bitmapOptions.inSampleSize = sampleSize;
            }

            if (Thread.interrupted()) {
                return;
            }*/

            // Second pass of decoding. If no bitmap is created, nothing
            // is set in the object.
            //bitmapOptions.inJustDecodeBounds = false;

            /*
             * This does the actual decoding of the buffer. If the
             * decode encounters an an out-of-memory error, it may throw
             * an Exception or an Error, both of which need to be
             * handled. Once the problem is handled, the decode is
             * re-tried.
             */
            for (int i = 0; i < NUMBER_OF_TRANSPOSE_TRIES; i++) {
                try {
                    // Tries to decode the image buffer
                    /*returnBitmap = BitmapFactory.decodeByteArray(
                            imageBuffer,
                            0,
                            imageBuffer.length,
                            bitmapOptions
                    );*/
                    //returnMat =
                    /*
                     * If the decode works, no Exception or Error has occurred.
                    break;

                    /*
                     * If the decode fails, this block tries to get more memory.
                     */
                } catch (Throwable e) {

                    // Logs an error
                    Log.e(LOG_TAG, "Out of memory in decode stage. Throttling.");

                    /*
                     * Tells the system that garbage collection is
                     * necessary. Notice that collection may or may not
                     * occur.
                     */
                    java.lang.System.gc();

                    if (Thread.interrupted()) {
                        return;

                    }
                    /*
                     * Tries to pause the thread for 250 milliseconds,
                     * and catches an Exception if something tries to
                     * activate the thread before it wakes up.
                     */
                    try {
                        Thread.sleep(SLEEP_TIME_MILLISECONDS);
                    } catch (java.lang.InterruptedException interruptException) {
                        return;
                    }
                }
            }

            // Catches exceptions if something tries to activate the
            // Thread incorrectly.
        } finally {
            // If the decode failed, there's no bitmap.
            if (null == returnMat) {

                // Sends a failure status to the PhotoTask
                mFrameTask.handleTransposeState(TRANSPOSE_STATE_FAILED);

                // Logs the error
                Log.e(LOG_TAG, "Download failed in PhotoDecodeRunnable");

            } else {

                // Sets the ImageView Bitmap
                mFrameTask.setImage(returnMat);

                // Reports a status of "completed"
                mFrameTask.handleTransposeState(TRANSPOSE_STATE_COMPLETED);
            }

            // Sets the current Thread to null, releasing its storage
            //mFrameTask.setMatTransposeThread(null);

            // Clears the Thread's interrupt flag
            Thread.interrupted();

        }

    }

}
