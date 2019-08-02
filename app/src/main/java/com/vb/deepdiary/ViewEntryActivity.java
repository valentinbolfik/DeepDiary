package com.vb.deepdiary;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ViewEntryActivity extends AppCompatActivity {
    private static final String TAG = "ViewEntryActivity";

    @BindView(R.id.bottomAppBar_view_entry) BottomAppBar toolbar;
    @BindView(R.id.textView_title_view_entry) TextView textViewTitle;
    @BindView(R.id.textView_content_view_entry) TextView textViewContent;
    @BindView(R.id.textView_date) TextView textViewDate;
    @BindView(R.id.textView_time) TextView textViewTime;
    @BindView(R.id.fab_edit) FloatingActionButton fab;

    private static String title;
    private static String content;
    private static String ID;
    private static String dateTime;

    @NonNull
    public static Intent createIntent(@NonNull Context context, String title, String content, String ID, String dateTime) {
        Intent intent = new Intent().setClass(context, ViewEntryActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("content", content);
        intent.putExtra("id", ID);
        intent.putExtra("dateTime", dateTime);
        return intent;
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "onStart: starting activity.");
        super.onStart();
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_entry);

        ButterKnife.bind(this);

        title = getIntent().getStringExtra("title");
        content = getIntent().getStringExtra("content")  + "\n\n\n";
        ID = getIntent().getStringExtra("id");
        dateTime = getIntent().getStringExtra("dateTime");

        setSupportActionBar(toolbar);
        ActionBar bar = getSupportActionBar();
        bar.setDisplayHomeAsUpEnabled(true);

        DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("dd.MM.yyyy_HH:mm");

        LocalDateTime time = LocalDateTime.parse(dateTime, formatterDate);

        @SuppressLint("DefaultLocale") String timeString = String.format("%02d", time.getHour()) + ":" + String.format("%02d", time.getMinute());
        @SuppressLint("DefaultLocale") String dateString = String.format("%02d", time.getDayOfMonth()) + "." + String.format("%02d", time.getMonthValue()) + "." + String.format("%02d", time.getYear());

        textViewContent.setText(content);
        textViewTitle.setText(title);
        textViewDate.setText(dateString);
        textViewTime.setText(timeString);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bottom_app_bar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemID = item.getItemId();
        if (itemID == R.id.delete_entry) {
            displayDeleteDialog();
        }
        return super.onOptionsItemSelected(item);
    }

    private void displayDeleteDialog(){
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setMessage("Delete diary entry?");
        builder.setPositiveButton("delete", (dialogInterface, i) -> deleteEntry());
        builder.setNegativeButton("cancel", (dialogInterface, i) -> dialogInterface.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteEntry() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null){
            String UUID = currentUser.getUid();
            DocumentReference db = FirebaseFirestore.getInstance()
                    .collection(MainActivity.COLLECTION).document(UUID)
                    .collection("diary_entries").document(ID);
            db.delete()
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Entry deleted successfully.", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Data deleted successfully!");
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Unknown error", Toast.LENGTH_SHORT).show();
                        Log.w(TAG, "Error deleting data!", e);
                    });
        }
    }

    @OnClick(R.id.fab_edit)
    public void editEntry() {
        startActivity(NewDiaryEntryActivity.createIntent(this, title, content.trim(), ID, dateTime));
        finish();
    }
}
