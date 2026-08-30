package com.example.memoappjava;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "memo_table")
public class Memo {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String title;
    private String content;

    public Memo(String title, String content) {
        this.title = title;
        this.content = content;
    }

    // --- Getter 메소드 (필수) ---
    public int getId() {
        return id;
    }
    public String getTitle() {
        return title;
    }
    public String getContent() {
        return content;
    }
    // ----------------------------

    public void setId(int id) {
        this.id = id;
    }
}