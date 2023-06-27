/*******************************************************************************
 * Copyright 2020-2022 Zebrunner Inc (https://www.zebrunner.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.zebrunner.carina.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessBufferedFileInputStream;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * PDFUtil - utility for PDF file parsing.
 * 
 * @author Sergey Zagriychuk
 *         <a href="mailto:szagriychuk@gmail.com">Sergey Zagriychuk</a>
 * @deprecated old/useless logic.
 */
@Deprecated(forRemoval = true, since = "1.0.5")
public final class PDFUtil {

    private PDFUtil() {
    }

    /**
     * Reads PDF content in specified page range.
     * 
     * @param inputStream InputStream
     * @param startPage Start Page
     * @param endPage End Page
     * @return PDF content
     */
    public static String readTxtFromPDF(InputStream inputStream, int startPage, int endPage) {
        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream argument cannot be null");
        }
        PDFTextStripper pdfStripper = null;
        PDFParser parser = null;
        try (inputStream;
                RandomAccessBufferedFileInputStream randomAccessBufferedFileInputStream = new RandomAccessBufferedFileInputStream(inputStream)) {
            parser = new PDFParser(randomAccessBufferedFileInputStream);
            parser.parse();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        try (COSDocument cosDoc = parser.getDocument();
                PDDocument pdDoc = new PDDocument(cosDoc)) {
            pdfStripper = new PDFTextStripper();
            pdfStripper.setSortByPosition(true);
            pdfStripper.setStartPage(startPage);
            pdfStripper.setEndPage(endPage);
            return pdfStripper.getText(pdDoc);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
