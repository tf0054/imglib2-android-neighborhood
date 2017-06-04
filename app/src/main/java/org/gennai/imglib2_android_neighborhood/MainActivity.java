package org.gennai.imglib2_android_neighborhood;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import org.beyka.tiffbitmapfactory.TiffBitmapFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static android.graphics.Bitmap.createBitmap;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "imglib2_android_neighborhood";

    private SubsamplingScaleImageView mainView, neighborView;

    private ProgressDialog progressDialog;
    private int selected = -1;
    private int[] keep_intAry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mainView = (SubsamplingScaleImageView) findViewById(R.id.mainview);
        neighborView = (SubsamplingScaleImageView) findViewById(R.id.neighborview);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final AssetManager assetManager = getAssets();
        String[] list;

        final String[] fileNames;// = list;

        switch (item.getItemId()) {
            case R.id.load_file:
                try {
                    list = assetManager.list("samples");
                } catch (IOException e) {
                    list = new String[0];
                }
                selected = -1;
                fileNames = list;
                new AlertDialog.Builder(this)
                        .setTitle(getTitle())
                        .setSingleChoiceItems(fileNames, selected, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                selected = which;
                            }
                        })
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @SuppressLint("StaticFieldLeak") // TODO
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (selected >= 0) {
                                    new AsyncTask<Void, Void, Void>() {
                                        protected Void doInBackground(Void... params) {
                                            Runnable showBitmap = () -> {
                                                // on UI thread
                                                if (fileNames != null) {
                                                    Bitmap bmp = getSampleTiffAsBmp("samples" + '/'
                                                            + fileNames[selected]);
                                                    mainView.setImage(ImageSource.bitmap(bmp));
                                                    keep_intAry = getBmpIntarray(bmp);
                                                    //mainView.setBmp(bmp);
                                                }
                                            };
                                            runOnUiThread(showBitmap);
                                            return null;
                                        }
                                    }.execute();
                                }
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                break;
            case R.id.find_peak:
                ProcessThread processThread = new ProcessThread(new Handler(Looper.getMainLooper()),
                        (final int[] pixels, int x, int y) -> {
                            // Makeing result bitmap (tmpBitmap consists 1x1 aspect
                            int alpha = 100;
                            Bitmap tmpBuffer;
                            int[] converted = new int[pixels.length];

                            for (int i = 0; i < pixels.length; i++) {
                                //greyscale, so all r, g, and b have the same value
                                converted[i] = (alpha << 24) | (pixels[i] << 16) | (pixels[i] << 8) | pixels[i];
                            }

                            neighborView.setImage(
                                    ImageSource.bitmap(
                                            createBitmap(converted, x, y, Bitmap.Config.ARGB_8888)));

                            progressDialog.dismiss();
                        });
                processThread.start();
                processThread.prepareHandler(mainView.getWidth(),
                        mainView.getHeight());
                processThread.queueTask(1, keep_intAry);
                //
                progressDialog.setMessage("Please wait..");
                progressDialog.show();
                break;
        }
        return true;
    }

    public int[] getBmpIntarray(Bitmap bmp) {
        int[] tmpArray = new int[bmp.getWidth() * bmp.getHeight()];
        bmp.getPixels(tmpArray, 0, bmp.getWidth(),
                0, 0, bmp.getWidth(), bmp.getHeight());
        return tmpArray;
    }

    private Bitmap getSampleTiffAsBmp(String filepath) {
        AssetManager am = getAssets();

        InputStream inputStream = null;
        try {
            inputStream = am.open(filepath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        TiffBitmapFactory.Options options = new TiffBitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inAvailableMemory = 20000000; // bytes

        File file = createFileFromInputStream(inputStream);
        TiffBitmapFactory.decodeFile(file, options);

        double wscale = (double) mainView.getWidth() / options.outWidth;
        double hscale = (double) mainView.getHeight() / options.outHeight;
        double scale = Math.max(wscale,hscale);

        options.inJustDecodeBounds = false;

        return Bitmap.createScaledBitmap(
                //addPaddingBottomForBitmap(TiffBitmapFactory.decodeFile(file, options),400),
                TiffBitmapFactory.decodeFile(file, options),
                (int) (options.outWidth * wscale), (int) (options.outHeight * wscale), true);
    }

    private Bitmap addPaddingBottomForBitmap(Bitmap bitmap, int paddingBottom) {
        Bitmap outputBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight() + paddingBottom, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(outputBitmap);
        canvas.drawColor(Color.RED);
        canvas.drawBitmap(bitmap, 0, 0, null);
        return outputBitmap;
    }

    private File createFileFromInputStream(InputStream inputStream) {

        try {
            File f = File.createTempFile("prefix", ".tif");
            OutputStream outputStream = new FileOutputStream(f);
            byte buffer[] = new byte[1024];
            int length = 0;

            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();

            return f;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
