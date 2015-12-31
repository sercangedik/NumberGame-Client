package com.multigames.numbergame.Util;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.multigames.numbergame.NumberGameActivity;
import com.multigames.numbergame.R;

import java.util.List;

/**
 * Created by Sercan-PC on 19.12.2015.
 */
public class MoveAdapter extends BaseAdapter {

    private LayoutInflater mInflater;
    private List<String>   moveList;

    public MoveAdapter(Activity numberGameActivity, List<String> moves) {
        mInflater = (LayoutInflater) numberGameActivity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        this.moveList = moves;
    }

    @Override
    public int getCount() {
        return moveList.size();
    }

    @Override
    public Object getItem(int position) {
        return moveList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View moveView = mInflater.inflate(R.layout.move_item, null);
        TextView moveViewText = (TextView) moveView.findViewById(R.id.moveText);
        moveViewText.setText(moveList.get(position));
        return moveView;
    }
}
