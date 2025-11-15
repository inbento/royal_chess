package com.example.rc.adapters;

import android.graphics.Color;
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
    private String selectedKingType;

    public interface OnKingSelectedListener {
        void onKingSelected(King king);
    }

    public KingsAdapter(List<King> kings, OnKingSelectedListener listener, String selectedKingType) {
        this.kings = kings;
        this.listener = listener;
        this.selectedKingType = selectedKingType;
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

        boolean isSelected = isKingSelected(king);

        if (isSelected) {
            holder.itemView.setBackgroundResource(R.drawable.king_selected_border);
        } else {
            holder.itemView.setBackgroundResource(android.R.color.transparent);
        }
        holder.tvKingName.setTextColor(Color.WHITE);
        holder.tvKingFaction.setTextColor(Color.WHITE);
        holder.tvKingDescription.setTextColor(Color.WHITE);

        holder.ivKing.setImageResource(king.getImageRes());
        holder.tvKingName.setText(king.getName());
        holder.tvKingDescription.setText(king.getDescription());
        holder.tvKingFaction.setText(king.getFaction());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onKingSelected(king);
            }
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

    public void updateSelectedKing(String kingType) {
        this.selectedKingType = kingType;
        notifyDataSetChanged();
    }

    private boolean isKingSelected(King king) {
        String currentKingType = getKingType(king);
        return currentKingType.equals(selectedKingType);
    }

    private String getKingType(King king) {
        switch (king.getName()) {
            case "Король людей": return "human";
            case "Король драконов": return "dragon";
            case "Король эльфов": return "elf";
            case "Король гномов": return "gnome";
            default: return "human";
        }
    }

}