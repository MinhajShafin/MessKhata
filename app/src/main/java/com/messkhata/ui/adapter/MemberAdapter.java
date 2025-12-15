package com.messkhata.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.messkhata.R;
import com.messkhata.data.model.User;
import com.messkhata.utils.Constants;

/**
 * Adapter for displaying mess members.
 */
public class MemberAdapter extends ListAdapter<User, MemberAdapter.MemberViewHolder> {

    private final OnMemberActionListener listener;
    private final boolean isAdmin;

    public interface OnMemberActionListener {
        void onMemberClick(User user);
        void onChangeRole(User user, String newRole);
        void onRemoveMember(User user);
    }

    public MemberAdapter(OnMemberActionListener listener, boolean isAdmin) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.isAdmin = isAdmin;
    }

    private static final DiffUtil.ItemCallback<User> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<User>() {
                @Override
                public boolean areItemsTheSame(@NonNull User oldItem, @NonNull User newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull User oldItem, @NonNull User newItem) {
                    return oldItem.getName().equals(newItem.getName()) &&
                           oldItem.getRole().equals(newItem.getRole()) &&
                           oldItem.isActive() == newItem.isActive();
                }
            };

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class MemberViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMemberName;
        private final TextView tvEmail;
        private final Chip chipRole;
        private final View btnMore;

        MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            chipRole = itemView.findViewById(R.id.chipRole);
            btnMore = itemView.findViewById(R.id.btnMore);
        }

        void bind(User user) {
            tvMemberName.setText(user.getName());
            tvEmail.setText(user.getEmail());
            chipRole.setText(user.getRole());

            // Set role color
            int colorRes;
            switch (user.getRole()) {
                case Constants.ROLE_ADMIN:
                    colorRes = R.color.role_admin;
                    break;
                case Constants.ROLE_MANAGER:
                    colorRes = R.color.role_manager;
                    break;
                default:
                    colorRes = R.color.role_member;
                    break;
            }
            chipRole.setChipBackgroundColorResource(colorRes);

            // Show more button only for admins (and not on admin users)
            if (isAdmin && !user.isAdmin()) {
                btnMore.setVisibility(View.VISIBLE);
                btnMore.setOnClickListener(v -> showPopupMenu(v, user));
            } else {
                btnMore.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMemberClick(user);
                }
            });
        }

        private void showPopupMenu(View anchor, User user) {
            PopupMenu popup = new PopupMenu(anchor.getContext(), anchor);
            popup.getMenuInflater().inflate(R.menu.menu_member_actions, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                if (listener == null) return false;

                int itemId = item.getItemId();
                if (itemId == R.id.action_make_manager) {
                    listener.onChangeRole(user, Constants.ROLE_MANAGER);
                    return true;
                } else if (itemId == R.id.action_make_member) {
                    listener.onChangeRole(user, Constants.ROLE_MEMBER);
                    return true;
                } else if (itemId == R.id.action_remove) {
                    listener.onRemoveMember(user);
                    return true;
                }
                return false;
            });

            popup.show();
        }
    }
}
