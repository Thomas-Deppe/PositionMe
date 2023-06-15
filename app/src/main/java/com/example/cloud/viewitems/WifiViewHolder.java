package com.example.cloud.viewitems;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cloud.R;

/**
 * View holder class for the RecyclerView displaying Wifi data.
 *
 * @see WifiListAdapter the corresponding list adapter.
 * @see com.example.cloud.R.layout#item_wifi_card_view xml layout file
 *
 * @author Mate Stodulka
 */
public class WifiViewHolder extends RecyclerView.ViewHolder {

    TextView bssid;
    TextView level;

    /**
     * {@inheritDoc}
     * Assign TextView fields corresponding to Wifi attributes.
     *
     * @see com.example.cloud.sensors.Wifi the data class
     */
    public WifiViewHolder(@NonNull View itemView) {
        super(itemView);
        bssid = itemView.findViewById(R.id.wifiNameItem);
        level = itemView.findViewById(R.id.wifiLevelItem);
    }
}
