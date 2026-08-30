package com.example.memoappjava;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MemoListAdapter extends RecyclerView.Adapter<MemoListAdapter.MemoViewHolder> {

    private List<Memo> memoList = new ArrayList<>();

    @NonNull
    @Override
    public MemoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.memo_item, parent, false);
        return new MemoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemoViewHolder holder, int position) {
        Memo currentMemo = memoList.get(position);
        holder.title.setText(currentMemo.getTitle());
        holder.content.setText(currentMemo.getContent());
    }

    @Override
    public int getItemCount() {
        return memoList.size();
    }

    // LiveData가 변경되었을 때 RecyclerView에 데이터를 세팅하는 메소드
    public void setMemos(List<Memo> memos) {
        this.memoList = memos;
        notifyDataSetChanged(); // 데이터가 변경되었음을 어댑터에 알림
    }

    // ViewHolder 클래스
    static class MemoViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView content;

        public MemoViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_view_title);
            content = itemView.findViewById(R.id.text_view_content);
        }
    }
}