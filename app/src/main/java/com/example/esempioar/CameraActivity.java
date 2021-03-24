package com.example.esempioar;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Menu;
import android.view.MenuItem;
import android.view.PixelCopy;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.assets.RenderableSource;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class CameraActivity extends AppCompatActivity {
    PointerDrawable pointer = new PointerDrawable();
    private ModelRenderable renderable;
    boolean isTracking;
    boolean isHitting;
    private ArFragment fragment;
    private int count = 0;
    String fileName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        fragment = (ArFragment)
                getSupportFragmentManager().findFragmentById(R.id.arFragment);

        FloatingActionButton fab = findViewById(R.id.fab);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            fab.setOnClickListener(view -> takePhoto());
        }

        fragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            fragment.onUpdate(frameTime);
            onUpdate();
        });
       fileName = (String) getIntent().getSerializableExtra("name");
    }


    // To load and visualize the 3D model chosen from in MyDrive
    public void loadModel(View view){
        File file = new File(this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);
        if (count % 2 == 0) {
            buildModel(file);
            ((Button)findViewById(R.id.buttonload)).setText(R.string.visualize);
        }

        else {
            addObject();
            ((Button)findViewById(R.id.buttonload)).setText(R.string.load);
        }
        count++;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)

    private void onUpdate() {

        boolean trackingChanged = updateTracking();
        View contentView = findViewById(android.R.id.content);

        if (trackingChanged) {
            if (isTracking) {
                contentView.getOverlay().add(pointer);
            } else {
                contentView.getOverlay().remove(pointer);
            }
            contentView.invalidate();
        }

        if (isTracking) {
            boolean hitTestChanged = updateHitTest();
            if (hitTestChanged) {
                pointer.setEnabled(isHitting);
                contentView.invalidate();
            }
        }
    }

    private boolean updateTracking() {
        Frame frame = fragment.getArSceneView().getArFrame();
        boolean wasTracking = isTracking;

        isTracking = frame != null &&
                frame.getCamera().getTrackingState() == TrackingState.TRACKING;
        return isTracking != wasTracking;
    }


    private boolean updateHitTest() {
        Frame frame = fragment.getArSceneView().getArFrame();
        android.graphics.Point pt = getScreenCenter();
        List<HitResult> hits;
        boolean wasHitting = isHitting;
        isHitting = false;
        if (frame != null) {

            hits = frame.hitTest(pt.x, pt.y);

            for (HitResult hit : hits) {
                Trackable trackable = hit.getTrackable();
                if (trackable instanceof Plane &&
                        ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
                    isHitting = true;
                    break;
                }
            }
        }
        return wasHitting != isHitting;
    }

    private android.graphics.Point getScreenCenter() {
        View vw = findViewById(android.R.id.content);
        return new android.graphics.Point(vw.getWidth()/2, vw.getHeight()/2);
    }


            @RequiresApi(api = Build.VERSION_CODES.N)
            private void addObject() {
                Frame frame = fragment.getArSceneView().getArFrame();
                android.graphics.Point pt = getScreenCenter();
                List<HitResult> hits;
                if (frame != null) {
                    hits = frame.hitTest(pt.x, pt.y);
                    for (HitResult hit : hits) {
                        Trackable trackable = hit.getTrackable();
                        if (trackable instanceof Plane &&
                                ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
                            addNodeToScene(hit.createAnchor(), renderable);
                            break;

                        }
                    }
                }
            }


            private void buildModel(File file) {

                RenderableSource renderableSource = RenderableSource
                        .builder()
                        .setSource(this, Uri.parse(file.getPath()), RenderableSource.SourceType.GLB)
                        .setRecenterMode(RenderableSource.RecenterMode.ROOT)
                        .build();

                ModelRenderable
                        .builder()
                        .setSource(this, renderableSource)
                        .setRegistryId(file.getPath())
                        .build()
                        .thenAccept(modelRenderable -> {
                            Toast.makeText(this, "Model built", Toast.LENGTH_SHORT).show();
                            renderable = modelRenderable;
                        });
            }

            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            public void addNodeToScene(Anchor anchor, ModelRenderable renderable) {
                AnchorNode anchorNode = new AnchorNode(anchor);
                TransformableNode node = new TransformableNode(fragment.getTransformationSystem());
                node.setRenderable(renderable);
                node.setParent(anchorNode);
                fragment.getArSceneView().getScene().addChild(anchorNode);
                node.select();
            }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



            private String generateFilename() {
                String date =
                        new SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.getDefault()).format(new Date());
                return Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES) + File.separator + "Sceneform/" + date + "_screenshot.jpg";
            }

            private void saveBitmapToDisk(Bitmap bitmap, String filename) throws IOException {

                File out = new File(filename);
                if (!Objects.requireNonNull(out.getParentFile()).exists()) {
                    out.getParentFile().mkdirs();
                }
                try (FileOutputStream outputStream = new FileOutputStream(filename);
                     ByteArrayOutputStream outputData = new ByteArrayOutputStream()) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputData);
                    outputData.writeTo(outputStream);
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException ex) {
                    throw new IOException("Failed to save bitmap to disk", ex);
                }
            }

            @RequiresApi(api = Build.VERSION_CODES.N)
            private void takePhoto() {
                final String filename = generateFilename();
                ArSceneView view = fragment.getArSceneView();

                // Create a bitmap the size of the scene view.
                final Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),
                        Bitmap.Config.ARGB_8888);

                // Create a handler thread to offload the processing of the image.
                final HandlerThread handlerThread = new HandlerThread("PixelCopier");
                handlerThread.start();
                // Make the request to copy.
                PixelCopy.request(view, bitmap, (copyResult) -> {
                    if (copyResult == PixelCopy.SUCCESS) {
                        try {
                            saveBitmapToDisk(bitmap, filename);
                        } catch (IOException e) {
                            Toast toast = Toast.makeText(CameraActivity.this, e.toString(),
                                    Toast.LENGTH_LONG);
                            toast.show();
                            return;
                        }
                        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                                "Photo saved", Snackbar.LENGTH_LONG);
                        snackbar.setAction("Open in Photos", v -> {
                            File photoFile = new File(filename);

                            Uri photoURI = FileProvider.getUriForFile(CameraActivity.this,
                                    CameraActivity.this.getPackageName() + ".ar.codelab.name.provider",
                                    photoFile);
                            Intent intent = new Intent(Intent.ACTION_VIEW, photoURI);
                            intent.setDataAndType(photoURI, "image/*");
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(intent);
                        });
                        snackbar.show();
                    } else {
                        Toast toast = Toast.makeText(CameraActivity.this,
                                "Failed to copyPixels: " + copyResult, Toast.LENGTH_LONG);
                        toast.show();
                    }
                    handlerThread.quitSafely();
                }, new Handler(handlerThread.getLooper()));
            }
        }

