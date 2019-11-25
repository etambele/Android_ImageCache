package com.ambell.imagecache;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.File;
import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import static android.content.ContentValues.TAG;


public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder>{

    private File imagesFile;
    private Bitmap placeHolderBitmap;

    public static class AsyncDrawable extends BitmapDrawable {
        final WeakReference<BackGroundImageTask> taskReference;

        private AsyncDrawable(Resources resources,
                             Bitmap bitmap,
                             BackGroundImageTask bitmapWorkerTask){
            super(resources, bitmap);
            taskReference = new WeakReference(bitmapWorkerTask);
        }

        private BackGroundImageTask getBackGroundImageTask(){
            return taskReference.get();
        }
    }

    public ImageAdapter(File folderFile){
        imagesFile = folderFile;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.image_relative_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File imageFile = imagesFile.listFiles()[position];
        // Bitmap imageBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        // holder.getImageView().setImageBitmap(imageBitmap);
        // BackGroundImageTask imageTask = new BackGroundImageTask(holder.getImageView());
        // imageTask.execute(imageFile);
        Bitmap bitmap = MainActivity.getBitmapFromMemoryCache(imageFile.getName());
        if(bitmap != null){
            holder.getImageView().setImageBitmap(bitmap);
        }else if(checkBackGroundImageTask(imageFile,  holder.getImageView())){
            BackGroundImageTask backGroundWorker = new BackGroundImageTask(holder.getImageView());
            AsyncDrawable asyncDrawable = new AsyncDrawable(holder.getImageView().getResources(),
                    placeHolderBitmap,
                    backGroundWorker);
            holder.getImageView().setImageDrawable(asyncDrawable);
            backGroundWorker.execute(imageFile);
        }
    }

    @Override
    public int getItemCount() {
        try{
            return imagesFile.listFiles().length;
        }catch(Exception e){
            if(e instanceof NullPointerException){
                Log.d(TAG, "getItemCount: No Image in the folder");
            }
        }
        return 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageView;

        private ViewHolder(View view) {
            super(view);
            imageView = view.findViewById(R.id.image_layout);
        }

        private ImageView getImageView() {
            return imageView;
        }
    }

    private static boolean checkBackGroundImageTask(File imageFile, ImageView imageView){
        BackGroundImageTask backGroundImageTask = getBackGroundImageTask(imageView);
        if(backGroundImageTask != null) {
            final File workerFile = backGroundImageTask.getImageFile();
            if(workerFile != null){
                if(workerFile != imageFile){
                    backGroundImageTask.cancel(true);
                }else{
                    return false;
                }
            }
        }
        return true;
    }
    public static BackGroundImageTask getBackGroundImageTask(ImageView imageView){
        Drawable drawable = imageView.getDrawable();
        if(drawable instanceof AsyncDrawable){
            AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
            return asyncDrawable.getBackGroundImageTask();
        }
        return null;
    }
}
