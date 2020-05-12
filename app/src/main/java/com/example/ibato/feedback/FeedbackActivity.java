package com.example.ibato.feedback;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ibato.R;
import com.example.ibato.adapters.FeedbackAdapter;
import com.example.ibato.models.Feedback;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.List;

import static com.example.ibato.Utils.Utils.getDatabase;

public class FeedbackActivity extends Fragment {

    FeedbackAdapter adapter;
    List<Feedback> feedbacks;
    private TextView mRatingText;
    private ListView feedbackList;
    private static final String TAG = "FeedbackActivity";
    public static FloatingActionButton mAddFeedback;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private DatabaseReference mDatabaseRef;
    private FirebaseStorage mStorage;
    private String userID;
    public static boolean isUserHasFeedback = false;
    public static Feedback editFeedback;
    private boolean isLoading = false;
    private DialogFragment dialog;
    private ProgressBar progressBar;
    private RelativeLayout mEmpty;

    public static FeedbackActivity newInstance() {
        return new FeedbackActivity();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_feedbacks, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        feedbacks = new ArrayList<>();
        adapter = new FeedbackAdapter(feedbacks, view.getContext());

//        feedbacks.add(new Feedback("1", "", "Axl Cuyugan", "The app is so cool, now i can be human. This is so very revolutionary. It change my entire life to a whole new level!", "21 Jan 2020", (float) 4.5));
//        feedbacks.add(new Feedback("2", "", "Arman Cuyugan", "The app is so cool, now i can be human. This is so very revolutionary. It change my entire life to a whole new level!", "21 Jan 2020", (float) 4.5));

        initialize(view);
        setActionListeners();
//        feedbackList.setAdapter(adapter);
    }

    private void initialize(View view) {
        mAddFeedback = view.findViewById(R.id.add_feedback_btn);
        feedbackList = view.findViewById(R.id.feedback_list);
        mRatingText = view.findViewById(R.id.rating_text);
        progressBar = view.findViewById(R.id.loading);
        mEmpty = view.findViewById(R.id.empty_content);

            userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
            mStorage = FirebaseStorage.getInstance();
            mDatabaseRef = getDatabase().getReference("feedbacks");

        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);
        mDatabaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Clear list of photos if there is new data
                feedbacks.clear();

                if (dataSnapshot.getChildrenCount() == 0) {
                    checkIfUserHasFeedback(dataSnapshot, true);
                    mEmpty.setVisibility(View.VISIBLE);
                } else {
                    mEmpty.setVisibility(View.GONE);
                }


                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    checkIfUserHasFeedback(postSnapshot, false);

                    Feedback feedback = postSnapshot.getValue(Feedback.class);
                    feedback.setKey(postSnapshot.getKey());
                    feedbacks.add(feedback);
                }

                mRatingText.setText(String.valueOf(adapter.getTotalRating()));

                feedbackList.setAdapter(adapter);

                progressBar.setVisibility(View.GONE);
                isLoading = false;
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                isLoading = false;
                Toast.makeText(getActivity(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkIfUserHasFeedback(DataSnapshot dataSnapshot, boolean error) {
        editFeedback = null;

        if (error) {
            editFeedback = null;
            isUserHasFeedback = false;
            mAddFeedback.setImageResource(R.drawable.add_feedback);

            return ;
        }


        if (dataSnapshot.child("userID").getValue().toString().equals(userID)) {
            editFeedback = dataSnapshot.getValue(Feedback.class);
            editFeedback.setKey(dataSnapshot.getKey());
            isUserHasFeedback = true;
            mAddFeedback.setImageResource(R.drawable.ic_edit);
        }
    }

    private void setActionListeners() {
        mAddFeedback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isLoading) {

                    /*
                       ========================================================================
                          Purpose of this condition is to avoid opening multiple dialogs by
                          rapidly pressing the feedback button.
                       ========================================================================
                    */

                    /* If dialog is null (initial value) */
                    if (dialog == null)
                        showDiag(v);
                    else if (!dialog.isVisible())  // If dialog is already have new instance but view is closed
                        showDiag(v);


                }
            }
        });
    }

    private void showDiag(final View view) {
        String title, key, feedback;
        float rating;

        if (isUserHasFeedback) {
            title = "Edit Feedback" ;
            key = editFeedback.getKey();
            feedback = editFeedback.getFeedback();
            rating = editFeedback.getUserRating();
        } else {
            title = "Add Feedback";
            key = "";
            feedback = "";
            rating = (float) 5;
        }

        dialog = FullscreenDialogFeedback.newInstance(key, feedback, rating, title, isUserHasFeedback);
        ((FullscreenDialogFeedback) dialog).setCallback(new FullscreenDialogFeedback.Callback() {
            @Override
            public void onActionClick() {
            }

            @Override
            public void showPrompt() {

            }
        });

        dialog.show(getActivity().getSupportFragmentManager(), "tag");
    }

}
