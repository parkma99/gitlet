package gitlet;

import java.io.Serializable;
import java.io.File;
import java.nio.charset.StandardCharsets;

import static gitlet.Utils.*;
import static gitlet.Repository.BLOBS_DIR;

/**
 * Represents a gitlet blob object.
 * does at a high level.
 *
 * @author parker
 */

public class Blob implements Serializable {

    private String filePath;

    private byte[] content;

    private String hash;

    public Blob(String filePath) {
        this(join(Repository.CWD, filePath));
    }

    public Blob(File file) {
        if (!file.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        this.filePath = file.getAbsolutePath();
        this.content = readContents(file);
        this.hash = sha1(this.filePath, this.content);
    }

    public String getFilePath() {
        return filePath;
    }

    public byte[] getContent() {
        return content;
    }

    public String getHash() {
        return hash;
    }

    public String getContentAsString() {
        return new String(content,  StandardCharsets.UTF_8);
    }
    public static void save(Blob blob) {
        String hash = blob.getHash();
        File blobPrefix = join(BLOBS_DIR, hash.substring(0, 2));
        if (!blobPrefix.exists()) {
            blobPrefix.mkdir();
        }
        File f = join(blobPrefix, hash.substring(2));
        if (!f.exists()) {
            writeObject(f, blob);
        }
    }

    public static Blob get(String hash) {
        File blobPrefix = join(BLOBS_DIR, hash.substring(0, 2));
        File blob = join(blobPrefix, hash.substring(2));
        if (!blob.exists()) {
            System.out.println("No blob with that id exists.");
            System.exit(0);
        }
        return readObject(blob, Blob.class);
    }
}