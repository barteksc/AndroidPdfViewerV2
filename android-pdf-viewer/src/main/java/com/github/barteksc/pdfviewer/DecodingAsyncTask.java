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
package com.github.barteksc.pdfviewer;

import android.content.Context;
import android.os.AsyncTask;

import com.github.barteksc.pdfviewer.source.DocumentSource;

import org.benjinus.pdfium.PdfDocument;
import org.benjinus.pdfium.PdfiumSDK;
import org.benjinus.pdfium.util.Size;

import java.lang.ref.WeakReference;

class DecodingAsyncTask extends AsyncTask<Void, Void, Throwable> {

    private boolean cancelled;

//    private PDFView pdfView;

    private Context context;
    private PdfiumSDK pdfiumSDK;
//    private PdfDocument pdfDocument;

    private String password;
    private DocumentSource docSource;
    private int firstPageIdx;
    private int pageWidth;
    private int pageHeight;

    private int[] userPages;
    private WeakReference<PDFView> pdfViewReference;
    private PdfFile pdfFile;


//    DecodingAsyncTask(DocumentSource docSource, String password,  int[] userPages, PDFView pdfView, PdfiumSDK pdfiumSDK, int firstPageIdx) {
//        this.docSource = docSource;
//        this.firstPageIdx = firstPageIdx;
//        this.cancelled = false;
//        this.userPages = userPages;
//        this.pdfView = pdfView;
//        this.pdfViewReference = new WeakReference<>(pdfView);
//        this.password = password;
//        this.pdfiumSDK = pdfiumSDK;
//        context = pdfView.getContext();
//    }

    DecodingAsyncTask(DocumentSource docSource, String password, int[] userPages, PDFView pdfView, PdfiumSDK pdfiumSDK) {
        this.docSource = docSource;
        this.userPages = userPages;
        this.cancelled = false;
        this.pdfViewReference = new WeakReference<>(pdfView);
        this.password = password;
        this.pdfiumSDK = pdfiumSDK;
    }

    @Override
    protected Throwable doInBackground(Void... params) {
        try {
            PDFView pdfView = pdfViewReference.get();
            if (pdfView != null) {
                PdfDocument pdfDocument = docSource.createDocument(pdfView.getContext(), pdfiumSDK, password);
                pdfFile = new PdfFile(pdfiumSDK, pdfDocument, pdfView.getPageFitPolicy(), getViewSize(pdfView),
                        userPages, pdfView.isSwipeVertical(), pdfView.getSpacingPx(), pdfView.isAutoSpacingEnabled(),
                        pdfView.isFitEachPage());
                return null;
            } else {
                return new NullPointerException("pdfView == null");
            }

        } catch (Throwable t) {
            return t;
        }

//        try {
//            pdfDocument = docSource.createDocument(context, pdfiumSDK, password);
//            // We assume all the pages are the same size
//            pdfiumSDK.openPage(pdfDocument, firstPageIdx);
//            pageWidth = pdfiumSDK.getPageWidth(pdfDocument, firstPageIdx);
//            pageHeight = pdfiumSDK.getPageHeight(pdfDocument, firstPageIdx);
//            return null;
//        } catch (Throwable t) {
//            return t;
//        }
    }

    @Override
    protected void onPostExecute(Throwable t) {
        PDFView pdfView = pdfViewReference.get();
        if (t != null) {
            pdfView.loadError(t);
            return;
        }
        if (!cancelled) {
//            pdfView.loadComplete(pdfDocument, pageWidth, pageHeight);
            pdfView.loadComplete(pdfFile);
        }
    }

    @Override
    protected void onCancelled() {
        cancelled = true;
    }

    private Size getViewSize(PDFView pdfView) {
        return new Size(pdfView.getWidth(), pdfView.getHeight());
    }
}
