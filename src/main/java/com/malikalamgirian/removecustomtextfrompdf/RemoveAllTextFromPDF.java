/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
 /*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.malikalamgirian.removecustomtextfrompdf;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.contentstream.PDContentStream;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDAbstractPattern;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDTilingPattern;

/**
 * This is an example on how to remove all text from PDF document.
 *
 * @author Ben Litchfield, Wasif Altaf
 */
public final class RemoveAllTextFromPDF {

    /**
     * Add the file path i.e. the folder where the file to be cleaned exists
     * 
     * 
     */
    private static String input_pdf_file_path = "D:\\post\\outgoing\\2019\\april";
    
    /**
     * Add the input file name without extension i.e. the name of file which you want to clean
     */
    private static String input_pdf_file_name_without_extension = "zusatzblatt_18_05_2018_to_30_09_2019";
    
    

    private static String input_PDF_file_with_path_with_extension = input_pdf_file_path 
            + "\\"
            + input_pdf_file_name_without_extension 
            + ".pdf";
    
    private static String output_PDF_file_with_path_with_extension = input_pdf_file_path 
            + "\\"
            + input_pdf_file_name_without_extension 
            + "_cleaned.pdf";

    public static String getInput_pdf_file_path() {
        return input_pdf_file_path;
    }

    public static void setInput_pdf_file_path(String aInput_pdf_file_path) {
        input_pdf_file_path = aInput_pdf_file_path;
    }

    public static String getInput_pdf_file_name_without_extension() {
        return input_pdf_file_name_without_extension;
    }

    public static void setInput_pdf_file_name_without_extension(String aInput_pdf_file_name_without_extension) {
        input_pdf_file_name_without_extension = aInput_pdf_file_name_without_extension;
    }

    public static String getInput_PDF_file_with_path_with_extension() {
        return input_PDF_file_with_path_with_extension;
    }

    public static void setInput_PDF_file_with_path_with_extension(String aInput_PDF_file_with_path_with_extension) {
        input_PDF_file_with_path_with_extension = aInput_PDF_file_with_path_with_extension;
    }

    public static String getOutput_PDF_file_with_path_with_extension() {
        return output_PDF_file_with_path_with_extension;
    }

    public static void setOutput_PDF_file_with_path_with_extension(String aOutput_PDF_file_with_path_with_extension) {
        output_PDF_file_with_path_with_extension = aOutput_PDF_file_with_path_with_extension;
    }

    /**
     * Default constructor.
     */
    private RemoveAllTextFromPDF() {

    }

    /**
     * This will remove all text from a PDF document.
     *
     * @param args The command line arguments.
     *
     * @throws IOException If there is an error parsing the document.
     */
    public static void main(String[] args) throws IOException {

        try (PDDocument document = PDDocument.load(new File(RemoveAllTextFromPDF.input_PDF_file_with_path_with_extension))) {
            if (document.isEncrypted()) {
                System.err.println(
                        "Error: Encrypted documents are not supported for this example.");
                System.exit(1);
            }
            for (PDPage page : document.getPages()) {
                List<Object> newTokens = createTokensWithoutText(document, page);
                PDStream newContents = new PDStream(document);
                writeTokensToStream(newContents, newTokens);
                page.setContents(newContents);
                processResources(document, page.getResources());
            }
            document.save(RemoveAllTextFromPDF.output_PDF_file_with_path_with_extension);
        }

    }

    private static void processResources(PDDocument document, PDResources resources) throws IOException {
        for (COSName name : resources.getXObjectNames()) {
            PDXObject xobject = resources.getXObject(name);
            if (xobject instanceof PDFormXObject) {
                PDFormXObject formXObject = (PDFormXObject) xobject;
                writeTokensToStream(formXObject.getContentStream(),
                        createTokensWithoutText(document, formXObject));
                processResources(document, formXObject.getResources());
            }
        }
        for (COSName name : resources.getPatternNames()) {
            PDAbstractPattern pattern = resources.getPattern(name);
            if (pattern instanceof PDTilingPattern) {
                PDTilingPattern tilingPattern = (PDTilingPattern) pattern;
                writeTokensToStream(tilingPattern.getContentStream(),
                        createTokensWithoutText(document, tilingPattern));
                processResources(document, tilingPattern.getResources());
            }
        }
    }

    private static void writeTokensToStream(PDStream newContents, List<Object> newTokens) throws IOException {
        try (OutputStream out = newContents.createOutputStream(COSName.FLATE_DECODE)) {
            ContentStreamWriter writer = new ContentStreamWriter(out);
            writer.writeTokens(newTokens);
        }
    }

    private static List<Object> createTokensWithoutText(PDDocument document, PDContentStream contentStream) throws IOException {
        PDFStreamParser parser = new PDFStreamParser(new PDStream(document, contentStream.getContents()));
        Object token = parser.parseNextToken();
        List<Object> newTokens = new ArrayList<>();
        while (token != null) {
            if (token instanceof Operator) {
                Operator op = (Operator) token;
                if ("TJ".equals(op.getName())
                        || "Tj".equals(op.getName())
                        || "'".equals(op.getName())) {
                    // remove the argument to this operator
                    newTokens.remove(newTokens.size() - 1);

                    token = parser.parseNextToken();
                    continue;
                } else if ("\"".equals(op.getName())) {
                    // remove the 3 arguments to this operator
                    newTokens.remove(newTokens.size() - 1);
                    newTokens.remove(newTokens.size() - 1);
                    newTokens.remove(newTokens.size() - 1);

                    token = parser.parseNextToken();
                    continue;
                }
            }
            newTokens.add(token);
            token = parser.parseNextToken();
        }
        return newTokens;
    }

    /**
     * This will print the usage for this document.
     */
    private static void usage() {
        System.err.println("Usage: java " + RemoveAllTextFromPDF.class.getName() + " <input-pdf> <output-pdf>");
    }

   

   

}
