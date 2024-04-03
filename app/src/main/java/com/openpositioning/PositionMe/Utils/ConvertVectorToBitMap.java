package com.openpositioning.PositionMe.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

public final class ConvertVectorToBitMap {

    public ConvertVectorToBitMap() {
    }

    public static BitmapDescriptor convert(Context context, int Color, int drawable_id){

        Drawable vectorDrawable = ContextCompat.getDrawable(context, drawable_id);
        if (Color != -1) {
            if (vectorDrawable != null) {
                vectorDrawable.setColorFilter(Color, PorterDuff.Mode.SRC_IN);
            }
        }
        Bitmap bitmap = null;
        if (vectorDrawable != null) {
            bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                    vectorDrawable.getIntrinsicHeight(),
                    Bitmap.Config.ARGB_8888);
        }
        Canvas canvas = new Canvas(bitmap);
        if (vectorDrawable != null) {
            vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            vectorDrawable.draw(canvas);
        }

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}
