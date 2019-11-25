package com.ambell.imagecache;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.io.File;
import java.lang.ref.WeakReference;

public class BackGroundImageTask extends AsyncTask<File, Void, Bitmap> {

    WeakReference<ImageView> imageViewRef;
    final static int IMAGE_VIEW_WIDTH = 200;
    final static int IMAAGE_VIEW_HEIGHT = 200;
    private File imageFile;

    public BackGroundImageTask(ImageView imageView){
        imageViewRef = new WeakReference<ImageView>(imageView);
    }
    @Override
    protected Bitmap doInBackground(File... files) {
       // return BitmapFactory.decodeFile(files[0].getAbsolutePath());
        imageFile = files[0];
        ///return decodeBitmapFromFile(files[0]);
        Bitmap bitmap = decodeBitmapFromFile(imageFile);
        MainActivity.setBitmapToMemoryCache(imageFile.getName(), bitmap);
        return bitmap;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if(bitmap != null && imageViewRef != null) {
            ImageView image = imageViewRef.get();
            if(image != null){
                image.setImageBitmap(bitmap);
            }
        }
        if(isCancelled()){
            bitmap = null;
        }
        if(bitmap != null && imageViewRef != null){
            ImageView imageView = imageViewRef.get();
            BackGroundImageTask backGroundImageTask = ImageAdapter.getBackGroundImageTask(imageView);
            if(this == backGroundImageTask && imageView != null){
               imageView.setImageBitmap(bitmap);
            }
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options bmOptions){
        final int photoWidth = bmOptions.outWidth;
        final int photoHeight = bmOptions.outHeight;
        int scaleFactor = 1;

        if(photoWidth > IMAGE_VIEW_WIDTH || photoHeight > IMAAGE_VIEW_HEIGHT) {
            final int halfPhotoWidth = photoWidth/2;
            final int halfPhotoHeight = photoHeight/2;
            while(halfPhotoWidth/scaleFactor > IMAGE_VIEW_WIDTH
                    || halfPhotoHeight/scaleFactor > IMAAGE_VIEW_HEIGHT) {
                scaleFactor *= 2;
            }
        }
        return scaleFactor;
    }

    private Bitmap decodeBitmapFromFile(File imageFile) {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imageFile.getAbsolutePath(), bmOptions);
        bmOptions.inSampleSize = calculateInSampleSize(bmOptions);
        bmOptions.inJustDecodeBounds = false;
        addInBitmapOptions(bmOptions);
        return BitmapFactory.decodeFile(imageFile.getAbsolutePath(), bmOptions);
    }

    public File getImageFile() {
        return imageFile;
    }

    private static void addInBitmapOptions(BitmapFactory.Options options){
        options.inMutable = true;
        Bitmap bitmap = MainActivity.getBitmapFromReuseableSet(options);
        if(bitmap != null){
            options.inBitmap = bitmap;
        }
    }
}
