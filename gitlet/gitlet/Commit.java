package gitlet;

// TODO: any imports you need here

import com.sun.source.tree.Tree;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date; // TODO: You'll likely use this in this class
import java.util.TreeSet;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Commit implements Serializable {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    public String message;

    public long timestamp;

    /** for the unique init commit, it points to null*/
    public Commit parentCommit;

    /** when generate a new commit, set for the current commit*/
    public Commit childCommit;

    /** use TreeSet to assure get the same ID using sha algorithm*/
    /** reference to the saved contents of files*/
    public TreeSet<File> blobs = new TreeSet<>();

    /** names of the files being tracked, corresponding to field 'blobs'*/
    public TreeSet<String> tracked_file_names = new TreeSet<>();

    public String ID;

    /** all commits in all repositories will trace back to it*/
    public static Commit RootCommit(){
        return RootCommit.getRootCommit();
    }

    /** using the singleton pattern*/
    private static class RootCommit extends Commit{
        private static RootCommit rootCommit;

        private RootCommit(){}

        public static RootCommit getRootCommit() {
            if(rootCommit == null){
                rootCommit = new RootCommit();
                rootCommit.timestamp = 0;
                rootCommit.message = "initial commit";

                rootCommit.ID = Utils.sha1(rootCommit);
            }
            return rootCommit;
        }
    }

    /** given a string file name, find the specific blob corresponding to it in this commit*/
    public File findFile(String fileName){
        if(this.tracked_file_names.contains(fileName)){
            for(File f : this.blobs){
                String nameWithPrefix = f.getName();
                String nameWithoutPrefix = nameWithPrefix.substring(0, nameWithPrefix.lastIndexOf("_"));
                if(nameWithoutPrefix.equals(fileName)){
                    return f;
                }
            }
        }
        return null;
    }

    /* TODO: fill in the rest of this class. */
}
