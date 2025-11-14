package com.example.rc.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rc.R;
import com.example.rc.adapters.KingsAdapter;
import com.example.rc.models.King;

import java.util.ArrayList;
import java.util.List;

public class KingsListFragment extends Fragment implements KingsAdapter.OnKingSelectedListener {

    private RecyclerView recyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_kings_list, container, false);
        recyclerView = view.findViewById(R.id.rvKings);
        setupRecyclerView();
        return view;
    }

    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(layoutManager);

        List<King> kings = createSampleKings();

        KingsAdapter adapter = new KingsAdapter(kings, this);
        recyclerView.setAdapter(adapter);
    }

    private List<King> createSampleKings() {
        List<King> kings = new ArrayList<>();

        kings.add(new King(
                R.drawable.king_of_man_bg,
                getString(R.string.king_man_name),
                getString(R.string.king_man_description),
                getString(R.string.faction_human),
                getString(R.string.king_man_ability)
        ));

        kings.add(new King(
                R.drawable.king_of_dragon_bg,
                getString(R.string.king_dragon_name),
                getString(R.string.king_dragon_description),
                getString(R.string.faction_dragon),
                getString(R.string.king_dragon_ability)
        ));

        kings.add(new King(
                R.drawable.king_of_elf_bg,
                getString(R.string.king_elf_name),
                getString(R.string.king_elf_description),
                getString(R.string.faction_elf),
                getString(R.string.king_elf_ability)
        ));

        kings.add(new King(
                R.drawable.king_of_gnom_bg,
                getString(R.string.king_gnom_name),
                getString(R.string.king_gnom_description),
                getString(R.string.faction_gnome),
                getString(R.string.king_gnom_ability)
        ));

        return kings;
    }

    @Override
    public void onKingSelected(King king) {
        showKingInfoDialog(king);
    }

    private void showKingInfoDialog(King king) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_king_info, null);

        ImageView ivKingDialog = dialogView.findViewById(R.id.ivKingDialog);
        TextView tvKingName = dialogView.findViewById(R.id.tvKingName);
        TextView tvKingFaction = dialogView.findViewById(R.id.tvKingFaction);
        TextView tvKingDescription = dialogView.findViewById(R.id.tvKingDescription);
        TextView tvKingAbility = dialogView.findViewById(R.id.tvKingAbility);

        ivKingDialog.setImageResource(king.getImageRes());
        tvKingName.setText(king.getName());
        tvKingFaction.setText("Фракция: " + king.getFaction());
        tvKingDescription.setText(king.getDescription());
        tvKingAbility.setText("💫 Способность:\n" + king.getAbility());

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(dialogView)
                .setPositiveButton("Понятно", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setNeutralButton("Выбрать", (dialog, which) -> {
                    String message = "Выбран король: " + king.getName();
                    android.widget.Toast.makeText(getContext(), message, android.widget.Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    }
}