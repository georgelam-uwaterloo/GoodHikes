/*------------------------------------------------------------------------------
 *   Authors: Slavik, George, Thao, Chelsea
 *   Copyright: (c) 2016 Team Magic
 *
 *   This file is part of GoodHikes.
 *
 *   GoodHikes is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   GoodHikes is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with GoodHikes.  If not, see <http://www.gnu.org/licenses/>.
 */
package ca.uwaterloo.magic.goodhikes;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import ca.uwaterloo.magic.goodhikes.data.Route;

public class RoutesAdapter extends ArrayAdapter<Route> {

    // Cache of the children views for a route list item
    public static class ViewHolder {
        public final TextView usernameView, descriptionView, durationView, dateOnView,
                dateFromView, dateToView, pointsCountView;
        public final ImageView iconDeleteItemView;

        public ViewHolder(View view) {
            usernameView = (TextView) view.findViewById(R.id.list_item_user_name);
            descriptionView = (TextView) view.findViewById(R.id.list_item_descr_textview);
            dateOnView = (TextView) view.findViewById(R.id.list_item_date_on);
            dateFromView = (TextView) view.findViewById(R.id.list_item_date_from);
            dateToView = (TextView) view.findViewById(R.id.list_item_date_to);
            durationView = (TextView) view.findViewById(R.id.list_item_duration_textview);
            pointsCountView = (TextView) view.findViewById(R.id.list_item_points_count_textview);
            iconDeleteItemView = (ImageView) view.findViewById(R.id.list_item_delete_icon);
        }
    }

    public RoutesAdapter(Context context, ArrayList<Route> routes) {
        super(context, 0, routes);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder; // to reference the child views for later actions
        Route route = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_route, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }
        viewHolder = (ViewHolder) convertView.getTag();

        viewHolder.usernameView.setText(route.getUser().getUsername());
        viewHolder.descriptionView.setText(route.getDescription());
        viewHolder.dateOnView.setText(route.getDateStartString());
        viewHolder.durationView.setText(route.getDurationString());
        viewHolder.dateFromView.setText(route.getTimeStart());
        viewHolder.dateToView.setText(route.getTimeEnd());
        viewHolder.pointsCountView.setText(String.valueOf(route.size()));
        viewHolder.iconDeleteItemView.setOnClickListener(new DeleteIconClickCallback(position));

        return convertView;
    }

    private class DeleteIconClickCallback implements View.OnClickListener {
        private final int position;
        public DeleteIconClickCallback(int position) {
            this.position = position;
        }

        @Override
        public void onClick(View v) {
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            DialogInterface.OnClickListener deleteIconDialogCallback = new DeleteIconDialogCallback(position);
            builder.setMessage("Are you sure?")
                    .setPositiveButton("Yes", deleteIconDialogCallback)
                    .setNegativeButton("No", deleteIconDialogCallback).show();
        }

    }

    private class DeleteIconDialogCallback implements DialogInterface.OnClickListener {
        private final int position;
        public DeleteIconDialogCallback(int position) {
            this.position = position;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
            case DialogInterface.BUTTON_POSITIVE:
                Intent intent = new Intent(HistoryActivity.DELETE_ROUTE);
                intent.putExtra(HistoryActivity.POSITION_ID, position);
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                break;
            }
        }
    };
}
