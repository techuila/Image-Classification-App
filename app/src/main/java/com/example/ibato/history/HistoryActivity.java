package com.example.ibato.history;


import android.animation.ArgbEvaluator;
import android.content.Context;
import android.os.Bundle;
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
import androidx.viewpager.widget.ViewPager;

import com.example.ibato.R;
import com.example.ibato.adapters.Adapter;
import com.example.ibato.interfaces.IMainActivity;
import com.example.ibato.models.Model;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

import static com.example.ibato.Utils.Utils.getDatabase;

public class HistoryActivity extends Fragment {
    public static HistoryActivity newInstance() {
        return new HistoryActivity();
    }

    ListView viewPager;
    Adapter adapter;
    List<Model> models;
    Integer[] colors = null;
    ArgbEvaluator argbEvaluator = new ArgbEvaluator();

    private Button mBackButton;

    private IMainActivity mIMainActivity;
    private static final String TAG = "HistoryActivity";
    private DatabaseReference mDatabaseRef;
    private FirebaseStorage mStorage;
    private String userID;
    private ProgressBar progressBar;
    private RelativeLayout mEmpty;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_history, container, false);

        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        models = new ArrayList<>();
        adapter = new Adapter(models, view.getContext(), HistoryActivity.this);

        mEmpty = view.findViewById(R.id.empty_content);
        progressBar = view.findViewById(R.id.loading);
        viewPager = view.findViewById(R.id.viewPager);
        viewPager.setAdapter(adapter);
        viewPager.setPadding(0, 0, 0, 180);

        userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mStorage = FirebaseStorage.getInstance();
        mDatabaseRef = getDatabase().getReference("uploads").child(userID);

        progressBar.setVisibility(View.VISIBLE);
        mDatabaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Clear list of photos if there is new data
                models.clear();

                Log.d(TAG, "NAKU POOOOO=========");

                if (dataSnapshot.getChildrenCount() == 0) {
                    mEmpty.setVisibility(View.VISIBLE);
                } else {
                    mEmpty.setVisibility(View.GONE);
                }

                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    Model model = postSnapshot.getValue(Model.class);
                    model.setKey(postSnapshot.getKey());
                    models.add(model);
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

//        Integer[] colors_temp = {
//                getResources().getColor(R.color.color1),
//                getResources().getColor(R.color.color2),
//                getResources().getColor(R.color.color3),
//                getResources().getColor(R.color.color4)
//        };
//
//        colors = colors_temp;

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

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try{
            mIMainActivity = (IMainActivity) getActivity();
        }catch (ClassCastException e){
            Log.e(TAG, "onAttach: ClassCastException: " + e.getMessage() );
        }
    }

    public void deleteData(Model model) {
        mIMainActivity.showProgressDialog(true);
        final String modelKey = model.getKey();

        // Gets the image reference from the selected model
        StorageReference imageRef = mStorage.getReferenceFromUrl(model.getImage());
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
    }
}