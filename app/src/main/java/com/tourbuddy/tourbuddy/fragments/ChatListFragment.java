package com.tourbuddy.tourbuddy.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tourbuddy.tourbuddy.R;
import com.tourbuddy.tourbuddy.adapters.ChatListAdapter;
import com.tourbuddy.tourbuddy.managers.ChatFirebaseManager;
import com.tourbuddy.tourbuddy.utils.Chat;

import java.util.List;
import java.util.Map;

public class ChatListFragment extends Fragment implements ChatFirebaseManager.OnChatsAndUsernamesFetchListener {
    RecyclerView recyclerView;
    ChatListAdapter chatListAdapter;
    ChatFirebaseManager chatFirebaseManager;
    TextView noActiveChats;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);
        if(isAdded()) {
            recyclerView = view.findViewById(R.id.recyclerView);
            noActiveChats = view.findViewById(R.id.noAvailableChats);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            chatFirebaseManager = new ChatFirebaseManager();
            fetchChatsAndUsernames();
        }
        return view;
    }

    private void fetchChatsAndUsernames() {
        chatFirebaseManager.fetchChatsAndUsernames(this);
    }

    @Override
    public void onChatsAndUsernamesFetched(List<Chat> chats, Map<String, String> userIdToUsernameMap) {
        if (!isAdded())
            // Fragment is not attached to an activity, return
            return;

        if (chats.size() == 0)
            noActiveChats.setVisibility(View.VISIBLE);
        else
            noActiveChats.setVisibility(View.GONE);


        // Extract unique user IDs from fetched chats
        chatListAdapter = new ChatListAdapter(requireActivity(), chats, new ChatListAdapter.OnChatItemClickListener() {
            @Override
            public void onChatItemClick(Chat chat) {
                switchFragment(chat);
            }
        });
        recyclerView.setAdapter(chatListAdapter);
        String curId = chatFirebaseManager.getUserId();
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
        chatListAdapter.notifyDataSetChanged(); // Notify adapter of changes
    }

    private void switchFragment(Chat chat) {
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
