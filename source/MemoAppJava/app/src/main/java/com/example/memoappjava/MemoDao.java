package com.example.memoappjava;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface MemoDao {
    @Query("SELECT * FROM memo_table ORDER BY id DESC")
    LiveData<List<Memo>> getAllMemos();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Memo memo);
}