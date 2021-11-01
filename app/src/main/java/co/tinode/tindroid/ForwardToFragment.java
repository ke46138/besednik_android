package co.tinode.tindroid;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SearchView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import co.tinode.tindroid.media.VxCard;
import co.tinode.tindroid.widgets.HorizontalListDivider;
import co.tinode.tinodesdk.ComTopic;
import co.tinode.tinodesdk.model.Drafty;

public class ForwardToFragment extends BottomSheetDialogFragment implements MessageActivity.DataSetChangeListener {
    private static final String TAG = "ForwardToFragment";

    public static final String CONTENT_TO_FORWARD = "content_to_forward";
    public static final String FORWARDING_FROM = "forwarding_from";
    private static final int SEARCH_REQUEST_DELAY = 300; // 300 ms;
    private static final int MIN_TERM_LENGTH = 3;

    private ChatsAdapter mAdapter = null;
    private Drafty mContent = null;
    private String mSearchTerm = null;
    private String mForwardingFrom = null;

    // Delayed search action.
    private Handler mHandler = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forward_to, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        final AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity == null) {
            return;
        }

        EditText search = view.findViewById(R.id.searchContacts);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing (auto-stub).
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Do nothing (auto-stub).
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mHandler == null) {
                    mHandler = new Handler();
                } else {
                    mHandler.removeCallbacksAndMessages(null);
                }

                mSearchTerm = s.toString();
                mHandler.postDelayed(() -> mAdapter.resetContent(activity), SEARCH_REQUEST_DELAY);
            }
        });
        search.setOnKeyListener((v, keyCode, event) -> {
            // ENTER key pressed: perform search immediately.
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                if (mHandler != null) {
                    mHandler.removeCallbacksAndMessages(null);
                }

                mSearchTerm = ((EditText) v).getText().toString();
                mAdapter.resetContent(activity);
                return true;
            }
            return false;
        });
        view.findViewById(R.id.cancel).setOnClickListener(v -> {
            if (mHandler != null) {
                mHandler.removeCallbacksAndMessages(null);
            }
            mSearchTerm = null;
            dismiss();
        });

        RecyclerView rv = view.findViewById(R.id.chat_list);
        rv.setLayoutManager(new LinearLayoutManager(activity));
        rv.setHasFixedSize(true);
        rv.addItemDecoration(new HorizontalListDivider(activity));
        mAdapter = new ChatsAdapter(activity, topicName -> {
            dismiss();
            Bundle args = new Bundle();
            args.putSerializable(ForwardToFragment.CONTENT_TO_FORWARD, mContent);
            ((MessageActivity) activity).changeTopic(topicName, true);
            ((MessageActivity) activity).showFragment(MessageActivity.FRAGMENT_MESSAGES, args, true);
        }, t -> doSearch((ComTopic) t));
        rv.setAdapter(mAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();

        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        setHasOptionsMenu(true);

        Bundle args = getArguments();
        if (args != null) {
            mContent = (Drafty) args.getSerializable(CONTENT_TO_FORWARD);
            mForwardingFrom = args.getString(FORWARDING_FROM);
        }

        mAdapter.resetContent(activity);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void notifyDataSetChanged() {
        Log.i(TAG, "notifyDataSetChanged called");
        mAdapter.notifyDataSetChanged();
    }

    private boolean doSearch(ComTopic t) {
        if (t.isBlocked()) {
            return false;
        }

        String name = t.getName();
        if (name.equals(mForwardingFrom)) {
            return false;
        }

        String query = mSearchTerm != null ? mSearchTerm.trim() : null;
        //noinspection ConstantConditions
        if (TextUtils.isEmpty(query) || query.length() < MIN_TERM_LENGTH) {
            return true;
        }

        query = query.toLowerCase(Locale.ROOT);
        VxCard pub = (VxCard) t.getPub();
        if (pub.fn != null && pub.fn.toLowerCase(Locale.ROOT).contains(query)) {
            return true;
        }

        String comment = t.getComment();
        if (comment != null && comment.toLowerCase(Locale.ROOT).contains(query)) {
            return true;
        }

        return name.toLowerCase(Locale.ROOT).startsWith(query);
    }
}
