package gitlet;

import java.io.Serializable;

/** as instance variable of class 'Repository', it must also implement interface 'Serializable'*/
public class Branch implements Serializable {
    public String name;

    public Commit currentCommit;

    public Branch(String name, Commit commit){
        this.name = name;
        this.currentCommit = commit;
    }

    public String getName(){
        return name;
    }
}
