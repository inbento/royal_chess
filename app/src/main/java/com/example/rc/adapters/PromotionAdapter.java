package com.example.rc.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.rc.R;

public class PromotionAdapter extends RecyclerView.Adapter<PromotionAdapter.PromotionViewHolder> {

    private String[] promotionPieces;
    private String[] pieceNames;
    private OnPromotionPieceSelected listener;
    private boolean isWhite;

    public interface OnPromotionPieceSelected {
        void onPieceSelected(int pieceType);
    }

    public PromotionAdapter(OnPromotionPieceSelected listener, boolean isWhite) {
        this.listener = listener;
        this.isWhite = isWhite;

        if (isWhite) {
            promotionPieces = new String[]{"♕", "♖", "♗", "♘"};
            pieceNames = new String[]{"Ферзь", "Ладья", "Слон", "Конь"};
        } else {
            promotionPieces = new String[]{"♛", "♜", "♝", "♞"};
            pieceNames = new String[]{"Ферзь", "Ладья", "Слон", "Конь"};
        }
    }

    @NonNull
    @Override
    public PromotionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_promotion, parent, false);
        return new PromotionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PromotionViewHolder holder, int position) {
        if (promotionPieces == null || pieceNames == null) return;

        holder.tvPiece.setText(promotionPieces[position]);

        holder.tvPiece.setTextColor(0xFF78DBE2);
        holder.tvPieceName.setTextColor(0xFF78DBE2);

        holder.tvPieceName.setText(pieceNames[position]);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                // 1-Ферзь, 2-Ладья, 3-Слон, 4-Конь
                listener.onPieceSelected(position + 1);
            }
        });

        holder.itemView.setContentDescription("Promotion: " + pieceNames[position]);
    }

    @Override
    public int getItemCount() {
        return promotionPieces != null ? promotionPieces.length : 0;
    }

    static class PromotionViewHolder extends RecyclerView.ViewHolder {
        TextView tvPiece;
        TextView tvPieceName;

        public PromotionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPiece = itemView.findViewById(R.id.tvPiece);
            tvPieceName = itemView.findViewById(R.id.tvPieceName);

            if (tvPiece == null || tvPieceName == null) {
                throw new RuntimeException("Promotion item views not found");
            }
        }
    }
}