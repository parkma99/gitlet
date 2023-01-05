package gitlet;

import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import static gitlet.Repository.COMMITS_DIR;
import static gitlet.Utils.*;
/** Represents a gitlet commit object.
 *  does at a high level.
 *
 *  @author parker
 */
public class Commit implements Serializable {
    /**
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private String message;
    /** The time of this Commit. */
    private Date time;
    /** The parent of this Commit. */
    private String parent;
    /** The blobs in this Commit. filePath to blobHash */
    private Map<String, String> blobs;
    /** The hash of this Commit. */
    private String hash;

    /** The constructor of Commit.
     * @param message the message of this Commit
     * @param time the time of this Commit
     * @param parent the parent of this Commit
     * @param blobs the blobs in this Commit
     */

    public Commit(String message, Date time, String parent,
                  Map<String, String> blobs) {
        this.message = message;
        this.time = time;
        this.parent = parent;
        this.blobs = blobs;
        this.hash =sha1(message, timeFormatted(time), parent, blobs.toString());
    }

    public String getMessage() {
        return message;
    }

    public String getParent() {
        return parent;
    }

    public String getHash() {
        return hash;
    }

    public String getTime() {
        return timeFormatted(time);
    }

    public Map<String, String> getBlobs() {
        return blobs;
    }

    public boolean isTracked(String filePath) {
        return blobs.containsKey(filePath);
    }

    public String getBlobHash(String filePath) {
        return blobs.get(filePath);
    }

    public boolean isModified(String filePath, String blobHash) {
        return !blobs.get(filePath).equals(blobHash);
    }

    public static String timeFormatted(Date time) {
        DateFormat df = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.ENGLISH);
        return df.format(time);
    }

    public static void save(Commit commit) {
        String hash = commit.getHash();
        File commitPrefix = join(COMMITS_DIR, hash.substring(0, 2));
        if (!commitPrefix.exists()) {
            commitPrefix.mkdir();
        }
        writeObject(join(commitPrefix, hash.substring(2)), commit);
    }

    public static Commit get(String hash) {
        File commitPrefix = join(COMMITS_DIR, hash.substring(0, 2));
        File commit = join(commitPrefix, hash.substring(2));
        if (!commit.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        return readObject(commit, Commit.class);
    }

}
