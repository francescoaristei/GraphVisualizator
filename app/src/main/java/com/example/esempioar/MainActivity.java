package com.example.esempioar;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    public void launchCameraActivity(View view) throws IOException {

        Intent intent = new Intent(this, FileActivity.class);
        startActivity(intent);
    }
}