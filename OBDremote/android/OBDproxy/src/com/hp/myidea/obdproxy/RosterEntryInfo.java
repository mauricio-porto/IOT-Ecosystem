/**
 * 
 */
package com.hp.myidea.obdproxy;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author mapo
 *
 */
public class RosterEntryInfo implements Parcelable {
    private String address;
    private String alias;
    private String status;

    /**
     * Constructor from parcel
     */
    private RosterEntryInfo(Parcel in) {
        super();
        this.readFromParcel(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(this.address);
        parcel.writeString(this.alias);
        parcel.writeString(this.status);
    }

    /**
     * Reads the Parcel contents into this JobInfo, typically in order for it
     * to be passed through an IBinder connection.
     *
     * @param in The parcel to overwrite this JobInfo from.
     */
    public void readFromParcel(Parcel in) {
        this.address = in.readString();
        this.alias = in.readString();
        this.status = in.readString();
    }


    /**
     * Reads MessageListeners from Parcels.
     *
     * @see android.os.Parcelable#Creator
     */
    public static final Parcelable.Creator<RosterEntryInfo> CREATOR = new Parcelable.Creator<RosterEntryInfo>() {
        public RosterEntryInfo createFromParcel(Parcel in) {
            return new RosterEntryInfo(in);
        }

        public RosterEntryInfo[] newArray(int size) {
            return new RosterEntryInfo[size];
        }
    };
}
