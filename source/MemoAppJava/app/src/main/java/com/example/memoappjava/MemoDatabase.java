package com.example.memoappjava;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Memo.class}, version = 1, exportSchema = false)
public abstract class MemoDatabase extends RoomDatabase {

    // Room이 DAO를 인식할 수 있도록 추상 메소드를 제공합니다.
    public abstract MemoDao memoDao();

    // 데이터베이스 인스턴스는 하나만 유지합니다.
    private static volatile MemoDatabase INSTANCE;

    // 데이터베이스 작업을 위한 백그라운드 스레드 풀
    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    // 데이터베이스 인스턴스를 가져오는 메소드
    static MemoDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (MemoDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    MemoDatabase.class, "memo_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}