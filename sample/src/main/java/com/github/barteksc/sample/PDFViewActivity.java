/**
 * Copyright 2016 Bartosz Schiller
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.barteksc.sample;

import static com.github.barteksc.pdfviewer.util.PublicValue.KEY_REQUEST_FILE_PICKER;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.annotation.core.AnnotationManager;
import com.github.barteksc.pdfviewer.annotation.core.PdfToImageResultData;
import com.github.barteksc.pdfviewer.annotation.core.PdfUtil;
import com.github.barteksc.pdfviewer.link.LinkHandler;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnLongPressListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener;
import com.github.barteksc.pdfviewer.listener.OnTapListener;
import com.github.barteksc.pdfviewer.model.LinkTapEvent;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.github.barteksc.pdfviewer.util.DebugUtilKt;
import com.github.barteksc.pdfviewer.util.PublicValue;
import com.github.barteksc.pdfviewer.util.UriUtils;
import com.google.android.material.snackbar.Snackbar;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.NonConfigurationInstance;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;
import org.benjinus.pdfium.Bookmark;
import org.benjinus.pdfium.Meta;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@EActivity(R.layout.activity_main)
@OptionsMenu(R.menu.options)
public class PDFViewActivity extends AppCompatActivity implements OnPageChangeListener, OnLoadCompleteListener, OnErrorListener,
        OnPageErrorListener, OnTapListener, OnLongPressListener, LinkHandler {

    private static final String TAG = PDFViewActivity.class.getSimpleName();

    public static final int PERMISSION_CODE = 42042;
    public static final String READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE";

    PDFView.Configurator configurator = null;

    @ViewById
    PDFView pdfView;

    @NonConfigurationInstance
    Uri uri;

    @NonConfigurationInstance
    Integer pageNumber = 0;

    String pdfFileName;

    private Uri currUri = null;
    private String currFilePath = null;


    @OptionsItem(R.id.pickFile)
    void pickFile() {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                READ_EXTERNAL_STORAGE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{READ_EXTERNAL_STORAGE},
                    PERMISSION_CODE
            );

            return;
        }

        launchPicker();
    }

    void launchPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        try {
            startActivityForResult(intent, KEY_REQUEST_FILE_PICKER);
        } catch (ActivityNotFoundException e) {
            //alert user that file manager not working
            Toast.makeText(this, R.string.toast_pick_file_error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);

        this.pdfView = findViewById(R.id.pdfView);

    }

    @AfterViews
    void afterViews() {
        pdfView.setBackgroundColor(Color.LTGRAY);
        setTitle(pdfFileName);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == PublicValue.KEY_REQUEST_FILE_PICKER) {
            if (data != null && data.getData() != null) {
                this.currUri = data.getData();
                displayFileFromUri();
            } else {
                Log.e(TAG, "onActivityResult, requestCode:" + requestCode + "resultCode:" + resultCode);
            }
        }
    }

    private void displayFileFromUri() {

        if (currUri == null) {
            DebugUtilKt.toast(this, "currUri is null");
            return;
        }

        // TODO: 1/17/21  DON NOT FORGET TO USE YOUR FILE HANDLING SCENARIO FOR NEW ANDROID APIs

        // this is OK
        // /storage/emulated/0/Download/PDF_ENGLISH.pdf
        // this.currFilePath = UriUtils.getPathFromUri(MainActivity.this, currUri);

        // this is not OK
        // /data/user/0/ir.vasl.magicalpdfeditor/files/PDF_ENGLISH.pdf
        // this.currFilePath = PublicFunction.getFilePathForN(MainActivity.this, currUri);

        // this is working
        // /storage/emulated/0/Download/PDF_ENGLISH.pdf

        pdfFileName = getFileName(currUri);

        currFilePath = UriUtils.getPathFromUri(this, currUri);

        this.configurator = pdfView.fromUri(currUri)
                .defaultPage(PublicValue.DEFAULT_PAGE_NUMBER)
                .onPageChange(this)
                .enableAnnotationRendering(true)
                .enableAnnotationHandling(true)
                .onLoad(this)
                .enableSwipe(true)
                .scrollHandle(new DefaultScrollHandle(this))
                .spacing(10) // in dp
                .onPageError(this)
                .onTap(this)
                .onLongPress(this)
                .onError(this)
                .linkHandler(this);

        this.configurator.load();
    }

    @Override
    public void onPageChanged(int page, int pageCount) {
        pageNumber = page;
        setTitle(String.format("%s %s / %s", pdfFileName, page + 1, pageCount));
    }

    @SuppressLint("Range")
    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    @Override
    public void loadComplete(int nbPages) {
        Meta meta = pdfView.getDocumentMeta();
        Log.e(TAG, "title = " + meta.getTitle());
        Log.e(TAG, "author = " + meta.getAuthor());
        Log.e(TAG, "subject = " + meta.getSubject());
        Log.e(TAG, "keywords = " + meta.getKeywords());
        Log.e(TAG, "creator = " + meta.getCreator());
        Log.e(TAG, "producer = " + meta.getProducer());
        Log.e(TAG, "creationDate = " + meta.getCreationDate());
        Log.e(TAG, "modDate = " + meta.getModDate());

        printBookmarksTree(pdfView.getTableOfContents(), "-");

    }

    public void printBookmarksTree(List<Bookmark> tree, String sep) {
        for (Bookmark b : tree) {

            Log.e(TAG, String.format("%s %s, p %d", sep, b.getTitle(), b.getPageIdx()));

            if (b.hasChildren()) {
                printBookmarksTree(b.getChildren(), sep + "-");
            }
        }
    }

    /**
     * Listener for response to user permission request
     *
     * @param requestCode  Check that permission request code matches
     * @param permissions  Permissions that requested
     * @param grantResults Whether permissions granted
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchPicker();
            }
        }
    }

    @Override
    public void onPageError(int page, Throwable t) {
        Log.e(TAG, "Cannot load page " + page);
    }

    @Override
    public void onLongPress(MotionEvent e) {
        if (!pdfView.isAnnotationHandlingEnabled()) {
            return;
        }
        // here we have a long click
        Log.i(TAG, "onLongPress --> X: " + e.getX() + " | Y: " + e.getY());
        Log.i(TAG, "--------------------------------------------------");
        new Handler().post(() -> {
            try {
                // testing converting pdf to image, passing the result data
//                String testFilePath = "/storage/emulated/0/Download/simple-pdf.pdf";
                String testFilePath = "/storage/emulated/0/Download/simple-pdf-single-page.pdf";
//                String testPdfFilePath2 = "/storage/emulated/0/Download/foo.pdf";
                String imageOutputDirectory = "/storage/emulated/0/Download/";
                PdfToImageResultData result = PdfUtil.convertPdfAnnotationsToPngShapes(testFilePath, imageOutputDirectory);
                Log.d(TAG, "onLongPress: result data is" + result);
                boolean isAdded = true;

//                boolean isAdded = AnnotationManager.addTextAnnotation(this, e, currUri, pdfView);
//                boolean isAdded = AnnotationManager.addCircleAnnotation(this, e, currUri, pdfView);
//                boolean isAdded = AnnotationManager.addLineAnnotation(this, e, currUri, pdfView);
//                boolean isAdded = AnnotationManager.addRectAnnotation(this, e, currUri, pdfView);
//                boolean isAdded = AnnotationManager.addLines(this, currUri, pdfView);
//                boolean isAdded = AnnotationManager.addImageAnnotation(this, e, currUri, pdfView);

                if (isAdded) {
                    configurator.refresh(pdfView.getCurrentPage());// refresh view
                } else {
                    Toast.makeText(this, "Annotation couldn't be added", Toast.LENGTH_LONG).show();
                }
                DebugUtilKt.logInfo(TAG, "addAnnotation: isAdded = " + isAdded);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        });
    }

    public void showSnackbar(String referenceHash) {
        String message = "Annotation with " + referenceHash;
        Snackbar snackbar = Snackbar.make(pdfView, message, Snackbar.LENGTH_LONG);
        snackbar.setAction("Delete", v -> new Handler().post(() -> {
            try {
                boolean isRemoved = AnnotationManager.removeAnnotation(this, currUri, referenceHash);
                if (isRemoved) {
                    configurator.refresh(pdfView.getCurrentPage()); // refresh view
                } else {
                    DebugUtilKt.toast(this, "Annotation couldn't be removed");
                }
                DebugUtilKt.logInfo(TAG, "removeAnnotation: isRemoved = " + isRemoved);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
        snackbar.show();
    }

    @Override
    public boolean onTap(MotionEvent e) {
        // here we have a tap
        Log.i(TAG, "onTap --> X: " + e.getX() + " | Y: " + e.getY());
        Log.i(TAG, "--------------------------------------------------");

        // check zoom and scale
        Log.i(TAG, "zoom --> " + pdfView.getZoom() + " | scale " + pdfView.getScaleX() + " , " + pdfView.getScaleY());
        Log.i(TAG, "--------------------------------------------------");

        return false;
    }

    @Override
    public void handleLinkEvent(LinkTapEvent event) {
        String referenceHash = event.getLink().getUri();
        showSnackbar(referenceHash);
    }

    @Override
    public void onError(Throwable t) {
        DebugUtilKt.toast(this, Objects.requireNonNull(t.getMessage()));
    }
}
