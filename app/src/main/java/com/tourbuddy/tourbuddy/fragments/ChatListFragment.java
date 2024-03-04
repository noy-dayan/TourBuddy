package com.tourbuddy.tourbuddy.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.tourbuddy.tourbuddy.R;
import com.tourbuddy.tourbuddy.adapters.ChatListAdapter;
import com.tourbuddy.tourbuddy.managers.FirebaseManager;
import com.tourbuddy.tourbuddy.utils.Chat;

import java.util.List;
import java.util.Map;

public class ChatListFragment extends Fragment implements FirebaseManager.OnChatsAndUsernamesFetchListener {
    private RecyclerView recyclerView;
    private ChatListAdapter adapter;
    private FirebaseManager firebaseManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        firebaseManager = new FirebaseManager();
        fetchChatsAndUsernames();
        return view;
    }

    private void fetchChatsAndUsernames() {
        firebaseManager.fetchChatsAndUsernames(this);
    }

    @Override
    public void onChatsAndUsernamesFetched(List<Chat> chats, Map<String, String> userIdToUsernameMap) {
        // Extract unique user IDs from fetched chats
        adapter = new ChatListAdapter(chats, new ChatListAdapter.OnChatItemClickListener() {
            @Override
            public void onChatItemClick(Chat chat) {
                changeFragment(chat);
            }
        });
        recyclerView.setAdapter(adapter);
        String curId = firebaseManager.getUserId();
        // Assign usernames to chat participants
        for (Chat chat : chats) {
            for (String participant : chat.getParticipants()) {
                String username = userIdToUsernameMap.get(participant);
                chat.addParticipantName(participant, username);
                if (!participant.equals(curId)) {
                    chat.setOtherUserName(username);
                }
            }
        }
        adapter.notifyDataSetChanged(); // Notify adapter of changes
    }

    private void changeFragment(Chat chat) {
        if (getActivity() != null) {
            FragmentActivity activity = getActivity();
            if (activity.getSupportFragmentManager() != null) {
                activity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.frameLayout, ChatFragment.newInstance(chat))
                        .addToBackStack(null)
                        .commit();
            }
        }
    }

    @Override
    public void onChatsAndUsernamesFetchFailed(Exception e) {
        // Handle failure if needed
    }
}
