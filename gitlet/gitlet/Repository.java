package gitlet;

import java.io.File;
import java.util.*;

import static gitlet.Utils.*;


/** Represents a gitlet repository.
 *  does at a high level.
 *
 *  @author parker
 */
public class Repository {
    /**
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    public static final File OBJECTS_DIR = join(GITLET_DIR, "objects");
    public static final File COMMITS_DIR = join(OBJECTS_DIR, "commits");
    public static final File BLOBS_DIR = join(OBJECTS_DIR, "blobs");
    public static final File BRANCHES_DIR = join(GITLET_DIR, "branches");

    public static final File INDEX_FILE = join(GITLET_DIR, "index");
    public static final File HEAD_FILE = join(GITLET_DIR, "HEAD");

    private static void checkGitletDir() {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }

    private static String relativePath(String filePath) {
        return filePath.substring(CWD.getPath().length() + 1);
    }

    private static Commit getHeadCommit() {
        String curBranch = readContentsAsString(HEAD_FILE);
        return getCommit(curBranch);
    }

    private static Commit getCommit(String branch) {
        String hash = getBranchCommitHash(branch);
        return getCommitFromHash(hash);
    }

    private static Commit getCommitFromHash(String hash) {
        if (hash.length() == 40) {
            return Commit.get(hash);
        }
        File commitPrefix = join(COMMITS_DIR, hash.substring(0, 2));
        if (commitPrefix.exists()) {
            List<String> commits = plainFilenamesIn(commitPrefix);
            assert commits != null;
            for (String commit : commits) {
                if (commit.startsWith(hash.substring(2))) {
                    return Commit.get(hash.substring(0, 2) + commit);
                }
            }
        }
        System.out.println("No commit with that id exists.");
        System.exit(0);
        return null;
    }

    private static String getBranchCommitHash(String branch) {
        File file = join(BRANCHES_DIR, branch);
        if (!file.exists()) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        return readContentsAsString(file);
    }

    private static void updateHeadBranch(String branch) {
        writeContents(HEAD_FILE, branch);
    }

    private static void checkUntrackedOverwritten(Map<String, String> snapShot, Map<String, String> newBlobs,
                                                  Commit targetCommit) {
        for (String fileName : snapShot.keySet()) {
            if (!newBlobs.containsKey(fileName) && targetCommit.isTracked(fileName)) {
                System.out.println("There is an untracked file in the way;" +
                            " delete it, or add and commit it first.");
                System.exit(0);
            }
        }
    }

    private static void updateHeadCommit(String hash) {
        String curBranch = readContentsAsString(HEAD_FILE);
        updateBranch(curBranch, hash);
    }

    private static void updateBranch(String branch, String hash) {
        writeContents(join(BRANCHES_DIR, branch), hash);
    }

    private static void clearStageArea(Index index) {
        index.clear();
        Index.save(index);
    }

    private static Map<String, String> getCurrentBlobs(Commit commit, Index index) {
        Map<String, String > curBlobs = new HashMap<>(commit.getBlobs());
        curBlobs.putAll(index.getStaged());
        for (String fileName : index.getRemoved()) {
            curBlobs.remove(fileName);
        }
        return curBlobs;
    }

    private static Map<String ,String > getCurrentSnapShot() {
        List<String> curFiles = plainFilenamesIn(CWD);
        HashMap<String, String> snapShot = new HashMap<>();
        assert curFiles != null;
        for (String fileName : curFiles) {
            Blob blob = new Blob(fileName);
            String hash = blob.getHash();
            snapShot.put(blob.getFilePath(), hash);
        }
        return snapShot;
    }
    public static void init(){
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already " +
                    "exists in the current directory.");
            System.exit(0);
        }
        GITLET_DIR.mkdir();
        OBJECTS_DIR.mkdir();
        COMMITS_DIR.mkdir();
        BLOBS_DIR.mkdir();
        BRANCHES_DIR.mkdir();

        Commit initialCommit = new Commit("initial commit", new Date(0), "", new HashMap<>());
        Commit.save(initialCommit);
        writeContents(join(BRANCHES_DIR, "master"), initialCommit.getHash());
        writeContents(HEAD_FILE, "master");
    }

    public static void add(String fileName) {
        checkGitletDir();

        Blob blob = new Blob(fileName);
        Blob.save(blob);
        String blob_hash = blob.getHash();
        String file_path = blob.getFilePath();

        Commit head= getHeadCommit();
        Index stage = Index.get();
        if (head.isTracked(file_path) && !head.isModified(file_path, blob_hash)) {
            if (stage.getRemoved().contains(file_path)) {
                stage.addRemoved(file_path);
                Index.save(stage);
            }
            return;
        }
        stage.add(file_path, blob_hash);
        Index.save(stage);
    }

    public static void commit(String message){
        checkGitletDir();

        if (message == null || message.equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }

        Index staging = Index.get();
        if (staging.isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }

        Commit parent = getHeadCommit();
        Map<String, String> newBlobs = getCurrentBlobs(parent, staging);
        Commit commit = new Commit(message, new Date(), parent.getHash(), newBlobs);
        Commit.save(commit);

        updateHeadCommit(commit.getHash());
        clearStageArea(staging);
    }

    public static void remove(String fileName) {
        checkGitletDir();

        File file = join(CWD, fileName);
        fileName = file.getAbsolutePath();
        Commit head = getHeadCommit();
        Index stage = Index.get();
        if (stage.getStaged().containsKey(fileName)) {
            stage.removeStaged(fileName);
            Index.save(stage);
            return;
        }
        if (head.isTracked(fileName)) {
            if (file.exists() && ! restrictedDelete(file)) {
                System.exit(0);
            }
            stage.remove(fileName);
            Index.save(stage);
            return;
        }

        System.out.println("No reason to remove the file.");
        System.exit(0);
    }

    private static String getCommitAsString(Commit commit) {
        StringBuilder builder = new StringBuilder();

        builder.append("===").append("\n");
        builder.append("commit ").append(commit.getHash()).append("\n");
        builder.append("Date: ").append(commit.getTime()).append("\n");
        builder.append(commit.getMessage()).append("\n");
        builder.append("\n");
        return builder.toString();
    }
    public static void log() {
        checkGitletDir();

        Commit commit = getHeadCommit();
        while (commit != null) {
            System.out.print(getCommitAsString(commit));
            String parentHash = commit.getParent();
            if (!parentHash.equals("")) {
                commit = Commit.get(parentHash);
            } else {
                commit = null;
            }
        }
    }

    public static void globalLog() {
        checkGitletDir();

        String[] commitDirs = COMMITS_DIR.list();
        assert commitDirs != null;
        for (String commitDir : commitDirs) {
            List<String> commits = plainFilenamesIn(join(COMMITS_DIR, commitDir));
            assert commits != null;
            for (String commit : commits) {
                String hash = commitDir + commit;
                Commit commitObj = Commit.get(hash);
                System.out.print(getCommitAsString(commitObj));
            }
        }
    }

    public static void status() {
        checkGitletDir();

        StringBuilder builder = new StringBuilder();

        String curBranch = readContentsAsString(HEAD_FILE);
        List<String> branches = plainFilenamesIn(BRANCHES_DIR);
        builder.append("=== Branches ===").append("\n");
        builder.append("*").append(curBranch).append("\n");

        assert branches != null;
        for (String branch : branches) {
            if (!branch.equals(curBranch)) {
                builder.append(branch).append("\n");
            }
        }
        builder.append("\n");

        Index changes = Index.get();
        List<String> stagedFiles = changes.getStagedFiles();
        Collections.sort(stagedFiles);
        builder.append("=== Staged Files ===\n");
        for (String stagedFile : stagedFiles) {
            builder.append(relativePath(stagedFile)).append("\n");
        }
        builder.append("\n");

        List<String> removedFiles = changes.getRemovedFiles();
        Collections.sort(removedFiles);
        builder.append("=== Removed Files ===\n");
        for (String removedFile : removedFiles) {
            builder.append(relativePath(removedFile)).append("\n");
        }
        builder.append("\n");

        builder.append("=== Modifications Not Staged For Commit ===\n");
        Map<String, String> newBlobs = getCurrentBlobs(getHeadCommit(), changes);
        Map<String, String> snapShot = getCurrentSnapShot();
        Set<String> modifications = new HashSet<>();
        for (Map.Entry<String, String> entry : newBlobs.entrySet()) {
            if (snapShot.containsKey(entry.getKey()) && !snapShot.get(entry.getKey()).equals(entry.getValue())) {
                modifications.add(relativePath(entry.getKey()) + " (modified)");
            } else if (!snapShot.containsKey(entry.getKey())) {
                modifications.add(relativePath(entry.getKey()) + " (deleted)");
            }
        }
        for (String entry : modifications) {
            builder.append(entry).append("\n");
        }
        builder.append("\n");

        builder.append("=== Untracked Files ===\n");
        Set<String> untracked = new HashSet<>();
        for (Map.Entry<String, String> entry : snapShot.entrySet()) {
            if (!newBlobs.containsKey(entry.getKey())) {
                untracked.add(relativePath(entry.getKey()));
            }
        }
        for (String entry : untracked) {
            builder.append(entry).append("\n");
        }
        builder.append("\n");

        System.out.print(builder);
    }

    public static void find(String message) {
        checkGitletDir();

        StringBuilder builder = new StringBuilder();

        String[] commitDirs = COMMITS_DIR.list();
        assert commitDirs != null;
        for (String commitDir : commitDirs) {
            List<String> commits = plainFilenamesIn(join(COMMITS_DIR, commitDir));
            assert commits != null;
            for (String commit : commits) {
                String ID = commitDir + commit;
                Commit commitObj = Commit.get(ID);
                if (commitObj.getMessage().contains(message)) {
                    builder.append(ID).append("\n");
                }
            }
        }
        if (builder.toString().isEmpty()) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }

        builder.append("\n");
        System.out.println(builder);
    }

    public static void checkout(String... args) {
        checkGitletDir();

        if (args.length == 2) {
            // checkout branch
            checkoutBranch(args[1]);
        } else if (args.length == 3 && args[1].equals("--")) {
            // checkout  -- file
            checkoutFile(args[2]);
        } else if (args.length == 4 && args[2].equals("--")) {
            // checkout commit -- file
            checkoutFileFromCommit(args[1], args[3]);
        } else {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

    private static byte[] getBlobContent(String hash) {
        Blob blob = Blob.get(hash);
        return blob.getContent();
    }

    private static void checkoutCommit(Commit target) {
        Commit head = getHeadCommit();

        Index changes = Index.get();
        Map<String, String> newBlobs = getCurrentBlobs(head, changes);
        Map<String, String> snapShot = getCurrentSnapShot();

        checkUntrackedOverwritten(snapShot, newBlobs, target);

        for (String fileName : snapShot.keySet()) {
            if (head.isTracked(fileName) && !target.isTracked(fileName)) {
                restrictedDelete(new File(fileName));
            }
        }

        for (Map.Entry<String, String> entry : target.getBlobs().entrySet()) {
            File file = new File(entry.getKey());
            String blobHash = entry.getValue();
            writeContents(file, getBlobContent(blobHash));
        }

        clearStageArea(changes);
    }

    private static void checkoutBranch(String branch) {
        String curBranch = readContentsAsString(HEAD_FILE);
        if (curBranch.equals(branch)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }
        Commit commit = getCommit(branch);
        checkoutCommit(commit);
        updateHeadBranch(branch);
    }

    private static void checkoutFile(String fileName){
        Commit head = getHeadCommit();
        checkoutFileFromCommit(head, fileName);
    }

    private static void checkoutFileFromCommit(String hash, String fileName){
        Commit commit = getCommitFromHash(hash);
        checkoutFileFromCommit(commit, fileName);
    }

    private static void checkoutFileFromCommit(Commit commit, String fileName) {
        File file = join(CWD, fileName);
        if (!commit.isTracked(file.getAbsolutePath())) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        String blobHash = commit.getBlobHash(file.getAbsolutePath());
        writeContents(file, getBlobContent(blobHash));
    }

    public static void branch(String branch){
        checkGitletDir();
        File file = join(BRANCHES_DIR, branch);
        if (file.exists()) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        String head = readContentsAsString(HEAD_FILE);
        String commitHash = getBranchCommitHash(head);
        writeContents(file, commitHash);
    }

    public static void removeBranch(String branch) {
        checkGitletDir();
        File file = join(BRANCHES_DIR, branch);
        if (!file.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        String head = readContentsAsString(HEAD_FILE);
        if (head.equals(branch)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        file.delete();

    }

    public static void reset(String hash) {
        checkGitletDir();
        Commit commit = getCommitFromHash(hash);
        checkoutCommit(commit);
        updateHeadCommit(commit.getHash());
    }

}
