package com.vb.deepdiary;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
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

import java.security.spec.KeySpec;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
        SharedPreferences.OnSharedPreferenceChangeListener {
    @SuppressWarnings("unused")
    private static final String TAG = "Main Activity";
    static final String COLLECTION = "users_diaries";
    private static SharedPreferences prefs;

    private final List<SimpleItem> rvItems = new ArrayList<>();
    private volatile FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

    private int id = 100;

    //create the ItemAdapter holding  items
    private final ItemAdapter<SimpleItem> itemAdapter = new ItemAdapter<>();

    @BindView(R.id.fab) FloatingActionButton fab;
    @BindView(R.id.bottom_NSV) NestedScrollView NSV;
    @BindView(R.id.user_icon) ImageView userIconView;
    @BindView(R.id.user_name_textView) TextView userNameView;
    @BindView(R.id.user_email_textView) TextView userEmailView;
    @BindView(R.id.navigation_view) NavigationView navView;
    @BindView(R.id.recyclerView_diary_entries) RecyclerView mRecyclerViewEntries;
    @BindView(R.id.progressBar_decrypting) ProgressBar progressBarDecrypting;

    private BottomSheetBehavior<NestedScrollView> BSB;

    @NonNull
    public static Intent createIntent(@NonNull Context context) {
        return new Intent().setClass(context, MainActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Sets default preferences on the first start
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);
        setUpSharedPreferences();
        //Load dark_theme boolean from preferences and set theme accordingly
        boolean isDark = prefs.getBoolean("dark_theme", false);
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        //Check if user is signed in, if not prompt sign in
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(FirebaseSignInActivity.createIntent(this));
            finish();
            return;
        } else {
            //Writes time of access to database
            DocumentReference db = FirebaseFirestore.getInstance().collection(COLLECTION).document(currentUser.getUid());
            LocalDateTime time = LocalDateTime.now();
            final Map<String, Object> userData = new HashMap<>();
            userData.put("last_access", time);
            db.set(userData);
        }

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        BSB = BottomSheetBehavior.from(NSV);
        //Makes fab hide when dragging NSV
        CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) fab.getLayoutParams();
        FloatingActionButton.Behavior behavior = (FloatingActionButton.Behavior) lp.getBehavior();
        if (behavior != null) {
            behavior.setAutoHideEnabled(false);
        } else {
            behavior = new FloatingActionButton.Behavior();
            behavior.setAutoHideEnabled(false);
            lp.setBehavior(behavior);
        }

        BSB.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        fab.show();
                        break;
                    case BottomSheetBehavior.STATE_DRAGGING:
                        if (fab.isShown()) {
                            fab.hide();
                        }
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        fab.hide();
                        break;
                    case BottomSheetBehavior.STATE_HALF_EXPANDED:
                        fab.hide();
                        break;
                    case BottomSheetBehavior.STATE_HIDDEN:
                        BSB.setState(BottomSheetBehavior.STATE_COLLAPSED);
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
        //Sets margin between each item in recycler view
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

    @SuppressLint("CheckResult")
    private void addListenerForChange() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Query db = FirebaseFirestore.getInstance()
                    .collection(MainActivity.COLLECTION).document(currentUser.getUid())
                    .collection("diary_entries");
            db.addSnapshotListener((queryDocumentSnapshots, e) -> {
                if (e != null) {
                    System.out.println("Listen failed: " + e);
                    return;
                }
                if (queryDocumentSnapshots != null) {
                    progressBarDecrypting.setVisibility(View.VISIBLE);
                    progressBarDecrypting.setIndeterminate(true);
                    rvItems.clear();
                    id = 100;
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        int x = queryDocumentSnapshots.size();
                        String title = (String) doc.get("Title");
                        String content = (String) doc.get("Content");
                        HashMap dateTime = (HashMap) doc.get("Time");
                        //noinspection ConstantConditions
                        @SuppressLint("DefaultLocale") String time = String.format("%02d", (Long) dateTime.get("dayOfMonth"))
                                + "." + String.format("%02d", (Long) dateTime.get("monthValue")) + "." + dateTime.get("year")
                                + "_" + String.format("%02d", (Long) dateTime.get("hour")) + ":" + String.format("%02d", (Long) dateTime.get("minute"));
                        SimpleItem item = new SimpleItem();
                        item.withTitle(title)
                                .withContent(content)
                                .withID(doc.getId())
                                .withDateTime(time)
                                .withIdentifier(id);
                        //noinspection ResultOfMethodCallIgnored
                        Observable.fromCallable(() -> decrypt(content, currentUser.getUid()))
                                .subscribeOn(Schedulers.computation())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(result -> {
                                    item.withContent(result);
                                    id = ++id;
                                    rvItems.add(item);
                                    if (rvItems.size() == x) {
                                        itemAdapter.clear();
                                        Collections.sort(rvItems);
                                        itemAdapter.add(rvItems);
                                        progressBarDecrypting.setVisibility(View.GONE);
                                        progressBarDecrypting.setIndeterminate(false);
                                        //noinspection ConstantConditions
                                        mRecyclerViewEntries.getAdapter().notifyDataSetChanged();
                                    }
                                }, throwable -> {
                                    item.withContent(content);
                                    rvItems.add(item);
                                    itemAdapter.clear();
                                    Collections.sort(rvItems);
                                    itemAdapter.add(rvItems);
                                    progressBarDecrypting.setVisibility(View.GONE);
                                    progressBarDecrypting.setIndeterminate(false);
                                    //noinspection ConstantConditions
                                    mRecyclerViewEntries.getAdapter().notifyDataSetChanged();
                                });
                    }
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
        //Switch day/night theme
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_NO) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private String decrypt(String strToDecrypt, String password) {
        try {
            byte[] toDecryptByteArray = Base64.getDecoder().decode(strToDecrypt);
            byte [] iv = Arrays.copyOfRange(toDecryptByteArray, toDecryptByteArray.length - 144, toDecryptByteArray.length - 128);
            byte[] salt = Arrays.copyOfRange(toDecryptByteArray, toDecryptByteArray.length - 128, toDecryptByteArray.length);
            byte[] content = Arrays.copyOfRange(toDecryptByteArray, 0, toDecryptByteArray.length - 144);

            IvParameterSpec ivParamSpec = new IvParameterSpec(iv);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParamSpec);
            return new String(cipher.doFinal(content));
        } catch (Exception e) {
            System.out.println("Error while decrypting: " + e.toString());
        }
        return null;
    }

    @Override
    protected void onResume() {
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            restart();
        }
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        //Close bottom drawer if open else call super()
        if (BSB.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            BSB.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        BSB.setState(BottomSheetBehavior.STATE_COLLAPSED);

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
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("dark_theme")){
            changeTheme();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }
}
