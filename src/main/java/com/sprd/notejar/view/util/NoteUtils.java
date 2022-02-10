package com.sprd.notejar.view.util;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.StringReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by danny.liu on 2017/4/24.
 */

public class NoteUtils {

    private static final String TAG = "NoteUtils";

    /**
     *Construct a Document.
     * @param xml It's a xml style string.
     * @return
     */
    public static Document string2Doc(String xml) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        Document doc = null;
        InputSource source = null;
        StringReader reader = null;
        try {
            builder = factory.newDocumentBuilder();
            reader = new StringReader(xml);
            source = new InputSource(reader);
            doc = builder.parse(source);
            return doc;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    /**
     *Get content for note, but exclude image path. used to search.
     * @param content It's a xml style string.
     * @return
     */
    public static String parseXml (String content) {
        /*UNISOC: Modify for bug 1235490 @{*/
        StringBuffer str = new StringBuffer();
        Document doc = string2Doc(content);
        if (doc == null){
            return null;
        }
        Element rootElement = doc.getDocumentElement();
        NodeList list = rootElement.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            Log.d(TAG, "Node === " + n.getNodeName() + " : text = " + n.getTextContent());
            if (n.getNodeName().equals("notetxt") || n.getNodeName().equals("notetodo")) {
                str.append(n.getTextContent());
            }
        }
        return str.toString();
        /*}@*/
    }

    public static void deleteDirOrFile(File f){
        if (f.isDirectory()){
            File files [] = f.listFiles();
            for (int i = 0; i < files.length; i++) {
                /* UNISOC : Modify for bug 1235479 @{ */
                if (!files[i].delete()) {
                    Log.e(TAG, "files[" + i + "] = " + files[i] + "delete failed.");
                }
            }
        } else {
            if (!f.delete()) {
                Log.e(TAG, f + " delete failed.");
            }
            /* }@ */
        }
    }

    public static  int getOffset (int position, ArrayList<NoteCategory> list) {
        if (list != null && list.size() != 0) {
             /*Modified for bug 741244 @{*/
            //Collections.sort(list, new NoteCategoryComparator());
            /*}@*/
            int all = 0;
            int count = list.size();
            boolean isContainCurrent = false;
            for (int i = 0; i < count; i++) {
                int year = Integer.parseInt(list.get(i).getHeaderTitle());
                int items =( list.get(i)).getItemCounts();
                all += items;
                Log.d(TAG, "i = ="+i+": postion == "+position+ ": all = "+all);
                if (year == Integer.parseInt(NoteCategory.getYear(new Date(System.currentTimeMillis())))) {
                    isContainCurrent = true;
                }
                /*Modified for bug 741142 @{*/
                if (position < all) {
                    if (isContainCurrent) {
                        Log.d(TAG, "getOffset.....i="+i);
                        return i;
                    } else{
                        Log.d(TAG, "!isContainCurrent getOffset...i="+i);
                        return i+1;
                    }
                }
                /*}@*/

            }
        }
        return -1;
    }
}

//UNISOC: Modify for  bug 1235573
class NoteCategoryComparator implements Comparator, Serializable {

    public int compare(Object o1,Object o2) {
        NoteCategory nc1=(NoteCategory) o1;
        NoteCategory nc2=(NoteCategory)o2;
        if(Integer.parseInt(nc1.getHeaderTitle()) > Integer.parseInt(nc2.getHeaderTitle())) {
            return 1;
        } else {
            return 0;
        }
    }
}
