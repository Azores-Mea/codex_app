package com.example.codex;

import android.text.Editable;
import android.text.TextWatcher;

public class SimpleTextWatcher implements TextWatcher {
    private final Runnable afterTextChangedAction;

    public SimpleTextWatcher(Runnable afterTextChangedAction) {
        this.afterTextChangedAction = afterTextChangedAction;
    }

    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    @Override public void afterTextChanged(Editable s) {
        afterTextChangedAction.run();
    }
}
