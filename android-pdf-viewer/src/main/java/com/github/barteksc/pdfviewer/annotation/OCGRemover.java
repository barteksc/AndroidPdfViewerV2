package com.github.barteksc.pdfviewer.annotation;

import com.lowagie.text.pdf.PRStream;
import com.lowagie.text.pdf.PdfArray;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfObject;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OCGRemover {

    /**
     * Removes layers from a PDF document
     *
     * @param reader a PdfReader containing a PDF document
     * @param layers a sequence of names of OCG layers
     * @throws IOException
     */
    public void removeLayers(PdfReader reader, String... layers) throws IOException {
        int n = reader.getNumberOfPages();
        for (int i = 1; i <= n; i++)
            reader.setPageContent(i, reader.getPageContent(i));
        Set<String> ocgs = new HashSet<String>();
        for (int i = 0; i < layers.length; i++) {
            ocgs.add(layers[i]);
        }
        OCGParser parser = new OCGParser(ocgs);
        PdfDictionary page;
        for (int i = 1; i <= n; i++) {
            page = reader.getPageN(i);
            parse(parser, page);
            // page.remove(PdfName.PIECEINFO); // TODO: 12/22/20 can not find PIECEINFO in pdf name :?
            removeAnnots(page, ocgs);
            removeProperties(page, ocgs);
        }
        PdfDictionary root = reader.getCatalog();
        PdfDictionary ocproperties = root.getAsDict(PdfName.OCPROPERTIES);
        if (ocproperties != null) {
            removeOCGsFromArray(ocproperties, PdfName.OCGS, ocgs);
            PdfDictionary d = ocproperties.getAsDict(PdfName.D);
            if (d != null) {
                removeOCGsFromArray(d, PdfName.ON, ocgs);
                removeOCGsFromArray(d, PdfName.OFF, ocgs);
                removeOCGsFromArray(d, PdfName.LOCKED, ocgs);
                removeOCGsFromArray(d, PdfName.RBGROUPS, ocgs);
                removeOCGsFromArray(d, PdfName.ORDER, ocgs);
                removeOCGsFromArray(d, PdfName.AS, ocgs);
            }
            PdfArray ocgsArray = ocproperties.getAsArray(PdfName.OCGS);
            if (ocgsArray != null && ocgsArray.isEmpty()) {
                root.remove(PdfName.OCPROPERTIES);
                if (PdfName.USEOC.equals(root.getAsName(PdfName.PAGEMODE))) {
                    root.remove(PdfName.PAGEMODE);
                }
            }
        }
        reader.removeUnusedObjects();
    }

    /**
     * Gets an array from a dictionary and checks if it contains references to OCGs that need to be removed
     *
     * @param dict the dictionary
     * @param name the name of an array entry
     * @param ocgs the removal list
     */
    private void removeOCGsFromArray(PdfDictionary dict, PdfName name, Set<String> ocgs) {
        if (dict == null)
            return;
        PdfArray array = dict.getAsArray(name);
        if (array == null)
            return;
        removeOCGsFromArray(array, ocgs);
    }

    /**
     * Searches an array for references to OCGs that need to be removed.
     *
     * @param array the array
     * @param ocgs  the removal list
     */
    private void removeOCGsFromArray(PdfArray array, Set<String> ocgs) {
        if (array == null)
            return;
        PdfObject o;
        PdfDictionary dict;
        List<Integer> remove = new ArrayList<Integer>();
        for (int i = array.size(); i > 0; ) {
            o = array.getDirectObject(--i);
            if (o.isDictionary()) {
                dict = (PdfDictionary) o;
                if (isToBeRemoved(dict, ocgs)) {
                    remove.add(i);
                } else {
                    removeOCGsFromArray(dict, PdfName.OCGS, ocgs);
                }
            }
            if (o.isArray()) {
                removeOCGsFromArray((PdfArray) o, ocgs);
            }
        }
        for (Integer i : remove) {
            array.remove(i);
        }
    }

    /**
     * Removes annotations from a page dictionary
     *
     * @param page a page dictionary
     * @param ocgs a set of names of OCG layers
     */
    private void removeAnnots(PdfDictionary page, Set<String> ocgs) {
        PdfArray annots = page.getAsArray(PdfName.ANNOTS);
        if (annots == null) return;
        PdfDictionary annot;
        List<Integer> remove = new ArrayList<Integer>();
        for (int i = annots.size(); i > 0; ) {
            annot = annots.getAsDict(--i);
            if (isToBeRemoved(annot.getAsDict(PdfName.OC), ocgs)) {
                remove.add(i);
            } else {
                removeOCGsFromArray(annot.getAsDict(PdfName.A), PdfName.STATE, ocgs);
            }
        }
        for (Integer i : remove) {
            annots.remove(i);
        }
    }

    /**
     * Removes ocgs from a page resources
     *
     * @param page a page dictionary
     * @param ocgs a set of names of OCG layers
     */
    private void removeProperties(PdfDictionary page, Set<String> ocgs) {
        PdfDictionary resources = page.getAsDict(PdfName.RESOURCES);
        if (resources == null) return;
        PdfDictionary properties = resources.getAsDict(PdfName.PROPERTIES);
        if (properties == null) return;
        Set<PdfName> names = properties.getKeys();
        List<PdfName> remove = new ArrayList<PdfName>();
        PdfDictionary dict;
        for (PdfName name : names) {
            dict = properties.getAsDict(name);
            if (isToBeRemoved(dict, ocgs)) {
                remove.add(name);
            } else {
                removeOCGsFromArray(dict, PdfName.OCGS, ocgs);
            }
        }
        for (PdfName name : remove) {
            properties.remove(name);
        }
    }

    /**
     * Checks if an OCG dictionary is on the list for removal.
     *
     * @param ocg   a dictionary
     * @param names the removal list
     * @return true if the dictionary should be removed
     */
    private boolean isToBeRemoved(PdfDictionary ocg, Set<String> names) {
        if (ocg == null)
            return false;
        PdfString n = ocg.getAsString(PdfName.NAME);
        if (n == null)
            return false;
        return names.contains(n.toString());
    }

    /**
     * Uses the OCGParser on a page
     *
     * @param parser the OCGParser
     * @param page   the page dictionary of the page that needs to be parsed.
     * @throws IOException
     */
    private void parse(OCGParser parser, PdfDictionary page) throws IOException {
        PRStream stream = (PRStream) page.getAsStream(PdfName.CONTENTS);
        PdfDictionary resources = page.getAsDict(PdfName.RESOURCES);
        parser.parse(stream, resources);
    }

}
