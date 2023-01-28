/*
 * Created by ishaanjav
 * github.com/ishaanjav
 */

package app.ij.mlwithtensorflowlite;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.media.Image;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;

// Initialization code
// Create an ImageProcessor with all ops required. For more ops, please
// refer to the ImageProcessor Architecture section in this README.


import app.ij.mlwithtensorflowlite.ml.Model;


public class MainActivity extends AppCompatActivity {

    Button camera, gallery;
    ImageView imageView;
    TextView result;
    int imageSize = 224;

    int cracked = 0;
    int uncracked = 0;

    private static final int PERMISSION_REQUEST_CODE = 123;
    private int REQUEST_PICK_IMAGE = 1000;
    private int REQUEST_CAPTURE_IMAGE = 1001;
    private ImageView inputImageView;
    private TextView outputTextView;


    private File photoFile;



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // All permissions granted
                Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show();
            } else {
                // Some permissions denied
                Toast.makeText(this, "Permissions denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    String currentPhotoPath;
    private File createPhotoFile() {
        File photoFileDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "ML_IMAGE_HELPER");

        if (!photoFileDir.exists()) {
            photoFileDir.mkdirs();
        }

        String name = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File file = new File(photoFileDir.getPath() + File.separator + name);
        return file;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        camera = findViewById(R.id.button);
        gallery = findViewById(R.id.button2);

        result = findViewById(R.id.result);
        imageView = findViewById(R.id.imageView);

        requestPermissions(new String[] {Manifest.permission.CAMERA}, 0);
        requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);


        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    photoFile = createPhotoFile();
                    Uri fileUri = FileProvider.getUriForFile(getApplicationContext(), "com.example.fileproviders", photoFile);

                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

                    //StartActivity
                    startActivityForResult(intent, REQUEST_CAPTURE_IMAGE);

                } else {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
                }
            }
        });
        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent cameraIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                cameraIntent.setType("image/*");

                startActivityForResult(cameraIntent, REQUEST_PICK_IMAGE);
            }
        });
    }

    private Bitmap loadFromUri(Uri uri) {
        Bitmap bitmap = null;

        try {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
                bitmap = ImageDecoder.decodeBitmap(source);
            } else {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    public int classifyImage(Bitmap image){
        int maxPos = 0;
        try {
            Model model = Model.newInstance(getApplicationContext());

            ImageProcessor imageProcessor =
                    new ImageProcessor.Builder()
                            .add(new ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
                            .build();

            TensorImage tensorImage = new TensorImage(DataType.FLOAT32);


            tensorImage.load(image);
            tensorImage = imageProcessor.process(tensorImage);


            // Creates inputs for reference.
            TensorBuffer inputFeature0 = tensorImage.getTensorBuffer();

            // Runs model inference and gets result.
            Model.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidences = outputFeature0.getFloatArray();
            // find the index of the class with the biggest confidence.

            float maxConfidence = 0;

            if (confidences[1] > confidences[0]){
                maxPos = 1;
                maxConfidence = confidences[1];
                cracked++;
            }
            else{
                maxPos = 0;
                maxConfidence = confidences[0];
                uncracked++;
            }
            String[] classes = {"Not Cracked", "Cracked"};
            result.setText(classes[maxPos]);

            // Releases model resources if no longer used.
            model.close();
            return maxPos;
        } catch (IOException e) {
            // TODO Handle the exception
        }
        return maxPos;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        int index = 0;

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_PICK_IMAGE) {
                Uri dat = data.getData();
                Bitmap image = null;
                try {
                    image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), dat);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                imageView.setImageBitmap(image);

                classifyImage(image);
            } else if (requestCode == REQUEST_CAPTURE_IMAGE) {
                Log.d("ML", "received callback from camera");
                Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                imageView.setImageBitmap(bitmap);
                classifyImage(bitmap);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}