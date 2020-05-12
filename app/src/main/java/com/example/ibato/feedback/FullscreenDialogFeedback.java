package com.example.ibato.feedback;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.ibato.R;
import com.example.ibato.interfaces.IMainActivity;
import com.example.ibato.models.Feedback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.example.ibato.Utils.Utils.getDatabase;

public class FullscreenDialogFeedback extends DialogFragment implements View.OnClickListener {

    private static String TAG = "FullscreenDialogFeedback";
    private TextInputEditText mFeedback;
    private TextView mDialogTitle;
    private ImageView closeBtn;
    private Callback callback;
    private View dialogView;
    private RatingBar mRatingBar;
    private Button mSubmit, mCancel;
    private String userID, username;
    private ProgressDialog progressDialog;
    private IMainActivity mIMainActivity;


    FirebaseAuth mAuth;
    DatabaseReference mDatabaseRef;

    static FullscreenDialogFeedback newInstance(String key, String feedback, float rating, String title, boolean isUserHasFeedback) {
        FullscreenDialogFeedback instance = new FullscreenDialogFeedback();
        Bundle args = new Bundle();

        /* Passes arguements to the lifecycle from this constructor */
        if (isUserHasFeedback) {
            // Supply num input as an argument.
            args.putString("key", key);
            args.putString("feedback", feedback);
            args.putString("title", title);
            args.putFloat("rating", rating);
        }

        args.putBoolean("isUserHasFeedback", isUserHasFeedback);


        instance.setArguments(args);
        return instance;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogTheme);

        progressDialog = new ProgressDialog(this.getActivity());

        mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();
        username = mAuth.getCurrentUser().getDisplayName();
        mDatabaseRef = getDatabase().getReference("feedbacks" );
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new Dialog(getActivity(), getTheme()){
            @Override
            public void onBackPressed() {
                revealShow(dialogView, false, FullscreenDialogFeedback.this.getDialog());
            }
        };
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        dialogView = inflater.inflate(R.layout.fullscreen_dialog_feedback, container, false);
        mDialogTitle = dialogView.findViewById(R.id.dialog_title);
        closeBtn = dialogView.findViewById(R.id.fullscreen_dialog_close);
        mFeedback = dialogView.findViewById(R.id.feedback_input);
        mRatingBar = dialogView.findViewById(R.id.ratingBar);
        mSubmit = dialogView.findViewById(R.id.submit_button);
        mCancel = dialogView.findViewById(R.id.delete_button);

        closeBtn.setOnClickListener(this);
        mSubmit.setOnClickListener(this);
        mCancel.setOnClickListener(this);

        getDialog().setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                revealShow(dialogView, true, null);
                mIMainActivity = (IMainActivity) getActivity();
            }
        });

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mSubmit.getLayoutParams();

        if (getArguments().getBoolean("isUserHasFeedback")) {
            params.bottomMargin = (int) getResources().getDimension(R.dimen.isUserHasFeedback);
            mCancel.setVisibility(View.VISIBLE);
            mCancel.bringToFront();

            mDialogTitle.setText(getArguments().getString("title"));
            mFeedback.setText(getArguments().getString("feedback"));
            mRatingBar.setRating(getArguments().getFloat("rating"));
        } else {
            params.bottomMargin = (int) getResources().getDimension(R.dimen.isUserHasNoFeedback);
            mCancel.setVisibility(View.INVISIBLE);
        }

        return dialogView;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        switch (id) {
            case R.id.fullscreen_dialog_close:
                revealShow(dialogView, false, this.getDialog());
                break;

            case R.id.submit_button:
                saveChanges(getArguments().getBoolean("isUserHasFeedback"));
                break;

            case R.id.delete_button:
                removeFeedback(v);
                break;
        }

    }

    private void removeFeedback(View view) {
        new MaterialAlertDialogBuilder(view.getContext(), R.style.custom_material_theme)
            .setTitle("Delete Feedback")
            .setMessage("Are you sure you want to delete your feedback?")
            .setNegativeButton("Cancel", null )
            .setPositiveButton("Continue", ((dialog1, which) -> {

                mDatabaseRef.child(getArguments().getString("key")).removeValue(((databaseError, databaseReference) -> {
                    mIMainActivity.showProgressDialog(true);

                    validateOnComplete(databaseError, "Feedback Successfully Deleted!");
                }));
            }))
            .show();
    }


    private void saveChanges(boolean isUserHasFeedback) {
        String feedback_text = mFeedback.getText().toString();
        Float rating = mRatingBar.getRating();
        String date = getCurrentDate();
        String photoUrl = "";
        if (mAuth.getCurrentUser().getPhotoUrl() != null) {
            photoUrl = mAuth.getCurrentUser().getPhotoUrl().toString();
        }
        Feedback feedback = new Feedback(userID, photoUrl, username, feedback_text, date, rating);

        if (isUserHasFeedback) {
            /* Edit existing feedback */

            mDatabaseRef.child(getArguments().getString("key")).setValue(feedback, ((databaseError, databaseReference) -> {
                mIMainActivity.showProgressDialog(true);

                validateOnComplete(databaseError, "Feedback Successfully Updated!");
            }));

        } else {
            /* Add new feedback */

            String feedbackID = mDatabaseRef.push().getKey();
            mDatabaseRef.child(feedbackID).setValue(feedback, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                    mIMainActivity.showProgressDialog(true);

                    validateOnComplete(databaseError, "Feedback Successfully Submitted!");
                }
            });
        }
    }

    private void validateOnComplete(@Nullable DatabaseError databaseError, String message) {
        mIMainActivity.showProgressDialog(false);

        if (databaseError != null) {
            Log.d(TAG, databaseError.toString());
            Snackbar snackbar = Snackbar.make(FullscreenDialogFeedback.this.getActivity().findViewById(android.R.id.content), databaseError.getMessage(), Snackbar.LENGTH_LONG);
            View sbView = snackbar.getView();
            TextView textView = sbView.findViewById(R.id.snackbar_text);
            textView.setTextColor(getResources().getColor(R.color.red2));

            snackbar.setAnchorView(FeedbackActivity.mAddFeedback);
            snackbar.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE);
            snackbar.show();

            return ;
        }


        showProgressDialog(false);
        Snackbar snackbar = Snackbar.make(FullscreenDialogFeedback.this.getActivity().findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG);
        View sbView = snackbar.getView();
        TextView textView = sbView.findViewById(R.id.snackbar_text);
        textView.setTextColor(getResources().getColor(R.color.gradient_end));

        snackbar.setAnchorView(FeedbackActivity.mAddFeedback);
        snackbar.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE);
        snackbar.show();

        revealShow(dialogView, false, FullscreenDialogFeedback.this.getDialog());
    }

    /* Circular reveal animation for opening and closing of fullscreen dialog */
    private void revealShow(View dialogView, boolean b, final Dialog dialog) {

        final View view = dialogView.findViewById(R.id.fullscreen_dialog);

        int w = view.getWidth();
        int h = view.getHeight();

        int endRadius = (int) Math.hypot(w, h);

        int cx = (int) (FeedbackActivity.mAddFeedback.getX() + (FeedbackActivity.mAddFeedback.getWidth()/2));
        int cy = (int) (FeedbackActivity.mAddFeedback.getY())+ FeedbackActivity.mAddFeedback.getHeight() + 56;


        if (b) {
            Animator revealAnimator = ViewAnimationUtils.createCircularReveal(view, cx,cy, 0, endRadius);

            view.setVisibility(View.VISIBLE);
            revealAnimator.setDuration(300);
            revealAnimator.start();

        } else {
            Animator anim =
                    ViewAnimationUtils.createCircularReveal(view, cx, cy, endRadius, 0);

            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    dialog.dismiss();
                    view.setVisibility(View.INVISIBLE);

                }
            });
            anim.setDuration(300);
            anim.start();
        }

    }

    private String getCurrentDate() {
        String pattern = "dd MMM yyyy";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

        String date = simpleDateFormat.format(new Date());

        return date;
    }

    private void showProgressDialog(Boolean show) {
        if (show) {
            progressDialog.show();
            progressDialog.setContentView(R.layout.progress_dialog);
            progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        } else {
            progressDialog.dismiss();
        }
    }

    public interface Callback {

        void onActionClick();

        void showPrompt();

    }
}
