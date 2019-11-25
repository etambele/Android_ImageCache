package com.ambell.imagecache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.LruCache;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private String IMAGE_FOLDER = "ImageCache app Images";
    private static final int MY_PERMISSIONS_REQUEST_SAVE = 1;
    private File fileGallaryFolder;
    private RecyclerView recyclerView;
    private static LruCache<String, Bitmap> memoryCache;
    private static Set<SoftReference<Bitmap>> reuseableBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getPermission();
        createImageFolder();

        recyclerView = (RecyclerView) findViewById(R.id.my_recyclerView);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, 1);
        recyclerView.setLayoutManager(layoutManager);
        RecyclerView.Adapter imageAdapter = new ImageAdapter(fileGallaryFolder);
        recyclerView.setAdapter(imageAdapter);

        final int maxMemorySize = (int) Runtime. getRuntime().maxMemory() / 1024;
        final int cacheSize = maxMemorySize / 10;

        memoryCache = new LruCache<String, Bitmap>(cacheSize){
            @Override
            protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
                super.entryRemoved(evicted, key, oldValue, newValue);
                reuseableBitmap.add(new SoftReference<Bitmap>(oldValue));
            }

            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount() / 1024;
            }
        };

        reuseableBitmap = Collections.synchronizedSet(new HashSet<SoftReference<Bitmap>>());
    }

    protected void createImageFolder(){
        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        fileGallaryFolder = new File(storageDirectory, IMAGE_FOLDER);
        if(!fileGallaryFolder.exists()){
            fileGallaryFolder.mkdirs();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        refreshAdapter();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAdapter();
    }

    private void refreshAdapter(){
        RecyclerView.Adapter newImageAdapter = new ImageAdapter(fileGallaryFolder);
        recyclerView.swapAdapter(newImageAdapter, false);
    }

    private void getPermission() {
        if ((ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) || (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE},MY_PERMISSIONS_REQUEST_SAVE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_SAVE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "Permission granted!",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Permission denied!",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static Bitmap getBitmapFromMemoryCache(String key){
        return memoryCache.get(key);
    }

    public static void setBitmapToMemoryCache(String key, Bitmap bitmap){
        if(getBitmapFromMemoryCache(key) == null){
            memoryCache.put(key, bitmap);
        }
    }

    private static int getBytesPerPixel(Bitmap.Config config){
        if(config == Bitmap.Config.ARGB_8888){
            return 4;
        } else if(config == Bitmap.Config.RGB_565){
            return 2;
        } else if(config == Bitmap.Config.ARGB_4444){
            return 2;
        } else if (config == Bitmap.Config.ALPHA_8){
            return 1;
        }
        return 1;
    }

    private static boolean canUseForBitmap(Bitmap candidate, BitmapFactory.Options options){
        int width = options.outWidth / options.inSampleSize;
        int height = options.outHeight / options.inSampleSize;
        int byteCount = width * height * getBytesPerPixel(candidate.getConfig());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return byteCount <= candidate.getAllocationByteCount();
        }
        return candidate.getWidth() == options.outWidth &&
                candidate.getHeight() == options.outHeight &&
                options.inSampleSize == 1;
    }

    public static Bitmap getBitmapFromReuseableSet(BitmapFactory.Options options){
        Bitmap bitmap = null;
        if(reuseableBitmap != null && !reuseableBitmap.isEmpty()){
            synchronized (reuseableBitmap) {
                Bitmap item;
                Iterator<SoftReference<Bitmap>> iterator = reuseableBitmap.iterator();
                while(iterator.hasNext()){
                    item = iterator.next().get();
                    if(item != null && item.isMutable()){
                        if(canUseForBitmap(item, options)){
                            bitmap = item;
                            iterator.remove();
                            break;
                        }
                    }else {
                        iterator.remove();
                    }
                }
            }
        }
        return bitmap;
    }
}
