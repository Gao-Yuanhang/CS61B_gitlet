package gitlet;

import java.io.File;
import java.util.ArrayList;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {



    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        /** by Deserialization, recovery from the .gitlet folder*/
        Repository currentRepo = null;

        // TODO: what if args is empty?
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                // TODO: handle the `init` command
                File folder = Repository.GITLET_DIR;
                if (folder.exists() && folder.isDirectory()) {
                    throw new GitletException("A Gitlet version-control system already exists in the current directory.");
                } else {
                    Commit rootCommit = Commit.RootCommit();
                    currentRepo = new Repository();
                    currentRepo.initRepository(rootCommit);
                }
                break;
            case "add":
                // TODO: handle the `add [filename]` command
                break;
            // TODO: FILL THE REST IN
            case "commit":
                break;
        }
        /** by serialization before exit*/
        if (currentRepo != null) {
            currentRepo.storeRepo();
        }else{
            throw new GitletException("have not set the currentRepo");
        }
    }
}
