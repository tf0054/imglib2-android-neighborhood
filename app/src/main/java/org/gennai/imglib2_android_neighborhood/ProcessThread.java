package org.gennai.imglib2_android_neighborhood;

import android.graphics.Color;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gauss.Gauss;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.algorithm.region.hypersphere.HyperSphere;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.type.Type;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.view.Views;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Created by tf0054 on 5/27/17.
 */

// http://bit.ly/2qnMxYV
public class ProcessThread extends HandlerThread {

    private Callback mCallback;
    private Handler mWorkerHandler;
    private Handler resHandler;
    private static final String TAG = ProcessThread.class.getSimpleName();

    public interface Callback {
        public void addPeakInfo(final int[] pixels, int x, int y);
    }

    public ProcessThread(Handler responseHandler, Callback callback) {
        super(TAG);
        mCallback = callback;
        resHandler = responseHandler;
    }

    public void prepareHandler(int x, int y) {
        mWorkerHandler = new Handler(getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {

                int[] bIntAry = (int[]) msg.obj;
                int[] s = Arrays.stream(bIntAry).map(x -> Color.red(x)).toArray();

                kickFindPeaks(msg.what, s, x, y);
                // msg.recycle();
                return true; // TODO
            }
        });
    }

    public void queueTask(int flag, int[] audio) {
        mWorkerHandler.obtainMessage(flag, audio).sendToTarget();
    }

    // https://stackoverflow.com/q/7802707
    private void kickFindPeaks(int flag, int[] values, int x, int y) {
        // Preparing data for searching local maxima (Cutting low magnitudes cells and applying Gaussean filter on it)
        //lowMagsFilter(mags);
        ArrayImg<IntType, IntArray> img = ArrayImgs.ints(values, x, y);
        Gauss.inDoubleInPlace(new double[]{1, 1}, img);

        // Makeing local maxima result bitmap (tmpBitmap consists 1x1 aspect
        int[] pixels = findLocalMaxima(Views.interval(Views.extendBorder(img), img));
        resHandler.post(new Runnable() {
            @Override
            public void run() {
                mCallback.addPeakInfo(pixels, x, y);
            }
        });
    }

    private static <T extends Type<T> & Comparable<T>> int[] findLocalMaxima(final RandomAccessibleInterval<T> img) {
        Img<BitType> output = (new ArrayImgFactory<BitType>()).create(img, new BitType());
        // Create a neighborhood Shape, in this case a rectangle.
        // The parameters are span and skipCenter: span = 1 says that this will
        // be a 3x3x...x3 rectangle shape (where 3 == 2 * span + 1). skipCenter
        // = true says that the center pixel of the 3x3x...x3 shape is skipped
        // when iterating the shape.
        final RectangleShape shape = new RectangleShape(1, true);

        // Create a RandomAccess of img (This will be used to access the center
        // pixel of a neighborhood.)
        final RandomAccess<T> center = img.randomAccess();

        // Use the shape to create an Iterable<Neighborhood<T>>.
        // This is a IterableInterval whose elements of Neighborhood<T> are all
        // the 3x3x...x3 neighborhoods of img.
        // The Neighborhood<T> themselves are IterableIntervals over the pixels
        // of type T in the 3x3x...x3 neighborhood. The Neighborhood<T> are also
        // Localizable, and localize() provides the coordinates which the
        // neighborhood is currently centered on.
        //
        // Note: By "all the 3x3x...x3 neighborhoods of img" we mean the set of
        // 3x3x...x3 neighborhoods centered on every pixel of img.
        // This means that out-of-bounds values will be accessed. The 3x3
        // neighborhood centered on pixel (0,0) contains pixels
        // {(-1,-1)...(1,1)}
        final Iterable<Neighborhood<T>> neighborhoods = shape.neighborhoods(img);

        // Iterate over all neighborhoods.
        for (final Neighborhood<T> neighborhood : neighborhoods) {
            // Position the center RandomAccess to the origin of the current
            // neighborhood and get() the centerValue.
            center.setPosition(neighborhood);
            final T centerValue = center.get();

            // Loop over pixels of the neighborhood and check whether the
            // centerValue is strictly greater than all of them. Note that
            // because we specified skipCenter = true for the RectangleShape the
            // center pixel itself is not included in the neighborhood values.
            boolean isMaximum = true;
            for (final T value : neighborhood) {
                if (value.compareTo(centerValue) >= 0) {
                    isMaximum = false;
                    break;
                }
            }

            // If this is a maximum print it's coordinates.
            //    if ( isMaximum )
            //        System.out.println( "maximum found at " + Util.printCoordinates( center ) );
            if (isMaximum) {
                // draw a sphere of radius one in the new image
                HyperSphere<BitType> hyperSphere = new HyperSphere<BitType>(output, center, 1);

                // set every value inside the sphere to 1
                for (BitType value : hyperSphere)
                    value.setOne();
            }
        }
        Iterator<BitType> b = output.iterator();

        int i = 0;
        int[] ret = new int[(int) output.size()];
        while (b.hasNext()) {
            if (1 == b.next().getInteger()) {
                ret[i++] = 255;
            } else {
                ret[i++] = 0;
            }
        }
        return ret;
    }

}