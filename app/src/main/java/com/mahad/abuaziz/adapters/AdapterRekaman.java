package com.mahad.abuaziz.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.mahad.abuaziz.PlayerActivity;
import com.mahad.abuaziz.R;
import com.mahad.abuaziz.RekamanKajianActivity;
import com.mahad.abuaziz.models.ModelChat;
import com.mahad.abuaziz.models.ModelIklan;
import com.mahad.abuaziz.models.ModelRekaman;
import com.mahad.abuaziz.models.RecyclerViewItem;

import java.util.List;

public class AdapterRekaman extends RecyclerView.Adapter {
    private List<RecyclerViewItem> recyclerViewItems;
    private static final int REKAMAN_ITEM = 1;
    private static final int IKLAN_ITEM = 2;
    private Context context;

    public AdapterRekaman(List<RecyclerViewItem> recyclerViewItems, Context context){
        this.recyclerViewItems = recyclerViewItems;
        this.context = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View row;
        if (viewType == IKLAN_ITEM) {
            row = inflater.inflate(R.layout.layout_iklan, parent, false);
            return new IklanHolder(row);
        } else {
            row = inflater.inflate(R.layout.layout_rekaman, parent, false);
            return new RekamanHolder(row);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        RecyclerViewItem recyclerViewItem = recyclerViewItems.get(position);
        if (holder instanceof IklanHolder){
            IklanHolder iklanHolder = (IklanHolder) holder;
            ModelIklan modelIklan = (ModelIklan) recyclerViewItem;
            if (modelIklan.getPHOTOIKLAN() != null){
                Glide.with(context).load(modelIklan.getPHOTOIKLAN()).placeholder(R.drawable.back_putih).into(iklanHolder.photoIklan);
            }
            iklanHolder.textViewjuduliklan.setText(modelIklan.getJUDULIKLAN());
            iklanHolder.textViewdescriptioniklan.setText(modelIklan.getDESKRIPSIIKLAN());
        }
        else {
            final RekamanHolder rekamanHolder = (RekamanHolder) holder;
            final ModelRekaman modelRekaman = (ModelRekaman) recyclerViewItem;
            String judul = modelRekaman.getNama().replace("_", " ");
            judul = judul.replace("-", " ");
            rekamanHolder.rekaman_judul_kajian.setText(judul);
            rekamanHolder.rekaman_jam.setText("dirilis: " + modelRekaman.getUpload_date());
            rekamanHolder.layout_recycler.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(context, PlayerActivity.class);
                    intent.putExtra("id", modelRekaman.getId());
                    intent.putExtra("nama", modelRekaman.getNama());
                    intent.putExtra("upload_date", modelRekaman.getUpload_date());
                    intent.putExtra("status", modelRekaman.getStatus());
                    context.startActivity(intent);
                    ((RekamanKajianActivity)context).finish();
                }
            });
        }
    }

    @Override
    public int getItemViewType(int position) {
        RecyclerViewItem recyclerViewItem = recyclerViewItems.get(position);

        if (recyclerViewItem instanceof ModelIklan)
            return IKLAN_ITEM;
        else if (recyclerViewItem instanceof ModelChat)
            return REKAMAN_ITEM;
        else
            return super.getItemViewType(position);

    }

    @Override
    public int getItemCount() {
        return recyclerViewItems.size();
    }

    private static class IklanHolder extends RecyclerView.ViewHolder {
        ImageView photoIklan;
        TextView textViewjuduliklan, textViewdescriptioniklan;
        IklanHolder(@NonNull View itemView) {
            super(itemView);
            photoIklan = itemView.findViewById(R.id.photoiklan);
            textViewjuduliklan = itemView.findViewById(R.id.juduliklan);
            textViewdescriptioniklan = itemView.findViewById(R.id.descriptioniklan);
        }
    }

    private static class RekamanHolder extends RecyclerView.ViewHolder {
        TextView rekaman_judul_kajian, rekaman_jam;
        RelativeLayout layout_recycler;
        RekamanHolder(@NonNull View itemView) {
            super(itemView);
            rekaman_judul_kajian = itemView.findViewById(R.id.rekaman_judul_kajian);
            rekaman_jam = itemView.findViewById(R.id.rekaman_jam);
            layout_recycler = itemView.findViewById(R.id.layout_recycler);
        }
    }
}
