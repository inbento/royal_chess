package com.example.rc.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.rc.R;
import com.example.rc.models.King;
import java.util.List;

public class KingsAdapter extends RecyclerView.Adapter<KingsAdapter.KingViewHolder> {

    private List<King> kings;
    private OnKingSelectedListener listener;

    public interface OnKingSelectedListener {
        void onKingSelected(King king);
    }

    public KingsAdapter(List<King> kings, OnKingSelectedListener listener) {
        this.kings = kings;
        this.listener = listener;
    }

    @NonNull
    @Override
    public KingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_king, parent, false);
        return new KingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull KingViewHolder holder, int position) {
        King king = kings.get(position);

        holder.ivKing.setImageResource(king.getImageRes());
        holder.tvKingName.setText(king.getName());
        holder.tvKingDescription.setText(king.getDescription());

        holder.tvKingFaction.setText(king.getFaction());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onKingSelected(king);
            }
        });

        holder.itemView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    holder.itemView.setAlpha(0.7f);
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    holder.itemView.setAlpha(1.0f);
                    break;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return kings != null ? kings.size() : 0;
    }

    public void updateKings(List<King> newKings) {
        this.kings = newKings;
        notifyDataSetChanged();
    }

    static class KingViewHolder extends RecyclerView.ViewHolder {
        ImageView ivKing;
        TextView tvKingName;
        TextView tvKingFaction;
        TextView tvKingDescription;

        public KingViewHolder(@NonNull View itemView) {
            super(itemView);
            ivKing = itemView.findViewById(R.id.ivKing);
            tvKingName = itemView.findViewById(R.id.tvKingName);
            tvKingFaction = itemView.findViewById(R.id.tvKingFaction);
            tvKingDescription = itemView.findViewById(R.id.tvKingDescription);
        }
    }
}