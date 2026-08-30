package com.example.memoappjava;

import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.List;

public class MemoRepository {
    private MemoDao mMemoDao;
    private LiveData<List<Memo>> mAllMemos;

    public MemoRepository(Application application) {
        MemoDatabase db = MemoDatabase.getDatabase(application);
        mMemoDao = db.memoDao();
        mAllMemos = mMemoDao.getAllMemos();
    }

    LiveData<List<Memo>> getAllMemos() {
        return mAllMemos;
    }

    void insert(Memo memo) {
        MemoDatabase.databaseWriteExecutor.execute(() -> {
            mMemoDao.insert(memo);
        });
    }
}