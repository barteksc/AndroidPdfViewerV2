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
import android.graphics.PointF;
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
import com.github.barteksc.pdfviewer.link.LinkHandler;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnLongPressListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener;
import com.github.barteksc.pdfviewer.listener.OnTapListener;
import com.github.barteksc.pdfviewer.model.LinkTapEvent;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.github.barteksc.pdfviewer.util.PublicFunction;
import com.github.barteksc.pdfviewer.util.PublicValue;
import com.github.barteksc.pdfviewer.util.UriUtils;
import com.lowagie.text.Annotation;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfImage;
import com.lowagie.text.pdf.PdfIndirectObject;
import com.lowagie.text.pdf.PdfLayer;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.NonConfigurationInstance;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;
import org.benjinus.pdfium.Bookmark;
import org.benjinus.pdfium.Meta;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@EActivity(R.layout.activity_main)
@OptionsMenu(R.menu.options)
public class PDFViewActivity extends AppCompatActivity implements OnPageChangeListener, OnLoadCompleteListener, OnErrorListener,
        OnPageErrorListener, OnTapListener, OnLongPressListener, LinkHandler {

    private static final String TAG = PDFViewActivity.class.getSimpleName();

    private final static int REQUEST_CODE = 42;
    public static final int PERMISSION_CODE = 42042;

    public static final String SAMPLE_FILE = "foo.pdf";
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
    private String currFileName;
    private String currFilePath;


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
            Toast.makeText(this, "currUri is null", Toast.LENGTH_SHORT).show();
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

        this.currFilePath = UriUtils.getPathFromUri(this, currUri);
        this.currFileName = pdfFileName;

        this.configurator = pdfView.fromUri(currUri)
                .defaultPage(PublicValue.DEFAULT_PAGE_NUMBER)
                .onPageChange(this)
                .enableAnnotationRendering(true)
                .onLoad(this)
                .enableSwipe(true)
                .scrollHandle(new DefaultScrollHandle(this))
                .spacing(10) // in dp
                .onPageError(this)
                .onTap(this)
                .onLongPress(this)
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
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
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
        // here we have a long click
        Log.i(TAG, "onLongPress --> X: " + e.getX() + " | Y: " + e.getY());
        Log.i(TAG, "--------------------------------------------------");
        try {
            addAnnotation(e);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void addAnnotation(MotionEvent e) throws IOException {

        // Generate reference hash
        String referenceHash = new StringBuilder()
                .append(PublicValue.KEY_REFERENCE_HASH)
                .append(UUID.randomUUID().toString())
                .toString();

        // Get image marker
        byte[] OCGCover = PublicFunction.Companion.getByteFromDrawable(PDFViewActivity.this, R.drawable.marker);

        String filePath = UriUtils.getPathFromUri(PDFViewActivity.this, currUri);

        PointF pointF = pdfView.convertScreenPintsToPdfCoordinates(e);

        new Handler().post(() -> {
            // Code here will run in UI thread
            try {
                // todo: use real currPage
                boolean isAdded = addOCG(pointF, filePath, 1, referenceHash, OCGCover, 0, 0);
                Log.d(TAG, "addAnnotation: isAdded = " + isAdded);

                if(isAdded){
                    configurator.refresh(pdfView.getCurrentPage()); // refresh view
                }
//                    MagicalPdfCore.getInstance().addOCG(pointF, filePath, currPage, referenceHash, OCGCover, OCGWidth, OCGHeight);
//                    MagicalPECViewModel.this.pecCoreStatus.postValue(PECCoreStatusEnum.SUCCESS);
            } catch (Exception e1) {
//                    MagicalPECViewModel.this.pecCoreStatus.postValue(PECCoreStatusEnum.FAILED);
                e1.printStackTrace();
            }
        });

    }

    public boolean addOCG(PointF pointF, String filePath, int currPage, String referenceHash, byte[] OCGCover, float OCGWidth, float OCGHeight) throws Exception {

        // Hint: OCG -> optional content group
        // Hint: Page Starts From --> 1 In OpenPdf Core
        currPage++;

        // OCG width & height
        if (OCGWidth == 0 || OCGHeight == 0) {
            OCGWidth = PublicValue.DEFAULT_OCG_WIDTH;
            OCGHeight = PublicValue.DEFAULT_OCG_HEIGHT;
        }

        // get file and FileOutputStream
        if (filePath == null || filePath.isEmpty())
            throw new Exception("Input file is empty");

        File file = new File(filePath);

        if (!file.exists())
            throw new Exception("Input file does not exists");

        try {

            // inout stream from file
            InputStream inputStream = new FileInputStream(file);

            // we create a reader for a certain document
            PdfReader reader = new PdfReader(inputStream);

            // we create a stamper that will copy the document to a new file
            PdfStamper stamp = new PdfStamper(reader, new FileOutputStream(file));

            // get watermark icon
            Image img = Image.getInstance(OCGCover);
            img.setAnnotation(new Annotation(0, 0, 0, 0, referenceHash));
            img.scaleAbsolute(OCGWidth, OCGHeight);
            img.setAbsolutePosition(pointF.x, pointF.y);
            PdfImage stream = new PdfImage(img, referenceHash, null);
            stream.put(new PdfName(PublicValue.KEY_SPECIAL_ID), new PdfName(referenceHash));
            PdfIndirectObject ref = stamp.getWriter().addToBody(stream);
            img.setDirectReference(ref.getIndirectReference());

            // add as layer
            PdfLayer wmLayer = new PdfLayer(referenceHash, stamp.getWriter());

            // prepare transparency
            PdfGState transparent = new PdfGState();
            transparent.setAlphaIsShape(false);

            // get page file number count
            if (reader.getNumberOfPages() < currPage) {
                stamp.close();
                reader.close();
                throw new Exception("Page index is out of pdf file page numbers");
            }

            // add annotation into target page
            PdfContentByte over = stamp.getOverContent(currPage);
            if (over == null) {
                stamp.close();
                reader.close();
                throw new Exception("GetUnderContent() is null");
            }

            // add as layer
            over.beginLayer(wmLayer);
            over.setGState(transparent); // set block transparency properties
            over.addImage(img);
            over.endLayer();

            // closing PdfStamper will generate the new PDF file
            stamp.close();

            // close reader
            reader.close();

            // finish method
            return true;

        } catch (Exception ex) {
            throw new Exception(ex.getMessage());
        }
    }

    @Override
    public boolean onTap(MotionEvent e) {
        // here we have a tap
        Log.i(TAG, "onTap --> X: " + e.getX() + " | Y: " + e.getY());
        Log.i(TAG, "--------------------------------------------------");

//        // check zoom and scale
//        Log.i(TAG, "zoom --> " + pdfView.getZoom() + " | scale " + pdfView.getScaleX() + " , " + pdfView.getScaleY());
//        Log.i(TAG, "--------------------------------------------------");

        return false;
    }

    @Override
    public void handleLinkEvent(LinkTapEvent event) {
        // TODO
    }

    @Override
    public void onError(Throwable t) {
        Toast.makeText(this, t.getMessage(), Toast.LENGTH_LONG ).show();
    }
}
