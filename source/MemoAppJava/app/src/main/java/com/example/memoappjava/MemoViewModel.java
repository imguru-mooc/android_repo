package com.example.memoappjava;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;

public class MemoViewModel extends AndroidViewModel {
    private MemoRepository mRepository;
    private final LiveData<List<Memo>> mAllMemos;

    public MemoViewModel (Application application) {
        super(application);
        mRepository = new MemoRepository(application);
        mAllMemos = mRepository.getAllMemos();
    }

    public LiveData<List<Memo>> getAllMemos() {
        return mAllMemos;
    }

    public void insert(Memo memo) {
        mRepository.insert(memo);
    }
}