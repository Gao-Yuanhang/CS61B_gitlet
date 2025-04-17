package gitlet;

// TODO: any imports you need here

import java.io.File;
import java.io.Serializable;
import java.util.*;

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

    /** for the unique init commit, it points to null, when merging branches, one commit can have more than 1 parentCommit*/
    /** it is ordered, first parent, second parent...*/
    public ArrayList<Commit> parentCommits = new ArrayList<>();

    /** use TreeSet to assure get the same ID using sha algorithm*/
    /** reference to the saved contents of files*/
    public TreeSet<File> blobs = new TreeSet<>();

    /** names of the files being tracked, corresponding to field 'blobs'*/
    public TreeSet<String> tracked_file_names = new TreeSet<>();

    public String ID;

    /** date*/
    public Date timestamp;

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
                rootCommit.timestamp = new Date();
                rootCommit.message = "initial commit";
            }
            return rootCommit;
        }
    }

    /** given a string file name, find the specific blob corresponding to it in this commit*/
    public File findBlobFile(String fileName){
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

    public void CalculateID(){
        //SHA-1 ID must include the file references, parent reference, log message and commit time
        // TODO: what if parentCommit be null, how to convert long type(timestamp) to byte[]
        this.ID = Utils.sha1(Utils.serialize(blobs), Utils.serialize(parentCommits), message, String.valueOf(timestamp));
    }

    /** find the splitPoint(the latest common ancestor) of current head and givenCommit(the head of the given branch)*/
    public Commit findSplitPoint(Commit givenCommit){
        HashSet<Commit> currentAncestors = this.findAncestors();
        HashSet<Commit> givenAncestors = givenCommit.findAncestors();
        currentAncestors.retainAll(givenAncestors);
        Commit splitPoint = null;
        for(Commit c : currentAncestors){
            if(splitPoint == null)
                splitPoint = c;
            if(c.timestamp.after(splitPoint.timestamp))
                splitPoint = c;
        }
        return splitPoint;
    }


    /** use a dfs traverse, instead of a linkedList, the reason refers to the example in the document.*/
    private HashSet<Commit> findAncestors(){
        HashSet<Commit> result = new HashSet<>();
        Stack<Commit> stack = new Stack<>();
        stack.push(this);
        while(!stack.empty()){
            Commit temp = stack.pop();
            result.add(temp);
            for(Commit c : temp.parentCommits){
                stack.push(c);
            }
        }
        return result;
    }

    /** check if a file is modified in current commit compared with the given commit; precondition: the file is tracked by current commit.*/
    public boolean fileModifiedFrom(String fileName, Commit ancestor){
        if(!ancestor.tracked_file_names.contains(fileName)){
            //the file is added
            return true;
        }
        int oldVersion = ancestor.getVersion(fileName);
        int newVersion = this.getVersion(fileName);
        assert (oldVersion <= newVersion);
        return (oldVersion < newVersion);
    }

    /** get the version of a file in the blob for this commit, precondition: the file is tracked by current commit*/
    public int getVersion(String fileName){
        for(File f : this.blobs){
            String nameWithPrefix = f.getName();
            String nameWithoutPrefix = nameWithPrefix.substring(0, nameWithPrefix.lastIndexOf("_"));
            if(nameWithoutPrefix.equals(fileName)){
                int version = Integer.valueOf(nameWithPrefix.substring(nameWithPrefix.lastIndexOf("_")+1));
                return version;
            }
        }
        return -10;
    }
}
