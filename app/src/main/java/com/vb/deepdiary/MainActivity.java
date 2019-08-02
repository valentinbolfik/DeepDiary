package com.vb.deepdiary;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.widget.NestedScrollView;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.auth.AuthUI;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.squareup.picasso.Picasso;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
        SharedPreferences.OnSharedPreferenceChangeListener {
    @SuppressWarnings("unused")
    private static final String TAG = "Main Activity";
    static final String COLLECTION = "users_diaries";
    private static SharedPreferences prefs;

    private static final Map<String, Object> userData = new HashMap<>();
    private final List<SimpleItem> rvItems = new ArrayList<>();
    private volatile FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

    private int id = 100;

    //create the ItemAdapter holding  Items
    private final ItemAdapter<SimpleItem> itemAdapter = new ItemAdapter<>();

    @BindView(R.id.fab) FloatingActionButton fab;
    @BindView(R.id.bottom_NSV) NestedScrollView NSV;
    @BindView(R.id.user_icon) ImageView userIconView;
    @BindView(R.id.user_name_textView) TextView userNameView;
    @BindView(R.id.user_email_textView) TextView userEmailView;
    @BindView(R.id.navigation_view) NavigationView navView;
    @BindView(R.id.recyclerView_diary_entries) RecyclerView mRecyclerViewEntries;

    private BottomSheetBehavior<NestedScrollView> BSB;

    @NonNull
    public static Intent createIntent(@NonNull Context context) {
        return new Intent().setClass(context, MainActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpSharedPreferences();
        //Check if user is signed in, if not prompt sign in
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(FirebaseSignInActivity.createIntent(this));
            finish();
            return;
        } else {
            DocumentReference db = FirebaseFirestore.getInstance().collection(COLLECTION).document(currentUser.getUid());
            LocalDateTime time = LocalDateTime.now();
            userData.put("last_access", time);
            db.set(userData);
        }
        Log.i(TAG, "onCreate: " + currentUser.getUid());

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        BSB = BottomSheetBehavior.from(NSV);
        BSB.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        fab.show();
                        break;
                    case BottomSheetBehavior.STATE_DRAGGING:
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        fab.hide();
                        break;
                    case BottomSheetBehavior.STATE_HALF_EXPANDED:
                        fab.hide();
                        break;
                    case BottomSheetBehavior.STATE_HIDDEN:
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:

                        if (fab.isShown()) {
                            fab.hide();
                        } else {
                            fab.show();
                        }
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });
        setUserData();
        navView.setNavigationItemSelectedListener(this);

        //create the managing FastAdapter, by passing in the itemAdapter
        FastAdapter<SimpleItem> mFastAdapter = FastAdapter.with(itemAdapter);
        mFastAdapter.setHasStableIds(true);
        mFastAdapter.withSelectable(false);
        mFastAdapter.withOnClickListener((v, adapter, item, position) -> {
            startActivity(ViewEntryActivity.createIntent(this, item.getTitle(), item.getContent(), item.getID(), item.getDateTime()));
            return true;
        });

        mRecyclerViewEntries.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerViewEntries.addItemDecoration(new MarginItemDecorator(8));
        //set adapter to the RecyclerView
        mRecyclerViewEntries.setAdapter(mFastAdapter);

        //Add listener for changes in database and updating recyclerView if needed
        addListenerForChange();
    }

    @OnClick(R.id.imageView_menu_icon)
    public void handleButtonState() {
        //Expands/Collapses navigation drawer
        if (BSB.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
            BSB.setState(BottomSheetBehavior.STATE_EXPANDED);
            fab.hide();
        } else if (BSB.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            BSB.setState(BottomSheetBehavior.STATE_COLLAPSED);
            fab.show();
        }
    }

    @OnClick(R.id.fab)
    public void addDiaryEntry() {
        startActivity(NewDiaryEntryActivity.createIntent(this, null, null, null, null));
    }

    private void addListenerForChange() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Query db = FirebaseFirestore.getInstance()
                    .collection(MainActivity.COLLECTION).document(currentUser.getUid())
                    .collection("diary_entries").orderBy("Time");
            db.addSnapshotListener((queryDocumentSnapshots, e) -> {
                if (e != null) {
                    System.out.println("Listen failed: " + e);
                    return;
                }
                if (queryDocumentSnapshots != null) {
                    rvItems.clear();
                    itemAdapter.clear();
                    id = 100;
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String title = (String) doc.get("Title");
                        String content = (String) doc.get("Content");
                        HashMap dateTime = (HashMap) doc.get("Time");
                        @SuppressLint("DefaultLocale") String time = String.format("%02d", (Long) dateTime.get("dayOfMonth"))
                                + "." + String.format("%02d", (Long) dateTime.get("monthValue")) + "." + dateTime.get("year")
                                + "_" + String.format("%02d", (Long) dateTime.get("hour")) + ":" + String.format("%02d", (Long) dateTime.get("minute"));
                        SimpleItem item = new SimpleItem();
                        item.withTitle(title)
                                .withContent(content)
                                .withID(doc.getId())
                                .withDateTime(time)
                                .withIdentifier(id);
                        id = ++id;
                        rvItems.add(item);
                    }
                    Collections.sort(rvItems);
                    itemAdapter.add(rvItems);
                    //noinspection ConstantConditions
                    mRecyclerViewEntries.getAdapter().notifyDataSetChanged();
                }
            });
        } else {
            restart();
        }
    }

    private void setUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userNameView.setText(user.getDisplayName());
            userEmailView.setText(user.getEmail());
            Picasso.get().load(user.getPhotoUrl()).noFade().into(userIconView);
        } else {
            restart();
        }
    }

    private void restart() {
        //Restarts the app
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
        finish();
    }

    private void setUpSharedPreferences(){
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    private void changeTheme() {
        Log.i(TAG, "changeTheme: Theme changed");
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_NO) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    @Override
    protected void onResume() {
        if (currentUser == null) {
            restart();
        }
        super.onResume();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_logout) {
            AuthUI.getInstance()
                    .signOut(this)
                    .addOnCompleteListener(task -> {
                        Toast.makeText(this, "Logged out!", Toast.LENGTH_SHORT).show();
                        restart();
                    });
            return true;
        } else if (id == R.id.nav_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("dark_theme")){
            changeTheme();
        }
    }
}
