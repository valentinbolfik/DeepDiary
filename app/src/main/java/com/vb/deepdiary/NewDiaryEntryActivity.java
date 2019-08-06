package com.vb.deepdiary;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

@SuppressWarnings("FieldCanBeLocal")
public class NewDiaryEntryActivity extends AppCompatActivity {
    private static final String TAG = "NewDiaryEntryActivity";

    private String title;
    private String content;
    private String ID;
    private String time;
    private String currentDate;
    private String currentTime;

    private static final Map<String, Object> diaryEntryData = new HashMap<>();

    @SuppressWarnings("unused")
    @BindView(R.id.fab_save) FloatingActionButton fabSave;
    @BindView(R.id.TIL_title) TextInputLayout TILTitle;
    @BindView(R.id.TIL_content) TextInputLayout TILContent;
    @BindView(R.id.TIET_title) TextInputEditText TIETTitle;
    @BindView(R.id.TIET_content) TextInputEditText TIETContent;
    @BindView(R.id.bottomAppBar) BottomAppBar toolbar;
    @BindView(R.id.button_date) Button buttonDate;
    @BindView(R.id.button_time) Button buttonTime;
    @BindView(R.id.textView_encrypt) TextView textViewEncrypting;
    @BindView(R.id.progressBar_encrypting) ProgressBar encryptingBar;

    @NonNull
    public static Intent createIntent(@NonNull Context context, String title, String content, String ID, String time) {
        Intent intent = new Intent().setClass(context, NewDiaryEntryActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("content", content);
        intent.putExtra("id", ID);
        intent.putExtra("time", time);
        return intent;
    }

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_entry);

        title = getIntent().getStringExtra("title");
        content = getIntent().getStringExtra("content");
        ID = getIntent().getStringExtra("id");
        time = getIntent().getStringExtra("time");

        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        ActionBar bar = getSupportActionBar();
        //noinspection ConstantConditions
        bar.setDisplayHomeAsUpEnabled(true);

        TIETTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString().length() <= 50 && TILTitle.isErrorEnabled()) {
                    TILTitle.setErrorEnabled(false);
                }
            }
        });
        TIETContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString().length() <= 5000 && TILContent.isErrorEnabled()) {
                    TILContent.setErrorEnabled(false);
                }
            }
        });

        TIETContent.setText(content);
        TIETTitle.setText(title);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy_HH:mm");
        if (time == null) {
            LocalDateTime localDateTime = LocalDateTime.now();
            time = localDateTime.format(formatter);
        }

        LocalDateTime LDT = LocalDateTime.parse(time, formatter);
        currentDate = String.format("%02d", LDT.getDayOfMonth()) + "." + String.format("%02d", LDT.getMonthValue()) + "." + LDT.getYear();
        currentTime = String.format("%02d", LDT.getHour()) + ":" + String.format("%02d", LDT.getMinute());

        buttonDate.setText(currentDate);
        buttonTime.setText(currentTime);


        toolbar.setNavigationOnClickListener(view -> {
            String content = TIETContent.getText() != null ? TIETContent.getText().toString() : null;
            String title = TIETTitle.getText() != null ? TIETTitle.getText().toString() : null;
            checkIfEmpty(content, title);
        });
    }

    @Override
    public void onBackPressed() {
        String content = TIETContent.getText() != null ? TIETContent.getText().toString() : null;
        String title = TIETTitle.getText() != null ? TIETTitle.getText().toString() : null;
        checkIfEmpty(content, title);
    }

    //Deselects TextInputLayout on tap outside of the layout
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    getApplicationContext();
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @BindString(R.string.cancel) String cancel;
    @BindString(R.string.discard) String discard;
    @BindString(R.string.too_long) String tooLong;

    @SuppressLint("CheckResult")
    @OnClick(R.id.fab_save)
    public void saveEntry() {
        content = TIETContent.getText() != null ? TIETContent.getText().toString() : null;
        title = TIETTitle.getText() != null ? TIETTitle.getText().toString() : null;
        //Calls saveToFirebase method if title and content are within limits
        if (title != null && title.length() > 50) {
            TILTitle.setError(tooLong);
            TILTitle.setErrorEnabled(true);
        } else if (content != null && content.length() > 5000) {
            TILContent.setError(tooLong);
            TILContent.setErrorEnabled(true);
        } else {
            saveToFirebase(content, title);
        }
    }

    private void checkIfEmpty(String content, String title) {
        //Checks if textInputEditTexts are empty, if not display dialog to check if user wants to discard the entry
        if ((title != null && !title.equals("")) || (content != null && !content.equals(""))) {
            if ((title != null && !title.equals(this.title)) || (!content.equals(this.content))) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
                builder.setTitle("Unsaved entry!");
                builder.setMessage("Discard unsaved changes?");
                builder.setPositiveButton(discard, (dialogInterface, i) -> finish());
                builder.setNegativeButton(cancel, (dialogInterface, i) -> dialogInterface.dismiss());
                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                System.gc();
                finish();
            }
        } else {
            System.gc();
            finish();
        }
    }

    @SuppressLint("CheckResult")
    private void saveToFirebase(String content, String title) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && ((title != null && !title.equals("")) && (content != null && !content.equals("")))) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy_HH:mm");
            LocalDateTime time = LocalDateTime.parse(currentDate + "_" + currentTime, formatter);
            diaryEntryData.put("Title", title);
            diaryEntryData.put("Time", time);

            encryptingBar.setIndeterminate(true);
            textViewEncrypting.setVisibility(View.VISIBLE);

            //noinspection ResultOfMethodCallIgnored
            Observable.fromCallable(() -> encrypt(content, currentUser.getUid()))
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(result -> {
                        diaryEntryData.put("Content", result);
                        sendDataToFirebase();
                    });
        } else {
            //Checks if both fields are empty if yes then finish activity, if not then display warning on the empty field
            if ((title != null && title.equals("")) || (content != null && content.equals(""))) {
                if ((title != null && title.equals("")) && content.equals("")) {
                    //If both are empty finish the activity
                    checkIfEmpty(content, title);
                } else if (title != null && title.equals("")) {
                    //Display warning on title field
                    TILTitle.setError("Must not be empty!");
                    TILTitle.setErrorEnabled(true);
                } else {
                    //Display warning on content field
                    TILContent.setError("Must not be empty!");
                    TILContent.setErrorEnabled(true);
                }
            } else {
                Toast.makeText(this, "Nothing to save :(", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void sendDataToFirebase() {
        Log.i(TAG, "sendDataToFirebase: sending data.");
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String UUID = currentUser.getUid();
            //Check if entry already exists in database
            if (ID == null) {
                CollectionReference db = FirebaseFirestore.getInstance()
                        .collection(MainActivity.COLLECTION).document(UUID)
                        .collection("diary_entries");
                db.add(diaryEntryData)
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(this, "Entry added successfully.", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Data added successfully!");
                        })
                        .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Unknown error", Toast.LENGTH_SHORT).show();
                                    Log.w(TAG, "Error adding data!", e);
                                }
                        );
            } else {
                //If entry already exists in database, only update fields
                DocumentReference db = FirebaseFirestore.getInstance()
                        .collection(MainActivity.COLLECTION).document(UUID)
                        .collection("diary_entries").document(ID);
                db.update(diaryEntryData)
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(this, "Entry updated successfully.", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Data updated successfully!");
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Unknown error", Toast.LENGTH_SHORT).show();
                            Log.w(TAG, "Error updating data!", e);
                        });
            }
        }
        finish();
    }

    private String encrypt(String strToEncrypt, String password) {
        try {
            SecureRandom secureRandom = new SecureRandom();
            byte[] salt = new byte[128];
            byte[] iv = new byte[16];
            secureRandom.nextBytes(salt);
            secureRandom.nextBytes(iv);
            IvParameterSpec ivParamSpec = new IvParameterSpec(iv);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParamSpec);
            byte[] encrypted = cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(encrypted);
            outputStream.write(iv);
            outputStream.write(salt);
            byte[] encryptedData = outputStream.toByteArray();
            return Base64.getEncoder().encodeToString(encryptedData);
        } catch (Exception e) {
            System.out.println("Error while encrypting: " + e.toString());
        }
        return null;
    }

    @OnClick(R.id.button_date)
    public void setDate() {
        CharSequence buttonDateText = buttonDate.getText();
        int day = Integer.parseInt((String) buttonDateText.subSequence(0, 2));
        int month = Integer.parseInt((String) buttonDateText.subSequence(3, 5));
        int year = Integer.parseInt((String) buttonDateText.subSequence(6, 10));

        //Create date picker dialog
        @SuppressLint("DefaultLocale") DatePickerDialog datePickerDialog = new DatePickerDialog(this, (datePicker, my_year, my_month, my_dayOfMonth) -> {
            currentDate = String.format("%02d", my_dayOfMonth) + "." + String.format("%02d", my_month) + "." + my_year;
            buttonDate.setText(currentDate);
        }, year, month, day);
        datePickerDialog.show();
    }

    @OnClick(R.id.button_time)
    public void setTime() {
        CharSequence buttonTimeText = buttonTime.getText();
        int minute = Integer.parseInt((String) buttonTimeText.subSequence(3, 5));
        int hour = Integer.parseInt((String) buttonTimeText.subSequence(0, 2));

        //Create time picker dialog
        @SuppressLint("DefaultLocale") TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view1, hourOfDay, my_minute) -> {
            currentTime = String.format("%02d", hourOfDay) + ":" + String.format("%02d", my_minute);
            buttonTime.setText(currentTime);
        }, hour, minute, android.text.format.DateFormat.is24HourFormat(this));
        timePickerDialog.show();
    }
}
