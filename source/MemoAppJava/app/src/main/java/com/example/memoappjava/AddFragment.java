package com.example.memoappjava;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

public class AddFragment extends Fragment {

    private MemoViewModel memoViewModel;
    private EditText editTitle;
    private EditText editContent;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final NavController navController = Navigation.findNavController(view);
        memoViewModel = new ViewModelProvider(requireActivity()).get(MemoViewModel.class);

        editTitle = view.findViewById(R.id.edit_text_title);
        editContent = view.findViewById(R.id.edit_text_content);

        Button saveButton = view.findViewById(R.id.button_save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = editTitle.getText().toString();
                String content = editContent.getText().toString();

                if (!TextUtils.isEmpty(title)) {
                    Memo memo = new Memo(title, content);
                    memoViewModel.insert(memo);
                    navController.popBackStack();
                }
            }
        });
    }
}