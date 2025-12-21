package com.messkhata.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.messkhata.R;
import com.messkhata.data.model.User;

import java.util.List;

/**
 * Adapter for displaying member items in RecyclerView
 * Used in Settings fragment for admin to manage members
 */
public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    private List<User> memberList;
    private OnMemberActionListener listener;
    private boolean isAdmin;

    public interface OnMemberActionListener {
        void onMemberMoreOptions(User user);
    }

    public MemberAdapter(List<User> memberList, boolean isAdmin, OnMemberActionListener listener) {
        this.memberList = memberList;
        this.isAdmin = isAdmin;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        User user = memberList.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return memberList != null ? memberList.size() : 0;
    }

    public void updateMembers(List<User> newMembers) {
        this.memberList = newMembers;
        notifyDataSetChanged();
    }

    class MemberViewHolder extends RecyclerView.ViewHolder {
        private TextView tvMemberName;
        private TextView tvEmail;
        private Chip chipRole;
        private ImageButton btnMore;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            chipRole = itemView.findViewById(R.id.chipRole);
            btnMore = itemView.findViewById(R.id.btnMore);
        }

        public void bind(User user) {
            // Set member name
            tvMemberName.setText(user.getFullName());

            // Set email
            tvEmail.setText(user.getEmail());

            // Set role badge
            String role = user.getRole();
            if (role != null && role.equalsIgnoreCase("ADMIN")) {
                chipRole.setVisibility(View.VISIBLE);
                chipRole.setText("ADMIN");
            } else {
                chipRole.setVisibility(View.GONE);
            }

            // Show more options button only for admin
            if (isAdmin) {
                btnMore.setVisibility(View.VISIBLE);
                btnMore.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onMemberMoreOptions(user);
                    }
                });
            } else {
                btnMore.setVisibility(View.GONE);
            }
        }
    }
}
