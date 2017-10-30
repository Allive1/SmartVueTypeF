package com.vsu.smartvuetypef.util;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class FrameManager {

    private static FrameManager sInstance = null;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT;
    private final BlockingQueue<Runnable> mTransposeWorkQueue;
    private final ThreadPoolExecutor mTransposeThreadPool;
    private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    private static final int KEEP_ALIVE_TIME = 1;
    private Handler mHandler;

    static final int DOWNLOAD_FAILED = -1;
    static final int DOWNLOAD_STARTED = 1;
    static final int DOWNLOAD_COMPLETE = 2;
    static final int DECODE_STARTED = 3;
    static final int TASK_COMPLETE = 4;

    // A static block that sets class fields
    static {

        // The time unit for "keep alive" is in seconds
        KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

        // Creates a single static instance of PhotoManager
        sInstance = new FrameManager();
    }

    private FrameManager() {
        mTransposeWorkQueue = new LinkedBlockingQueue<Runnable>();

        mTransposeThreadPool = new ThreadPoolExecutor(NUMBER_OF_CORES, NUMBER_OF_CORES,
                KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, mTransposeWorkQueue);
        mHandler = new Handler(Looper.getMainLooper()) {

            /*
             * handleMessage() defines the operations to perform when the
             * Handler receives a new Message to process.
             */
            @Override
            public void handleMessage(Message inputMessage) {

                // Gets the image task from the incoming Message object.
                FrameTask frameTask = (FrameTask) inputMessage.obj;

                // Sets an PhotoView that's a weak reference to the
                // input ImageView
                //FrameView localView = frameTask.getFrameView();

                // If this input view isn't null
                //if (localView != null) {

                    /*
                     * Gets the URL of the *weak reference* to the input
                     * ImageView. The weak reference won't have changed, even if
                     * the input ImageView has.
                     */
                   // URL localURL = localView.getLocation();

                    /*
                     * Compares the URL of the input ImageView to the URL of the
                     * weak reference. Only updates the bitmap in the ImageView
                     * if this particular Thread is supposed to be serving the
                     * ImageView.
                     */
                    //if (frameTask.getImageURL() == localURL)

                        /*
                         * Chooses the action to take, based on the incoming message
                         */
                        switch (inputMessage.what) {

                            // If the download has started, sets background color to dark green
                            case DOWNLOAD_STARTED:
                                //localView.setStatusResource(R.drawable.imagedownloading);
                                break;

                            /*
                             * If the download is complete, but the decode is waiting, sets the
                             * background color to golden yellow
                             */
                            case DOWNLOAD_COMPLETE:
                                // Sets background color to golden yellow
                                //localView.setStatusResource(R.drawable.decodequeued);
                                break;
                            // If the decode has started, sets background color to orange
                            case DECODE_STARTED:
                                //localView.setStatusResource(R.drawable.decodedecoding);
                                break;
                            /*
                             * The decoding is done, so this sets the
                             * ImageView's bitmap to the bitmap in the
                             * incoming message
                             */
                            case TASK_COMPLETE:
                                //localView.setImageBitmap(photoTask.getImage());
                                //recycleTask(photoTask);
                                break;
                            // The download failed, sets the background color to dark red
                            case DOWNLOAD_FAILED:
                                //localView.setStatusResource(R.drawable.imagedownloadfailed);

                                // Attempts to re-use the Task object
                                //recycleTask(photoTask);
                                break;
                            default:
                                // Otherwise, calls the super method
                                super.handleMessage(inputMessage);
                        }
               // }
            }
        };
    }

    public void handleState(FrameTask frameTask, int state) {
        switch (state) {

            // The task finished downloading and decoding the image
            case TASK_COMPLETE:

                // Puts the image into cache
                /*if (photoTask.isCacheEnabled()) {
                    // If the task is set to cache the results, put the buffer
                    // that was
                    // successfully decoded into the cache
                    mFrameCache.put(frameTask.getImageURL(), frameTask.getByteBuffer());
                }*/

                // Gets a Message object, stores the state in it, and sends it to the Handler
                Message completeMessage = mHandler.obtainMessage(state, frameTask);
                completeMessage.sendToTarget();
                break;

            // The task finished downloading the image
            case DOWNLOAD_COMPLETE:
                /*
                 * Decodes the image, by queuing the decoder object to run in the decoder
                 * thread pool
                 */
                mTransposeThreadPool.execute(frameTask.getFrameTransposeRunnable());

                // In all other cases, pass along the message without any other action.
            default:
                mHandler.obtainMessage(state, frameTask).sendToTarget();
                break;
        }

    }

    public static void cancelAll() {

        /*
         * Creates an array of tasks that's the same size as the task work queue
         */
        //FrameTask[] taskArray = new FrameTask[sInstance.mDownloadWorkQueue.size()];

        // Populates the array with the task objects in the queue
        //sInstance.mDownloadWorkQueue.toArray(taskArray);

        // Stores the array length in order to iterate over the array
        //int taskArraylen = taskArray.length;

        /*
         * Locks on the singleton to ensure that other processes aren't mutating Threads, then
         * iterates over the array of tasks and interrupts the task's current Thread.
         */
        /*synchronized (sInstance) {

            // Iterates over the array of tasks
            for (int taskArrayIndex = 0; taskArrayIndex < taskArraylen; taskArrayIndex++) {

                // Gets the task's current thread
                Thread thread = taskArray[taskArrayIndex].mThreadThis;

                // if the Thread exists, post an interrupt to it
                if (null != thread) {
                    thread.interrupt();
                }
            }
        }*/
    }

    //static public void removeDownload(FrameTask downloaderTask, URL pictureURL) {

        // If the Thread object still exists and the download matches the specified URL
        //if (downloaderTask != null && downloaderTask.getImageURL().equals(pictureURL)) {

            /*
             * Locks on this class to ensure that other processes aren't mutating Threads.
             */
           /* synchronized (sInstance) {

                // Gets the Thread that the downloader task is running on
                Thread thread = downloaderTask.getCurrentThread();

                // If the Thread exists, posts an interrupt to it
                if (null != thread)
                    thread.interrupt();
            }
            /*
             * Removes the download Runnable from the ThreadPool. This opens a Thread in the
             * ThreadPool's work queue, allowing a task in the queue to start.
             */
          //  sInstance.mDownloadThreadPool.remove(downloaderTask.getHTTPDownloadRunnable());
        //}
    //}

    /*static public FrameTask startDownload(
            FrameView imageView,
            boolean cacheFlag) {

        /*
         * Gets a task from the pool of tasks, returning null if the pool is empty
         */
       // FrameTask downloadTask = sInstance.mFrameTaskWorkQueue.poll();

        // If the queue was empty, create a new task instead.
        /*if (null == downloadTask) {
            downloadTask = new FrameTask();
        }*/

        // Initializes the task
        //downloadTask.initializeTransposerTask(FrameManager.sInstance, imageView, cacheFlag);

        /*
         * Provides the download task with the cache buffer corresponding to the URL to be
         * downloaded.
         */
        //downloadTask.setByteBuffer(sInstance.mPhotoCache.get(downloadTask.getImageURL()));

        // If the byte buffer was empty, the image wasn't cached
        //if (null == downloadTask.getByteBuffer()) {

            /*
             * "Executes" the tasks' download Runnable in order to download the image. If no
             * Threads are available in the thread pool, the Runnable waits in the queue.
             */
            //sInstance.mDownloadThreadPool.execute(downloadTask.getHTTPDownloadRunnable());

            // Sets the display to show that the image is queued for downloading and decoding.
            //imageView.setStatusResource(R.drawable.imagequeued);

            // The image was cached, so no download is required.
        //} else {

            /*
             * Signals that the download is "complete", because the byte array already contains the
             * undecoded image. The decoding starts.
             */

            //sInstance.handleState(downloadTask, DOWNLOAD_COMPLETE);
        //}

        // Returns a task object, either newly-created or one from the task pool
        //return downloadTask;
    //}

    void recycleTask(FrameTask downloadTask) {

        // Frees up memory in the task
        downloadTask.recycle();

        // Puts the task object back into the queue for re-use.
       // mFrameTaskWorkQueue.offer(downloadTask);
    }

    public static FrameManager getInstance() {

        return sInstance;
    }
}
