package com.blend.ndkadvanced;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.blend.ndkadvanced.databinding.ActivityMainBinding;
import com.blend.ndkadvanced.gif.GifDemoActivity;
import com.blend.ndkadvanced.hello.HelloWorldActivity;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnHelloWorld.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, HelloWorldActivity.class))
        );

        binding.btnGif.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, GifDemoActivity.class))
        );
    }
}