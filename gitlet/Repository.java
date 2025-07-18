package gitlet;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static gitlet.Utils.*;

/** Represents a gitlet repository.
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

    public ArrayList<Branch> branches = new ArrayList<>();

    public Branch currentBranch;

    /** collect all the commits, firstly, it can avoid the un-referred commit objects to be recycled*/
    public HashSet<Commit> commits = new HashSet<>();

    /** get the current commit for this repository*/
    public Commit getHead(){
        return currentBranch.currentCommit;
    }

    public void addBranch(String branchName, Commit currentCommit){
        branches.add(new Branch(branchName, currentCommit));
    }

    /** called when creating or checking out branch*/
    public void setCurrentBranch(String branchName){
        for(Branch branch : branches){
            if(branch.name.equals(branchName)){
                currentBranch = branch;
                return;
            }
        }
        throw new GitletException("no such branch");
    }

    /** called when committing*/
    public void setHead(Commit nextCommit){
        this.currentBranch.currentCommit = nextCommit;
    }

    public void initRepository(Commit rootCommit){
        Repository.GITLET_DIR.mkdirs();
        Repository.STAGING_ADD_DIR.mkdirs();
        Repository.STAGING_RM_DIR.mkdirs();
        Repository.BLOB_DIR.mkdirs();
        this.addBranch("master", rootCommit);
        this.setCurrentBranch("master");
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
        File fileInCommit = getHead().findBlobFile(fileToAdd.getName());
        if(fileInCommit != null){
            if(Utils.checkFilesDifference(newFile, fileInCommit)){
                newFile.delete();
            }
        }
        //unmark the file (if it is) in the removal staging area
        for(File f : STAGING_RM_DIR.listFiles()){
            if(f.getName().equals(fileToAdd.getName())){
                f.delete();
            }
        }
    }

    public void commit(String message){
        //resolve failure cases
        if(Utils.plainFilenamesIn(STAGING_ADD_DIR).isEmpty() && Utils.plainFilenamesIn(STAGING_RM_DIR).isEmpty()){
            System.err.println("No changes added to the commit.");
            System.exit(0);
        }
        //basic information for new commit
        Commit newCommit = new Commit();
        newCommit.message = message;
        newCommit.timestamp = new Date();
        newCommit.parentCommits.add(getHead());
        //when inherit [tracked_file_names] and [blobs] from parent commit
        //a copy construction instead of a (reference) assignment is needed!
        newCommit.tracked_file_names = new TreeSet<>(getHead().tracked_file_names);
        newCommit.blobs = new TreeSet<>(getHead().blobs);

        /** important!
         * the index(e.g. '0' in wug.txt_0) is important, we can not just add 1 based on the parent version(because there can be multiple branches)
         * the file is in a staging add dir, so a new version blob must be added(current max index + 1, no matter max is -1 or something)
         * then the old version blob should be removed(if it is inherited from the parent commit
         * in addition ,the index indicates the commit order(but maybe this feature is not used)*/
        for(String fileName : Utils.plainFilenamesIn(STAGING_ADD_DIR)){
            int maxVersionNum = Utils.findMaxIndexVersion(fileName);
            maxVersionNum++;
            //generate a new version of the file, and replace the old version
            File newBlob = join(BLOB_DIR, fileName+"_"+String.valueOf(maxVersionNum));
            Utils.writeContents(newBlob, readContents(join(STAGING_ADD_DIR, fileName)));
            newCommit.blobs.add(newBlob);
            //add to tracked name set anyway
            newCommit.tracked_file_names.add(fileName);
            //remove the old version if its parent commit also tracked this file
            if(getHead().tracked_file_names.contains(fileName)) {
                File oldversionFile = getHead().findBlobFile(fileName);
                newCommit.blobs.remove(oldversionFile);
            }
        }
        //resolve 'rm'
        for(String fileName : Utils.plainFilenamesIn(STAGING_RM_DIR)){
            if(getHead().tracked_file_names.contains(fileName)){
                File currentVersionFile = getHead().findBlobFile(fileName);
                newCommit.blobs.remove(currentVersionFile);
                newCommit.tracked_file_names.remove(fileName);
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

    public void rm(String fileName){
        //flag indicating if some operations are done
        boolean flag = false;
        for(File f : STAGING_ADD_DIR.listFiles()){
            if(f.getName().equals(fileName)){
                f.delete();
                flag = true;
            }
            //according to the description, if the file is staged but not tracked, it will not be deleted from CWD, only the copy in staging area will get deleted
        }
        //if it is neither staged nor tracked by head print error
        if(!getHead().tracked_file_names.contains(fileName) && !flag){
            System.err.println("No reason to remove the file.");
            System.exit(0);
        }
        //only when the file is tracked in the current commit
        if(getHead().tracked_file_names.contains(fileName)){
            //just use the file to record the name of files to remove, no contents will be written
            File f = join(STAGING_RM_DIR, fileName);
            if(!f.exists()){
                try {
                    f.createNewFile();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            File fileInCWD = join(CWD, fileName);
            if(fileInCWD.exists())
                fileInCWD.delete();
        }
    }

    public void log(){
        Commit current = getHead();
        do{
            Utils.printCommit(current);
            current = current.parentCommits.get(0);
        }while(current.parentCommits.size() != 0);
        Utils.printCommit(current);
    }

    public void global_log(){
        for(Commit commit : this.commits){
            Utils.printCommit(commit);
        }
    }

    public void find(String message){
        int num = 0;//the num of matched commits
        for(Commit commit : this.commits){
            if(commit.message.equals(message)){
                System.out.println(commit.ID);
                num++;
            }
        }
        //failure case
        if(num == 0){
            System.out.println("Found no commit with that message.");
            System.exit(0);
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
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println("");

        System.out.println("=== Untracked Files ===");
        System.out.println("");

        /** TODO end*/

    }

    public void checkout_files(String filename){
        File blobFile = this.getHead().findBlobFile(filename);
        aux_checkout(filename, blobFile, false);
    }

    public void checkout_files_byID(String ID, String filename){
        Commit c = findCommitByShortID(ID);
        File blobFile = c.findBlobFile(filename);
        aux_checkout(filename, blobFile, false);
    }

    /** filename- the original filename; blobFile- corresponding file in the blobs
     *  the 'errorMessageNeeded' field means that if an error message 'There is an untracked file in the way' is wanted when a file is overwritten
     *  in the requirements, if the operation is oriented to a single file(e.g. checkout a file/a file by a commit ID), such message is not needed*/
    private void aux_checkout(String filename, File blobFile, boolean errorMessageNeeded) {
        if(blobFile == null){
            System.err.println("File does not exist in that commit.");
            System.exit(0);
        }else{
            File fileToWrite = join(CWD, filename);
            //overwrite
            if(fileToWrite.exists()){
                //if it is untracked and will be overwritten, an error will be present
                //TODO but I'm not sure what 'untracked' means
                //just take it as the most simple way, if it is not tracked in the HEAD
                if(!getHead().tracked_file_names.contains(filename) && errorMessageNeeded){
                    System.err.println("There is an untracked file in the way; delete it, or add and commit it first.");
                    System.exit(0);
                }
                fileToWrite.delete();
            }
            Utils.writeContents(fileToWrite, readContents(blobFile));
        }
    }

    public void checkout_branch(String branchName){
        if(currentBranch.name.equals(branchName)){
            System.err.println("No need to checkout the current branch.");
            System.exit(0);
        }
        for(Branch b : branches){
            if (b.name.equals(branchName)){
                //checkout of files tracked in this commit
                Commit commit = b.currentCommit;
                checkoutCommit(commit);
                //switch the branch
                setCurrentBranch(branchName);
                //clear the staging area
                Utils.clearDirectory(STAGING_ADD_DIR);
                Utils.clearDirectory(STAGING_RM_DIR);
                return;
            }
        }
        System.err.println("No such branch exists.");
        System.exit(0);
    }

    /** it will only operate the files, excluding the metadata of the repository(e.g. branch, staging area..*/
    public void checkoutCommit(Commit commit){
        //load the files in the head of the new branch
        for(File blobFile : commit.blobs){
            String nameWithSuffix = blobFile.getName();
            String nameWithoutSuffix = nameWithSuffix.substring(0, nameWithSuffix.lastIndexOf("_"));
            aux_checkout(nameWithoutSuffix, blobFile, true);
        }
        //if a file is tracked in the current branch but not present in the given commit, it should be removed
        for(File f : CWD.listFiles()){
            if(getHead().tracked_file_names.contains(f.getName()) && !commit.tracked_file_names.contains(f.getName())){
                f.delete();
            }
        }
    }

    public void branch(String branchName){
        if(branches.stream().map(Branch::getName).collect(Collectors.toSet()).contains(branchName)){
            System.err.println("A branch with that name already exists.");
            System.exit(0);
        }
        branches.add(new Branch(branchName, getHead()));
    }

    public void rm_branch(String branchName){
        if(branchName.equals(currentBranch.name)){
            System.err.println("Cannot remove the current branch.");
            System.exit(0);
        }
        for(Branch b : branches){
            if(b.name.equals(branchName)){
                branches.remove(b);
                return;
            }
        }
        System.err.println("A branch with that name does not exist.");
        System.exit(0);
    }

    public void reset(String ID){
        Commit commit = findCommitByShortID(ID);
        checkoutCommit(commit);
        //set head for current branch
        currentBranch.currentCommit = commit;
        //clear the staging area
        Utils.clearDirectory(STAGING_ADD_DIR);
        Utils.clearDirectory(STAGING_RM_DIR);
    }

    /** Merges files from the given branch into the current branch.*/
    public void merge(String branchName){
        if(STAGING_ADD_DIR.listFiles().length!=0 || STAGING_RM_DIR.listFiles().length!=0){
            System.err.println("You have uncommitted changes.");
            System.exit(0);
        }
        Branch givenBranch = null;
        for(Branch b : branches){
            if(b.name.equals(branchName))
                givenBranch = b;
        }
        if(givenBranch == null){
            System.err.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if(currentBranch.getName().equals(branchName)){
            System.err.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        Commit givenCommit = givenBranch.currentCommit;
        Commit currentCommit = getHead();
        Commit splitPoint = currentCommit.findSplitPoint(givenCommit);
        if(splitPoint.equals(givenCommit)){
            //do nothing, because given branch is totally older than current branch
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        if(splitPoint.equals(currentCommit)){
            //checkout the given branch, because given commit is totally evolved based on current commit
            checkout_branch(branchName);
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        }
        //the else case, A as currentCommit, B as givenCommit, and will generate a new mergedCommit
        // *we will not construct a merged commit directly, instead, we just operate the staging area by 'add' and 'rm' commands, and call 'commit' finally
        // in this way, we can let the normal commit error message go through(e.g. when merge get no changes, no mergedCommit will be generated

        //Part 1: considering files in currentCommit
        for(String fileName : currentCommit.tracked_file_names){
            boolean modifiedByA = currentCommit.fileModifiedFrom(fileName, splitPoint);
            if(givenCommit.tracked_file_names.contains(fileName)){
                //cases that both modified or both added, it doesn't matter that if splitPoint has that file
                boolean modifiedByB = givenCommit.fileModifiedFrom(fileName, splitPoint);
                if(modifiedByA && modifiedByB){
                    File fileInA = currentCommit.findBlobFile(fileName);
                    File fileInB = givenCommit.findBlobFile(fileName);
                    if(Utils.checkFilesDifference(fileInA, fileInB)){
                        //case 3, do nothing
                    }else{
                        //write conflict file
                        File conflictFile = join(CWD, fileName);
                        Utils.clearFile(conflictFile);

                        Utils.writeFile(conflictFile, "<<<<<<< HEAD\n");
                        Utils.appendFile(currentCommit.findBlobFile(fileName), conflictFile);
                        Utils.writeFile(conflictFile, "=======\n");
                        Utils.appendFile(givenCommit.findBlobFile(fileName), conflictFile);
                        Utils.writeFile(conflictFile, ">>>>>>>\n");
                        add(conflictFile);
                        System.out.println("Encountered a merge conflict.");
                    }
                }else if(!modifiedByA && modifiedByB){
                    //case 1
                    checkout_files_byID(givenCommit.ID, fileName);
                    add(join(CWD, fileName));
                }else{
                    //only A modified or both A and B unmodified, do nothing
                }
            }else if(!givenCommit.tracked_file_names.contains(fileName) && splitPoint.tracked_file_names.contains(fileName)){
                //meaning B remove the file, depending on whether A is modified
                if(!modifiedByA){
                    //case 7, it should be absent after merging
                    rm(fileName);
                }else{
                    //conflict regarding a absent file(A modified while B removed)
                    //write conflict file
                    File conflictFile = join(CWD, fileName);
                    Utils.clearFile(conflictFile);

                    Utils.writeFile(conflictFile, "<<<<<<< HEAD\n");
                    Utils.appendFile(currentCommit.findBlobFile(fileName), conflictFile);
                    Utils.writeFile(conflictFile, "=======\n");
                    Utils.writeFile(conflictFile, ">>>>>>>\n");
                    add(conflictFile);
                    System.out.println("Encountered a merge conflict.");
                }
            }else{
                //B and splitPoint don't have the file, which is added by only A, do nothing
            }
        }

        //Part2: considering files not in currentCommit
        //HashSet<String> filesInBOrSplit = new HashSet<>(splitPoint.tracked_file_names);
        //filesInBOrSplit.addAll(givenCommit.tracked_file_names);
        for(String filename : givenCommit.tracked_file_names){
            if(currentCommit.tracked_file_names.contains(filename))
                continue;
            if(splitPoint.tracked_file_names.contains(filename) && givenCommit.tracked_file_names.contains(filename)){
                if(givenCommit.fileModifiedFrom(filename, splitPoint)){
                    //A deleted and B modified
                    File conflictFile = join(CWD, filename);
                    Utils.clearFile(conflictFile);

                    Utils.writeFile(conflictFile, "<<<<<<< HEAD\n");
                    Utils.writeFile(conflictFile, "=======\n");
                    Utils.appendFile(currentCommit.findBlobFile(filename), conflictFile);
                    Utils.writeFile(conflictFile, ">>>>>>>\n");
                    add(conflictFile);
                    System.out.println("Encountered a merge conflict.");
                }
            }else if(!splitPoint.tracked_file_names.contains(filename) && givenCommit.tracked_file_names.contains(filename)){
                //case 5
                checkout_files_byID(givenCommit.ID, filename);
                add(join(CWD, filename));
            }else{
                //meaning both A and B removed the file, do nothing
            }
        }
        commit("Merged "+ branchName +" into " + currentBranch.getName() + ".");

        getHead().parentCommits.add(givenCommit);
        getHead().CalculateID();
    }

    //the input ID that may be abbreviated, print error and exit(0) when [no matched commit] or [having conflicts]
    public Commit findCommitByShortID(String prefixID){
        Commit target = null;
        //num of commits that have the prefix
        int num = 0;
        int prefixLength = prefixID.length();
        for(Commit commit : commits){
            if(commit.ID.substring(0, prefixLength).equals(prefixID)){
                num++;
                target = commit;
            }
        }
        if(num > 1){
            System.err.println("[self defined error]ambiguous argument.");
            System.exit(0);
        }
        if(num == 0){
            System.err.println("No commit with that id exists.");
            System.exit(0);
        }
        return target;
    }
}






















