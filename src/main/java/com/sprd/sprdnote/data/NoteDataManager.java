package com.sprd.sprdnote.data;

import java.util.List;
import com.sprd.notejar.view.data.NoteItem;

public interface NoteDataManager {
    /*
     * < 1 > interface for note items
     */
    /*
     * A new note item will be created into DB for a note.
     */
    public int addNewNote(NoteItem item);

    /*
     * Update the delete flag to true in DB item for a note.
     */
    public void deleteNoteIntoDeletedFolder(int id);
    public void deleteNoteIntoDeletedFolder(NoteItem item);

    /*
     * Delete note item from DB forever.
     */
    public void deleteNoteFromDeletedFolder(int id);

    public void deleteNoteFromDeletedFolder(NoteItem item);

    /*
     * Restore a note which has deleted (show in the deleted folder)
     */
    public void restoreNoteFromDeletedFolder(int id);

    public void restoreNoteFromDeletedFolder(NoteItem item);

    /*
     * Update note item in DB after you change anythings for NoteItem
     */
    public int updateNote(NoteItem item);

    public NoteItem getNoteItem(int id);

    /*
     * Get notes for Custom folder
     */
    public List<NoteItem> getNotesForCustomFolder(int folderId);

    /*
     * Get notes for preset folder such as collect folder & private folder
     * when collect folder, user type NoteDataManager.COLLECTED_FOLDER
     * when private folder, user type NoteDataManager.PRIVATE_FOLDER
     */
    public List<NoteItem> getNotesForPresetFolder(int type);

    /*
     * Search all notes that contains the string.
     */
    public List<NoteItem> getNotesIncludeContent(String content);

    /*
     * < 2 > interface for folder items
     */
    public int addNewFolder(FolderItem item);

    public void deleteFolderIntoDeletedFolder(int id, int type);

    public void deleteFolderIntoDeletedFolder(FolderItem item, int type);

    public void deleteFolderFromDeletedFolder(int id);

    public void deleteFolderFromDeletedFolder(FolderItem item);

    public void restoreFolderFromDeletedFolder(int id);

    public void restoreFolderFromDeletedFolder(FolderItem item);

    public int updateFolder(FolderItem item);

    public FolderItem getFolderItem(int id);

    /*
     * Get all folder list, this api is use to move one note into other folder.
     */
    public List<FolderItem> getFolderListForNoteMove();

    /*
     * Get the folders which is not deleted.
     */
    public List<FolderItem> getAllValidFolders();

    /*
     * Get the folders which is deleted.
     * If want to restore folders of deleted folders,
     * You need to update the date, time and delete flag in DB.
     */
    public List<FolderItem> getDeletedFolders();

}