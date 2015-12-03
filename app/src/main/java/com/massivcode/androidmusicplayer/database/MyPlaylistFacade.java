package com.massivcode.androidmusicplayer.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Ray Choe on 2015-11-27.
 */
public class MyPlaylistFacade {
    private static final String TAG = MyPlaylistFacade.class.getSimpleName();
    private DbHelper mHelper;
    private Context mContext;

    public static String[] projection
            = new String[]{MyPlaylistContract.MyPlaylistEntry._ID,
            MyPlaylistContract.MyPlaylistEntry.COLUMN_NAME_PLAYLIST,
            MyPlaylistContract.MyPlaylistEntry.COLUMN_NAME_MUSIC_ID,
            MyPlaylistContract.MyPlaylistEntry.COLUMN_NAME_PLAYLIST_TYPE};

    public static String selection_music_id = MyPlaylistContract.MyPlaylistEntry.COLUMN_NAME_MUSIC_ID + "=?";
    public static String selection_playlist_name = MyPlaylistContract.MyPlaylistEntry.COLUMN_NAME_PLAYLIST + "=?";
    public static String selection_playlist_type = MyPlaylistContract.MyPlaylistEntry.COLUMN_NAME_PLAYLIST_TYPE + "=?";
    public static String selection_playlist_type_all = MyPlaylistContract.MyPlaylistEntry.COLUMN_NAME_PLAYLIST_TYPE + "=? OR " +
            MyPlaylistContract.MyPlaylistEntry.COLUMN_NAME_PLAYLIST_TYPE + "=?";
    public static String selection_playlist_type_and_name ="( " +  MyPlaylistContract.MyPlaylistEntry.COLUMN_NAME_PLAYLIST_TYPE + " =? OR " +
            MyPlaylistContract.MyPlaylistEntry.COLUMN_NAME_PLAYLIST_TYPE + " =? ) AND " +
            MyPlaylistContract.MyPlaylistEntry.COLUMN_NAME_PLAYLIST + "=?";

    public static String selection_toggle_favorite = MyPlaylistContract.MyPlaylistEntry.COLUMN_NAME_PLAYLIST + " = ? and " +
            MyPlaylistContract.MyPlaylistEntry.COLUMN_NAME_MUSIC_ID + " = ? ";

    private static String getAllUserPlaylist_SQL = "select _id, playlist_name, _id, (select count(music_id) from MyPlaylist as b where b.playlist_name = MyPlaylist.playlist_name) as music_count from MyPlaylist group by playlist_name order by _id asc";
    private static String getChildrenPlaylist_SQL = "select _id, music_id from MyPlaylist where playlist_name = '";
    private static String getMusicIdsFromSelectedPlaylist_SQL = "select music_id from MyPlaylist where playlist_name = '";


    public MyPlaylistFacade(Context context) {
        mHelper = DbHelper.getInstance(context);
        mContext = context;
    }

    public void createDb() {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        mHelper.onCreate(db);
    }

//    MediaStore.Audio.Media.ARTIST + " != ? and " + MediaStore.Audio.Media.ARTIST + " != ? " , new String[]{MediaStore.UNKNOWN_STRING, "김경호"}

    public void deleteUserPlaylist(String name) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        db.delete(MyPlaylistContract.MyPlaylistEntry.TABLE_NAME, MyPlaylistContract.MyPlaylistEntry.COLUMN_NAME_PLAYLIST + "=?", new String[]{name});
    }

    /**
     * PlayerFragment 에서 Favorite Button 표시하기 위해 사용
     * @param musicId
     * @return
     */
    public boolean isFavoritted(long musicId) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        boolean result = false;
        Cursor cursor = db.query(MyPlaylistContract.MyPlaylistEntry.TABLE_NAME, projection, selection_toggle_favorite, new String[]{MyPlaylistContract.PlaylistNameEntry.PLAYLIST_NAME_FAVORITE, String.valueOf(musicId)}, null, null, null);

        // 기존에 이런 데이터가 있을 때 -> true
        if(cursor != null && cursor.getCount() != 0) {
            result = true;
        }

        cursor.close();

        return result;
    }
    /**
     * 즐겨찾기
     * 기존 데이터 있으면 제거, 없으면 추가
     *
     * @param musicId
     */
    public void toggleFavoriteList(long musicId) {
        SQLiteDatabase db = mHelper.getWritableDatabase();

        Cursor cursor = db.query(MyPlaylistContract.MyPlaylistEntry.TABLE_NAME, projection, selection_toggle_favorite, new String[]{MyPlaylistContract.PlaylistNameEntry.PLAYLIST_NAME_FAVORITE, String.valueOf(musicId)}, null, null, null);

        // 1. 기존에 이런 데이터가 없을 때 -> Insert
        if (cursor == null || cursor.getCount() == 0) {
            ContentValues values = new ContentValues();
            values.put(MyPlaylistContract.PlaylistNameEntry.PLAYLIST_NAME_FAVORITE, "즐겨찾기");
            values.put(MyPlaylistContract.MyPlaylistEntry.COLUMN_NAME_MUSIC_ID, musicId);
            db.insert(MyPlaylistContract.MyPlaylistEntry.TABLE_NAME, null, values);
            Log.d(TAG, "즐겨찾기에 " + musicId + " 를 추가하였습니다.");
        }
        // 2. 기존에 이런 데이터가 있을 때 -> Delete
        else {
            db.delete(MyPlaylistContract.MyPlaylistEntry.TABLE_NAME, selection_toggle_favorite, new String[]{MyPlaylistContract.PlaylistNameEntry.PLAYLIST_NAME_FAVORITE, String.valueOf(musicId)});
            Log.d(TAG, "즐겨찾기에서 " + musicId + " 를 제거하였습니다.");
        }

        cursor.close();
    }

    public int getSelectedPlaylistTotal(String userPlaylistName) {
        SQLiteDatabase db = mHelper.getReadableDatabase();
        int result = -1;

        Cursor cursor = db.query(MyPlaylistContract.MyPlaylistEntry.TABLE_NAME, projection, selection_playlist_type_and_name, new String[]{MyPlaylistContract.PlaylistNameEntry.PLAYLIST_NAME_USER_DEFINITION, MyPlaylistContract.PlaylistNameEntry.PLAYLIST_NAME_USER_DEFINITION, userPlaylistName}, null, null, null);
        if(cursor == null || cursor.getCount() == 0) {
            result = 0;
        } else {
            result = cursor.getCount();
        }
        cursor.close();
        return result;
    }

    public Cursor getChildrenCursor(String playlist_name) {
        SQLiteDatabase db = mHelper.getReadableDatabase();
        return db.rawQuery(getChildrenPlaylist_SQL + playlist_name + "'", null);
    }

    /**
     * 유저가 추가한 모든 플레이리스트를 리턴
     * @return
     */
    public Cursor getAllUserPlaylist() {
        SQLiteDatabase db = mHelper.getReadableDatabase();
        return db.rawQuery(getAllUserPlaylist_SQL, null);
    }
//    /**
//     * 유저가 추가한 모든 플레이리스트를 리턴
//     * 플레이리스트 타입이 Favorite 이거나 User_Def 일 경우
//     */
//    public Cursor getAllUserPlaylist() {
//        SQLiteDatabase db = mHelper.getWritableDatabase();
//        Cursor cursor = db.query(MyPlaylistContract.MyPlaylistEntry.TABLE_NAME, projection, selection_playlist_type_all, new String[]{MyPlaylistContract.PlaylistNameEntry.PLAYLIST_NAME_FAVORITE, MyPlaylistContract.PlaylistNameEntry.PLAYLIST_NAME_USER_DEFINITION}, null, null, null);
//        while (cursor.moveToNext()) {
//            long id = cursor.getInt(cursor.getColumnIndexOrThrow(MyPlaylistContract.MyPlaylistEntry.COLUMN_NAME_MUSIC_ID));
//            String name = cursor.getString(cursor.getColumnIndexOrThrow(MyPlaylistContract.MyPlaylistEntry.COLUMN_NAME_PLAYLIST));
//            String type = cursor.getString(cursor.getColumnIndexOrThrow(MyPlaylistContract.MyPlaylistEntry.COLUMN_NAME_PLAYLIST_TYPE));
//            Log.d(TAG, "아이디 : " + id + " 플레이리스트_이름 : " + name + " 플레이리스트 타입 : " + type);
//        }
//        return db.query(MyPlaylistContract.MyPlaylistEntry.TABLE_NAME, projection, selection_playlist_type_all, new String[]{MyPlaylistContract.PlaylistNameEntry.PLAYLIST_NAME_FAVORITE, MyPlaylistContract.PlaylistNameEntry.PLAYLIST_NAME_USER_DEFINITION}, null, null, null);
//    }

    /**
     * 사용자 플레이리스트(즐겨찾기, 사용자 정의 재생목록)가 존재할 경우 true, 없을 경우 false
     * @return
     */
    public boolean isAlreadyExist() {
        boolean result = true;

        Cursor cursor = getAllUserPlaylist();

        if(cursor == null || cursor.getCount() == 0) {
            result = false;
        }

        cursor.close();

        return result;
    }

    public Cursor getSelectedPlaylistMusicIds(String userPlaylistName) {
        SQLiteDatabase db = mHelper.getReadableDatabase();
        return db.rawQuery(getMusicIdsFromSelectedPlaylist_SQL + userPlaylistName + "'", null);
    }

    /**
     * 해당 이름의 플레이리스트가 이미 존재하면 true, 아니면 false
     * @param userPlaylistName
     * @return
     */
    public boolean isAlreadyExist(String userPlaylistName) {
        boolean result = true;

        SQLiteDatabase db = mHelper.getReadableDatabase();

        Cursor cursor = db.query(MyPlaylistContract.MyPlaylistEntry.TABLE_NAME, projection, selection_playlist_name, new String[]{userPlaylistName}, null, null, null);

        if(cursor == null || cursor.getCount() == 0) {
          result = false;
        }

        cursor.close();

        return result;
    }

    /**
     * 사용자 정의 플레이 리스트 추가
     * @param userPlaylistName
     * @param userPlayList
     */
    public void addUserPlaylist(String userPlaylistName, ArrayList<Long> userPlayList) {
        SQLiteDatabase db = null;
        SQLiteStatement statement;

        if (userPlayList != null && userPlayList.size() != 0) {

            // 사용자가 추가하고자 하는 플레이리스트가 1곡일 때
            if (userPlayList.size() == 1) {
                Log.d(TAG, "case1");
                db = mHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put(MyPlaylistContract.MyPlaylistEntry.COLUMN_NAME_PLAYLIST, userPlaylistName);
                values.put(MyPlaylistContract.MyPlaylistEntry.COLUMN_NAME_MUSIC_ID, userPlayList.get(0));
                values.put(MyPlaylistContract.MyPlaylistEntry.COLUMN_NAME_PLAYLIST_TYPE, MyPlaylistContract.PlaylistNameEntry.PLAYLIST_NAME_USER_DEFINITION);
                db.insert(MyPlaylistContract.MyPlaylistEntry.TABLE_NAME, null, values);
                Log.d(TAG, "사용자 정의 플레이리스트 " + userPlaylistName + "에 1곡이 추가되었습니다.");
            }

            // 사용자가 추가하고자 하는 플레이리스트가 1곡 이상일 때
            else {
                Log.d(TAG, "case2 : " + userPlaylistName);
                db = mHelper.getWritableDatabase();
                db.beginTransaction();

                statement = db.compileStatement(
                        "INSERT INTO " + MyPlaylistContract.MyPlaylistEntry.TABLE_NAME + " ( " +
                                MyPlaylistContract.MyPlaylistEntry.COLUMN_NAME_PLAYLIST + " , " +
                                MyPlaylistContract.MyPlaylistEntry.COLUMN_NAME_MUSIC_ID + " , " +
                                MyPlaylistContract.MyPlaylistEntry.COLUMN_NAME_PLAYLIST_TYPE + " ) " +
                                "values(?, ?, ?)"

                );

                for (long id : userPlayList) {
                    int column = 1;
                    statement.bindString(column++, userPlaylistName);
                    statement.bindLong(column++, id);
                    statement.bindString(column++, MyPlaylistContract.PlaylistNameEntry.PLAYLIST_NAME_USER_DEFINITION);

                    statement.execute();
                }

                statement.close();
                db.setTransactionSuccessful();
                db.endTransaction();
                Log.d(TAG, "사용자 정의 플레이리스트 " + userPlaylistName + "에 " + userPlayList.size() + " 곡이 추가되었습니다.");

            }


        }
    }

}
