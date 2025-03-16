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
        if(args.length == 0){
            System.err.println("Please enter a command.");
            System.exit(0);
        }
        String firstArg = args[0];
        if(!firstArg.equals("init")){
            File folder = Repository.GITLET_DIR;
            if (!folder.exists() || !folder.isDirectory()){
                System.err.println("Not in an initialized Gitlet directory.");
                System.exit(0);
            }else{
                //deserialize the Object of current repository
                currentRepo = Repository.recoverRepo();
            }
        }
        switch(firstArg) {
            case "init":
                File folder = Repository.GITLET_DIR;
                if (folder.exists() && folder.isDirectory()) {
                    System.err.println("A Gitlet version-control system already exists in the current directory.");
                    System.exit(0);
                } else {
                    Commit rootCommit = Commit.RootCommit();
                    //set ID for rootCommit
                    rootCommit.CalculateID();
                    currentRepo = new Repository();
                    currentRepo.initRepository(rootCommit);
                }
                break;
            case "add":
                if(args.length != 2){
                    // TODO: output like this?
                    System.err.println("Incorrect operands.");
                    System.exit(0);
                }
                String secondArg = args[1];
                File fileToAdd = new File(secondArg);
                if(!fileToAdd.exists()){
                    // TODO: throw an exception or print in the err stream and exit(0)
                    System.err.println("File does not exist");
                    System.exit(0);
                }
                currentRepo.add(fileToAdd);
                break;
            case "commit":
                if(args.length != 2){
                    System.err.println("Incorrect operands.");
                    System.exit(0);
                }
                String message = args[1];
                if(message.length() == 0){
                    System.err.println("Please enter a commit message.");
                    System.exit(0);
                }
                currentRepo.Commit(message);
                break;
            default:
                System.err.println("No command with that name exists.");
                System.exit(0);
        }
        /** by serialization before exit*/
        if (currentRepo != null) {
            Repository.REPOINFO_DIR.delete();
            currentRepo.storeRepo();
        }else{
            throw new GitletException("have not set the currentRepo");
        }
    }
}
