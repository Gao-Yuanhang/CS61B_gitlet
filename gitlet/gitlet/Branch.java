package gitlet;

public class Branch {
    public String name;

    public Commit currentCommit;

    public Branch(String name, Commit commit){
        this.name = name;
        this.currentCommit = commit;
    }
}
