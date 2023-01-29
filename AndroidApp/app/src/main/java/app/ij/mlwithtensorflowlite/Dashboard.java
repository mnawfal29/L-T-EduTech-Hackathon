package app.ij.mlwithtensorflowlite;

import static app.ij.mlwithtensorflowlite.MainActivity.cracked;
import static app.ij.mlwithtensorflowlite.MainActivity.uncracked;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.TextView;

public class Dashboard extends AppCompatActivity {
    TextView cracked_count, uncracked_count;
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard);
        cracked_count = findViewById(R.id.cracked_count);
        uncracked_count = findViewById(R.id.uncracked_count);
        cracked_count.setText(String.valueOf(cracked));
        uncracked_count.setText(String.valueOf(uncracked));
    }
}
