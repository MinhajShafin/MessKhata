package com.messkhata.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.messkhata.MainActivity;
import com.messkhata.R;

/**
 * Activity for creating or joining a mess.
 */
public class MessSetupActivity extends AppCompatActivity {



    private MaterialButton btnCreateMess;
    private MaterialButton btnJoinMess;
    private TextInputEditText etMessName, etMessAddress, etJoinCode;
    private View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mess_setup);

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnCreateMess = findViewById(R.id.btnCreateMess);
        btnJoinMess = findViewById(R.id.btnJoinMess);

        etMessName = findViewById(R.id.etMessName);
        etMessAddress = findViewById(R.id.etMessAddress);
        etJoinCode = findViewById(R.id.etJoinCode);

        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        if (btnCreateMess != null) {
            btnCreateMess.setOnClickListener(v -> {
                String name = etMessName.getText().toString().trim();
                String address = etMessAddress.getText().toString().trim();
                // handle create mess
            });
        }

        if (btnJoinMess != null) {
            btnJoinMess.setOnClickListener(v -> {
                String code = etJoinCode.getText().toString().trim();
                // handle join mess
            });
        }
    }
}
