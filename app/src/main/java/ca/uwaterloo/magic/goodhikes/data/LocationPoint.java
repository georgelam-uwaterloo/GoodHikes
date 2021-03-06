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
package ca.uwaterloo.magic.goodhikes.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import ca.uwaterloo.magic.goodhikes.data.RoutesContract.LocationEntry;

public class LocationPoint extends Location{
    private long id;
    private long routeId;

    public LocationPoint() {
        super("sqlite_storage");
    };

    public LocationPoint(Location location) {
        super(location);
    };

    public long getId() {return id;}
    public void setId(long id) {this.id = id;}

    public long getRouteId() {return routeId;}
    public void setRouteId(long id) {this.routeId = id;}

    public static LocationPoint fromDBCursor(Cursor cursor){
        LocationPoint locationPoint = new LocationPoint();
        locationPoint.setId(       cursor.getLong(  cursor.getColumnIndex(LocationEntry._ID)));
        locationPoint.setRouteId(  cursor.getLong(  cursor.getColumnIndex(LocationEntry.COLUMN_ROUTE_KEY)));
        locationPoint.setTime(     cursor.getLong(  cursor.getColumnIndex(LocationEntry.COLUMN_DATE)));
        locationPoint.setLatitude( cursor.getDouble(cursor.getColumnIndex(LocationEntry.COLUMN_COORD_LAT)));
        locationPoint.setLongitude(cursor.getDouble(cursor.getColumnIndex(LocationEntry.COLUMN_COORD_LONG)));
        locationPoint.setSpeed(    cursor.getFloat( cursor.getColumnIndex(LocationEntry.COLUMN_SPEED)));
        locationPoint.setBearing(  cursor.getFloat( cursor.getColumnIndex(LocationEntry.COLUMN_BEARING)));
        locationPoint.setAccuracy( cursor.getFloat( cursor.getColumnIndex(LocationEntry.COLUMN_ACCURACY)));
        return locationPoint;
    }

    public ContentValues toContentValues(){
        ContentValues values = new ContentValues();
        values.put(LocationEntry.COLUMN_ROUTE_KEY,  this.routeId);
        values.put(LocationEntry.COLUMN_DATE,       this.getTime());
        values.put(LocationEntry.COLUMN_COORD_LAT,  this.getLatitude());
        values.put(LocationEntry.COLUMN_COORD_LONG, this.getLongitude());
        values.put(LocationEntry.COLUMN_SPEED,      this.getSpeed());
        values.put(LocationEntry.COLUMN_BEARING,    this.getBearing());
        values.put(LocationEntry.COLUMN_ACCURACY,   this.getAccuracy());
        return values;
    }


    // Not using Parcel functionality
    // Only needed since it extends android.location.Location
    public LocationPoint(Parcel parcel) {
        super("sqlite_storage");
    };

    // Not using Parcel functionality
    // Only needed since it extends android.location.Location
    public static final Parcelable.Creator<LocationPoint> CREATOR
            = new Parcelable.Creator<LocationPoint>() {
        public LocationPoint createFromParcel(Parcel in) {
            return new LocationPoint(in);
        }

        public LocationPoint[] newArray(int size) {
            return new LocationPoint[size];
        }
    };
}
