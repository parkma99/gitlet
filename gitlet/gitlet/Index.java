package gitlet;

import java.io.Serializable;
import java.util.*;

import static gitlet.Utils.*;
import static gitlet.Repository.INDEX_FILE;
import static gitlet.Repository.CWD;
public class Index implements Serializable {

    private Map<String, String> staged;
    private Set<String> removed;

    public Index() {
        staged = new HashMap<>();
        removed = new HashSet<>();
    }

    public Map<String, String> getStaged() {
        return staged;
    }

    public Set<String> getRemoved() {
        return removed;
    }

    public List<String> getStagedFiles() {
        return new ArrayList<>(staged.keySet());
    }

    public List<String> getRemovedFiles() {
        return new ArrayList<>(removed);
    }

    public void add(String fileName, String blobID) {
        removed.remove(fileName);
        staged.put(fileName, blobID);
    }
    public void addRemoved(String fileName) {
        removed.remove(fileName);
    }
    public void removeStaged(String fileName) {
        staged.remove(fileName);
    }

    public void remove(String fileName) {
        staged.remove(fileName);
        removed.add(fileName);
    }

    public void clear() {
        staged.clear();
        removed.clear();
    }

    public boolean isEmpty() {
        return staged.isEmpty() && removed.isEmpty();
    }

    public static void save(Index index){
        writeObject(INDEX_FILE, index);
    }

    public static Index get(){
        if (!INDEX_FILE.exists()) {
            return new Index();
        }
        return readObject(INDEX_FILE, Index.class);
    }
}
