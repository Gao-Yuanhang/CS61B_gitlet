package gitlet;

import jdk.jshell.execution.Util;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Repository implements Serializable {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    /** simulating the staging area, separately, record files to add and to remove*/
    public static final File STAGING_ADD_DIR = join(GITLET_DIR, "staging_addition");
    public static final File STAGING_RM_DIR = join(GITLET_DIR, "staging_removal");

    /** serialization on the Repository object, its instance variables(e.g. Commits) will be serialized recursively*/
    public static final File REPOINFO_DIR = join(GITLET_DIR, "repo_info");

    /** store all the blobs, add suffix '_ + number' to distinguish different versions*/
    public static final File BLOB_DIR = join(GITLET_DIR, "blobs");

    public Commit head;

    public ArrayList<Branch> branches = new ArrayList<>();

    public Branch currentBranch;

    /** collect all the commits, firstly, it can avoid the un-referred commit objects to be recycled*/
    public HashSet<Commit> commits = new HashSet<>();

    /** get the current commit for this repository*/
    public Commit getHead(){
        return head;
    }

    public void addBranch(String branchName, Commit currentCommit){
        branches.add(new Branch(branchName, currentCommit));
    }

    public void setCurrentBranch(String branchName){
        for(Branch branch : branches){
            if(branch.name.equals(branchName)){
                currentBranch = branch;
                return;
            }
        }
        throw new GitletException("no such branch");
    }

    /** called when committing, head for the repository and for branch both need to change
     * 'this.head = this.currentBranch.currentCommit' is an invariant*/
    public void setHead(Commit nextCommit){
        this.currentBranch.currentCommit = nextCommit;
        this.head = this.currentBranch.currentCommit;
    }

    public void initRepository(Commit rootCommit){
        Repository.GITLET_DIR.mkdirs();
        Repository.STAGING_ADD_DIR.mkdirs();
        Repository.STAGING_RM_DIR.mkdirs();
        Repository.BLOB_DIR.mkdirs();
        this.addBranch("master", rootCommit);
        this.setCurrentBranch("master");
        this.head = rootCommit;
        this.commits.add(rootCommit);
    }

    public void storeRepo(){
        Utils.writeObject(REPOINFO_DIR, this);
    }

    public static Repository recoverRepo(){
        /** by reflection*/
        return Utils.readObject(REPOINFO_DIR, Repository.class);
    }

    public void add(File fileToAdd){
        // TODO: as a simplification in the instruction, do not resolve the subdirectories?
        List<String> plainFileNames = Utils.plainFilenamesIn(this.STAGING_ADD_DIR);
        String fileInStaging = Utils.findString(plainFileNames, fileToAdd.getName());
        //phase1, add or overwrite in the staging area for addition
        if(fileInStaging != null){
            join(STAGING_ADD_DIR, fileInStaging).delete();
        }
        //write in the form of byte stream
        File newFile = join(STAGING_ADD_DIR, fileToAdd.getName());
        try {
            newFile.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        writeContents(newFile, Utils.readContents(fileToAdd));
        //phase2, check if the file is identical to the current commit version, if it is, remove from the staging area
        File fileInCommit = head.findBlobFile(fileToAdd.getName());
        if(fileInCommit != null){
            if(Utils.checkFilesDifference(newFile, fileInCommit)){
                newFile.delete();
            }
        }
        // TODO: for the file staged for removal
    }

    public void Commit(String message){
        //resolve failure cases
        if(Utils.plainFilenamesIn(STAGING_ADD_DIR).isEmpty() && Utils.plainFilenamesIn(STAGING_RM_DIR).isEmpty()){
            System.err.println("No changes added to the commit.");
            System.exit(0);
        }
        //basic information for new commit
        Commit newCommit = new Commit();
        newCommit.message = message;
        newCommit.timestamp = new Date();
        newCommit.parentCommits.add(head);
        //update the blobs and tracked file names
        newCommit.tracked_file_names = head.tracked_file_names;
        newCommit.blobs = head.blobs;
        for(String fileName : Utils.plainFilenamesIn(STAGING_ADD_DIR)){
            if(head.tracked_file_names.contains(fileName)){
                File currentVersionFile = head.findBlobFile(fileName);
                newCommit.blobs.remove(currentVersionFile);
                String currentVersionFileName = currentVersionFile.getName();
                int versionNum = Integer.valueOf(currentVersionFileName.substring(currentVersionFileName.lastIndexOf("_")));
                versionNum++;
                //generate a new version of the file, and replace the old version
                File newBlob = join(BLOB_DIR, fileName+"_"+String.valueOf(versionNum));
                Utils.writeContents(newBlob, readContents(join(STAGING_ADD_DIR, fileName)));
                newCommit.blobs.add(newBlob);
            }else{
                newCommit.tracked_file_names.add(fileName);
                File newBlob = join(BLOB_DIR, fileName+"_"+String.valueOf(0));
                Utils.writeContents(newBlob, readContents(join(STAGING_ADD_DIR, fileName)));
                newCommit.blobs.add(newBlob);
            }
        }
        //resolve 'rm'
        for(String fileName : Utils.plainFilenamesIn(STAGING_RM_DIR)){
            if(head.tracked_file_names.contains(fileName)){
                File currentVersionFile = head.findBlobFile(fileName);
                newCommit.blobs.remove(currentVersionFile);
                newCommit.tracked_file_names.remove(fileName);
            }else{
                // TODO: should be an error?
            }
        }
        //update head
        this.setHead(newCommit);
        newCommit.CalculateID();
        this.commits.add(newCommit);
        //clear the staging area
        for(String fileName : Utils.plainFilenamesIn(STAGING_ADD_DIR)){
            join(STAGING_ADD_DIR, fileName).delete();
        }
        for(String fileName : Utils.plainFilenamesIn(STAGING_RM_DIR)){
            join(STAGING_RM_DIR, fileName).delete();
        }
    }

    public void log(){
        Commit current = this.head;
        do{
            Utils.printCommit(current);
            current = current.parentCommits.get(0);
        }while(current.parentCommits != null);
        //TODO in the document, a commit has its successor, but why can the log command can be executed here?
    }

    public void global_log(){
        for(Commit commit : this.commits){
            Utils.printCommit(commit);
        }
    }

    public void find(String message){
        for(Commit commit : this.commits){
            if(commit.message.equals(message)){
                System.out.println(commit.ID);
            }
        }
    }

    public void status(){
        System.out.println("=== Branches ===");
        List<String> branches = this.branches.stream().map(Branch::getName).sorted().collect(Collectors.toList());
        branches.stream().forEach(s -> {
            if(s.equals(currentBranch.name)){
                s = "*" + s;
            }
            System.out.println(s);
        });
        System.out.println("");

        System.out.println("=== Staged Files ===");
        Utils.plainFilenamesIn(STAGING_ADD_DIR).stream()
                                               .sorted()
                                               .forEach(System.out::println);
        System.out.println("");

        System.out.println("=== Removed Files ===");
        Utils.plainFilenamesIn(STAGING_RM_DIR).stream()
                .sorted()
                .forEach(System.out::println);
        System.out.println("");

        /** TODO bonus*/
        /** TODO end*/

        System.out.println("");
    }
}
