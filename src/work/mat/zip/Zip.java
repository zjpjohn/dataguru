package work.mat.zip;

/*
 * 将数据压缩归档入一ZIP文件
 * http://www.ibm.com/developerworks/cn/java/l-compress/
 */

import java.io.*;
import java.util.zip.*;
public class Zip {
   static final int BUFFER = 2048;
   public static void main (String argv[]) {
      try {
         BufferedInputStream origin = null;
         FileOutputStream dest = new 
           FileOutputStream("c:\\files.zip");
         ZipOutputStream out = new ZipOutputStream(new 
           BufferedOutputStream(dest));
         //out.setMethod(ZipOutputStream.DEFLATED);
         byte data[] = new byte[BUFFER];
         // get a list of files from current directory
         File f = new File(".");
         String files[] = f.list();
         for (int i=0; i < files.length; i++) {
            System.out.println("Adding: "+files[i]);
            FileInputStream fi = new FileInputStream(files[i]);
            origin = new BufferedInputStream(fi, BUFFER);
            ZipEntry entry = new ZipEntry(files[i]);
            out.putNextEntry(entry);
            int count;
            while((count = origin.read(data, 0,BUFFER)) != -1) {
               out.write(data, 0, count);
            }
            origin.close();
         }
         out.close();
      } catch(Exception e) {
         e.printStackTrace();
      }
   }
}
