package gitlet;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;


/** Assorted utilities.
 *
 * Give this file a good read as it provides several useful utility functions
 * to save you some time.
 *
 *  @author P. N. Hilfinger
 */
class Utils {

    /** The length of a complete SHA-1 UID as a hexadecimal numeral. */
    static final int UID_LENGTH = 40;

    /* SHA-1 HASH VALUES. */

    /** Returns the SHA-1 hash of the concatenation of VALS, which may
     *  be any mixture of byte arrays and Strings. */
    static String sha1(Object... vals) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            for (Object val : vals) {
                if (val instanceof byte[]) {
                    md.update((byte[]) val);
                } else if (val instanceof String) {
                    md.update(((String) val).getBytes(StandardCharsets.UTF_8));
                } else {
                    throw new IllegalArgumentException("improper type to sha1");
                }
            }
            Formatter result = new Formatter();
            for (byte b : md.digest()) {
                result.format("%02x", b);
            }
            return result.toString();
        } catch (NoSuchAlgorithmException excp) {
            throw new IllegalArgumentException("System does not support SHA-1");
        }
    }

    /** Returns the SHA-1 hash of the concatenation of the strings in
     *  VALS. */
    static String sha1(List<Object> vals) {
        return sha1(vals.toArray(new Object[vals.size()]));
    }

    /* FILE DELETION */

    /** Deletes FILE if it exists and is not a directory.  Returns true
     *  if FILE was deleted, and false otherwise.  Refuses to delete FILE
     *  and throws IllegalArgumentException unless the directory designated by
     *  FILE also contains a directory named .gitlet. */
    static boolean restrictedDelete(File file) {
        if (!(new File(file.getParentFile(), ".gitlet")).isDirectory()) {
            throw new IllegalArgumentException("not .gitlet working directory");
        }
        if (!file.isDirectory()) {
            return file.delete();
        } else {
            return false;
        }
    }

    /** Deletes the file named FILE if it exists and is not a directory.
     *  Returns true if FILE was deleted, and false otherwise.  Refuses
     *  to delete FILE and throws IllegalArgumentException unless the
     *  directory designated by FILE also contains a directory named .gitlet. */
    static boolean restrictedDelete(String file) {
        return restrictedDelete(new File(file));
    }

    /* READING AND WRITING FILE CONTENTS */

    /** Return the entire contents of FILE as a byte array.  FILE must
     *  be a normal file.  Throws IllegalArgumentException
     *  in case of problems. */
    static byte[] readContents(File file) {
        if (!file.isFile()) {
            throw new IllegalArgumentException("must be a normal file");
        }
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    /** Return the entire contents of FILE as a String.  FILE must
     *  be a normal file.  Throws IllegalArgumentException
     *  in case of problems. */
    static String readContentsAsString(File file) {
        return new String(readContents(file), StandardCharsets.UTF_8);
    }

    /** Write the result of concatenating the bytes in CONTENTS to FILE,
     *  creating or overwriting it as needed.  Each object in CONTENTS may be
     *  either a String or a byte array.  Throws IllegalArgumentException
     *  in case of problems. */
    static void writeContents(File file, Object... contents) {
        try {
            if (file.isDirectory()) {
                throw
                    new IllegalArgumentException("cannot overwrite directory");
            }
            BufferedOutputStream str =
                new BufferedOutputStream(Files.newOutputStream(file.toPath()));
            for (Object obj : contents) {
                if (obj instanceof byte[]) {
                    str.write((byte[]) obj);
                } else {
                    str.write(((String) obj).getBytes(StandardCharsets.UTF_8));
                }
            }
            str.close();
        } catch (IOException | ClassCastException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    /** Return an object of type T read from FILE, casting it to EXPECTEDCLASS.
     *  Throws IllegalArgumentException in case of problems. */
    static <T extends Serializable> T readObject(File file,
                                                 Class<T> expectedClass) {
        try {
            ObjectInputStream in =
                new ObjectInputStream(new FileInputStream(file));
            T result = expectedClass.cast(in.readObject());
            in.close();
            return result;
        } catch (IOException | ClassCastException
                 | ClassNotFoundException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    /** Write OBJ to FILE. */
    static void writeObject(File file, Serializable obj) {
        writeContents(file, serialize(obj));
    }

    /* DIRECTORIES */

    /** Filter out all but plain files. */
    private static final FilenameFilter PLAIN_FILES =
        new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isFile();
            }
        };

    /** Returns a list of the names of all plain files in the directory DIR, in
     *  lexicographic order as Java Strings.  Returns null if DIR does
     *  not denote a directory. */
    static List<String> plainFilenamesIn(File dir) {
        String[] files = dir.list(PLAIN_FILES);
        if (files == null) {
            return null;
        } else {
            Arrays.sort(files);
            return Arrays.asList(files);
        }
    }

    /** Returns a list of the names of all plain files in the directory DIR, in
     *  lexicographic order as Java Strings.  Returns null if DIR does
     *  not denote a directory. */
    static List<String> plainFilenamesIn(String dir) {
        return plainFilenamesIn(new File(dir));
    }

    /* OTHER FILE UTILITIES */


    static File join(String first, String... others) {
        return Paths.get(first, others).toFile();
    }


    static File join(File first, String... others) {
        return Paths.get(first.getPath(), others).toFile();
    }


    /* SERIALIZATION UTILITIES */

    /** Returns a byte array containing the serialized contents of OBJ. */
    static byte[] serialize(Serializable obj) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(stream);
            objectStream.writeObject(obj);
            objectStream.close();
            return stream.toByteArray();
        } catch (IOException excp) {
            throw error("Internal error serializing commit.");
        }
    }



    /* MESSAGES AND ERROR REPORTING */

    /** Return a GitletException whose message is composed from MSG and ARGS as
     *  for the String.format method. */
    static GitletException error(String msg, Object... args) {
        return new GitletException(String.format(msg, args));
    }

    /** Print a message composed from MSG and ARGS as for the String.format
     *  method, followed by a newline. */
    static void message(String msg, Object... args) {
        System.out.printf(msg, args);
        System.out.println();
    }

    /** lgN by bisection, input: an ordered list, output: null represents no result*/
    static String findString(List<String> stringList, String target){
        if(stringList.size() == 0){
            return null;
        }
        int left = 0;
        int right = stringList.size()-1;
        while(left != right){
            int mid = (int)((left + right)/2);
            if(stringList.get(mid).compareTo(target) < 0){
                /** mid+-1, otherwise may lead to a dead loop*/
                left = mid+1;
            }else if(stringList.get(mid).compareTo(target) > 0){
                right = mid-1;
            }
        }
        if(stringList.get(left).equals(target)){
            return target;
        }else{
            return null;
        }

    }

    /** when same, return true*/
    static boolean checkFilesDifference(File file1, File file2){
        byte [] b1 = Utils.readContents(file1);
        byte [] b2 = Utils.readContents(file2);
        if(b2.length != b1.length){
            return false;
        }
        for(int i=0; i<b1.length; i++){
            if(b2[i]!=b1[i]){
                return false;
            }
        }
        return true;
    }

    static void printCommit(Commit current){
        System.out.println("===");
        System.out.println("commit " + current.ID);

        //merge information, first parent is the branch where you do the merge; 'git merge branchA' meaning merge branchA into current branch
        if(current.parentCommits.size() >= 2){
            Commit c1 = current.parentCommits.get(0);
            Commit c2 = current.parentCommits.get(1);
            System.out.println("Merge: " + c1.ID.substring(0,7) + " " + c2.ID.substring(0,7));
        }

        //show the time of current time zone instead of standard time zone
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", new Locale("ENGLISH"));
        sdf.setTimeZone(TimeZone.getDefault());
        System.out.println("Date: " + sdf.format(current.timestamp));
        System.out.println(current.message);
        System.out.println("");
    }

    static void clearDirectory(File f){
        if(!f.isDirectory()){
            System.out.println("error in clearDirectory");
        }
        for(File file : f.listFiles()){
            file.delete();
        }
    }

    static void writeFile(File file, String content){
        try (FileWriter fw = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void clearFile(File file){
        try (FileWriter fw = new FileWriter(file, false);
            BufferedWriter bw = new BufferedWriter(fw)) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void appendFile(File source, File target) {
        try (InputStream in = new FileInputStream(source);
             OutputStream out = Files.newOutputStream(
                     target.toPath(),
                     StandardOpenOption.CREATE,
                     StandardOpenOption.APPEND)) {

            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }

        } catch (IOException e) {
            throw new RuntimeException("Error appending file content", e);
        }
    }

    /** given the targetName(filename without suffix), return the largest index of blobs in this repository
     * if not tracked by any exist commits, return -1*/
    public static int findMaxIndexVersion(String targetname){
        int max = -1;
        for (File f : Repository.BLOB_DIR.listFiles()){
            String nameWithSuffix = f.getName();
            String nameWithoutSuffix = nameWithSuffix.substring(0, nameWithSuffix.lastIndexOf("_"));
            int version = Integer.valueOf(nameWithSuffix.substring(nameWithSuffix.lastIndexOf("_")+1));
            if(nameWithoutSuffix.equals(targetname))
                max = version;
        }
        return max;
    }

}
