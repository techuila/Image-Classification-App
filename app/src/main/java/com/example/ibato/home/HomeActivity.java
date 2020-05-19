package com.example.ibato.home;


import android.animation.ArgbEvaluator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ibato.R;
import com.example.ibato.adapters.Adapter;
import com.example.ibato.interfaces.IHomeActivity;
import com.example.ibato.interfaces.IMainActivity;
import com.example.ibato.models.Model;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

import static com.example.ibato.Utils.Utils.getDatabase;

public class HomeActivity extends Fragment implements IHomeActivity {
    public static HomeActivity newInstance() {
        return new HomeActivity();
    }

    ListView viewPager;
    Adapter adapter;
    List<Model> models;
    Integer[] colors = null;
    ArgbEvaluator argbEvaluator = new ArgbEvaluator();

    private Button mBackButton;

    private IMainActivity mIMainActivity;
    private static final String TAG = "HomeActivity";
    private DatabaseReference mDatabaseRef;
    private FirebaseStorage mStorage;
    private String userID;
    private ProgressBar progressBar;
    private RelativeLayout mEmpty;
    private TextInputEditText mSearch;



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_history, container, false);

        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        models = new ArrayList<>();
        adapter = new Adapter(models, view.getContext(), HomeActivity.this);

        mEmpty = view.findViewById(R.id.empty_content);
        progressBar = view.findViewById(R.id.loading);
        mSearch = view.findViewById(R.id.search_input_text);
        viewPager = view.findViewById(R.id.viewPager);
        viewPager.setAdapter(adapter);
        viewPager.setPadding(0, 0, 0, 180);

        userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mStorage = FirebaseStorage.getInstance();
        mDatabaseRef = getDatabase().getReference("uploads").child(userID);
        mDatabaseRef.keepSynced(true);

        progressBar.setVisibility(View.VISIBLE);

        search("");

        mSearch.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                search(s.toString());
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

//        Integer[] colors_temp = {
//                getResources().getColor(R.color.color1),
//                getResources().getColor(R.color.color2),
//                getResources().getColor(R.color.color3),
//                getResources().getColor(R.color.color4)
//        };
//
//        colors = colors_temp;
//
//        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
//            @Override
//            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//
////                if (position < (adapter.getCount() -1) && position < (colors.length - 1)) {
////                    viewPager.setBackgroundColor(
////
////                            (Integer) argbEvaluator.evaluate(
////                                    positionOffset,
////                                    colors[position],
////                                    colors[position + 1]
////                            )
////                    );
////                }
////
////                else {
////                    viewPager.setBackgroundColor(colors[colors.length - 1]);
////                }
//            }
//
//            @Override
//            public void onPageSelected(int position) {
//
//            }
//
//            @Override
//            public void onPageScrollStateChanged(int state) {
//
//            }
//        });
    }

    public void search(String text){
        mDatabaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Clear list of photos if there is new data
                models.clear();

                Log.d(TAG, "NAKU POOOOO=========");

                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    System.out.println(postSnapshot.child("title").getValue().toString());
                    System.out.println(text);
                    if (postSnapshot.child("title").getValue().toString().contains(text) || text.equals("")) {
                        Model model = postSnapshot.getValue(Model.class);
                        model.setKey(postSnapshot.getKey());
                        models.add(model);
                    }
                }

                if (models.size() == 0) {
                    mEmpty.setVisibility(View.VISIBLE);
                } else {
                    mEmpty.setVisibility(View.GONE);
                }

                viewPager.setAdapter(adapter);
                progressBar.setVisibility(View.GONE);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getActivity(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try{
            mIMainActivity = (IMainActivity) getActivity();
        }catch (ClassCastException e){
            Log.e(TAG, "onAttach: ClassCastException: " + e.getMessage() );
        }
    }

    @Override
    public void deleteData(String key, String image, Context context) {
        final AlertDialog builder = new AlertDialog.Builder(context)
                .setTitle("Confirmation")
                .setMessage("Are you sure you want to permanently delete this data?")
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mIMainActivity.showProgressDialog(true);
                        final String modelKey = key;

                        // Gets the image reference from the selected model
                        StorageReference imageRef = mStorage.getReferenceFromUrl(image);
                        // Deletes the image on the firebase storage
                        imageRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                // Deletes the data on the firebase database
                                mDatabaseRef.child(modelKey).removeValue();
                                mIMainActivity.showProgressDialog(false);
                                Toast.makeText(getActivity(), "Data Successfully Deleted!", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                mIMainActivity.showProgressDialog(false);
                                Toast.makeText(getActivity(), "Data failed to delete!", Toast.LENGTH_SHORT).show();
                            }
                        });

                        dialog.dismiss();
                    }
                })
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();

        builder.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                builder.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(getContext().getResources().getColor(R.color.transparent));
                builder.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getContext().getResources().getColor(R.color.colorPrimary));

                builder.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(getContext().getResources().getColor(R.color.transparent));
                builder.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getContext().getResources().getColor(R.color.colorPrimary));
            }
        });



        builder.show();
    }
}