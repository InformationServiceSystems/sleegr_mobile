/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.android.wearable.datalayer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

/**
 * @author Iaroslav
 */
public class Serializer {

    public static void SerializeToFile(Object obj, File file) throws IOException {

        FileOutputStream fileOut;


        if (!file.exists()) {
            //file.mkdirs();
            file.createNewFile();
        }

        fileOut = new FileOutputStream(file);
        ObjectOutputStream out = new ObjectOutputStream(fileOut);
        out.writeObject(obj);
        out.close();
        fileOut.close();


    }

    public static Object DeserializeFromFile(File file) throws FileNotFoundException, IOException, ClassNotFoundException {

        Object result = null;

        FileInputStream fileIn;

        fileIn = new FileInputStream(file);
        ObjectInputStream in = new ObjectInputStream(fileIn);
        result = in.readObject();
        in.close();
        fileIn.close();

        return result;
    }

    public static byte[] SerializeToBytes(Object object) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(object);
            return bos.toByteArray();
        }
    }

    public static Object DeserializeFromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInput in = new ObjectInputStream(bis)) {
            return in.readObject();
        }
    }

}
