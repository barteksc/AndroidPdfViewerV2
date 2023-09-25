package com.github.barteksc.pdfviewer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.github.barteksc.sample.PDFViewActivity;

public class PDFAnnotationsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdfview);

        Intent intent = new Intent(this, PDFViewActivity.class);
        this.startActivity(intent);

    }
}