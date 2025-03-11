package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

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

    /** simulating the staging area*/
    public static final File STAGING_DIR = join(GITLET_DIR, "staging");

    /** serialization on the Repository object, its instance variables(e.g. Commits) will be serialized recursively*/
    public static final File REPOINFO_DIR = join(CWD, "repo_info");

    public Commit head;

    public ArrayList<Branch> branches = new ArrayList<>();

    public Branch currentBranch;

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
        Repository.STAGING_DIR.mkdirs();
        Repository.REPOINFO_DIR.mkdirs();
        this.addBranch("master", rootCommit);
        this.setCurrentBranch("master");
        this.head = rootCommit;
    }

    public void storeRepo(){
        Utils.writeObject(REPOINFO_DIR, this);
    }

    public Repository recoverRepo(){
        /** by reflection*/
        return Utils.readObject(REPOINFO_DIR, Repository.class);
    }
    /* TODO: fill in the rest of this class. */
}
