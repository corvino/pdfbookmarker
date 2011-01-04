package org.corvino;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 *  Collects a hierarchical bookmark structure, where bookmarks are sequential at the
 *  same level, and descend from the last bookmark added.
 */
public class HierarchicalBookmarkCollector {
	private List<HashMap<String, Object>> bookmarks = new ArrayList<HashMap<String, Object>>();
	private Stack<List<HashMap<String, Object>>> parents = new Stack<List<HashMap<String, Object>>>();
	private int currentLevel = 1;


	/**
	 * Add a bookmark at the specified level.
	 *
	 * @param bookmark
	 * 		The bookmark to add.
	 * @param level
	 * 		The level in the hierarchy of the bookmark.
	 */
	public void addBookmark(HashMap<String,Object> bookmark, int level) {
		if (level == currentLevel) {
			bookmarks.add(bookmark);
		} else if (level > currentLevel) {
			parents.push(bookmarks);
			bookmarks = new ArrayList<HashMap<String, Object>>();
			bookmarks.add(bookmark);

			// If the specified level is more than one level from
			// current level, that is weird--it means that the bookmark
			// level is descending multiple levels at once.  This doesn't
			// make sense, and also isn't really feasible--it causes
			// things to go haywire.  So in all cases, descend one level.

			currentLevel++;
		} else if (level < currentLevel) {
			unwindBookmarksToLevel(level);

			bookmarks.add(bookmark);
		}
	}

	/**
	 * Unwind the bookmarks to the top level.
	 */
	public void unwindBookmarks() {
		unwindBookmarksToLevel(1);
	}

	/**
	 * Returns the bookmarks at the current level.
	 *
	 * @return
	 * 		The bookmarks at the current level.
	 */
	public List<HashMap<String, Object>> getBookmarks() {
		return bookmarks;
	}

	/**
	 * Unwind the bookmarks to the specified level.  This entails
	 * adding  the child bookmarks to the appropriate parents,
	 * winding back up the parent stack to the appropriate level.
	 * 
	 * @param level
	 * 		The level to which to unwind the bookmarks.
	 */
	private void unwindBookmarksToLevel(int level) {
		List<HashMap<String, Object>>			poppedBookmarks;

		for (int i = 0; i < currentLevel - level; i++) {
			poppedBookmarks = parents.pop();

			if (poppedBookmarks.size() > 0) {
				poppedBookmarks.get(poppedBookmarks.size() - 1).put("Kids", bookmarks);
				bookmarks = poppedBookmarks;
			}
		}

		currentLevel = level;
	}
}
