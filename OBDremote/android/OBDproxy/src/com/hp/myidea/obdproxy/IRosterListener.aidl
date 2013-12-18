package com.hp.myidea.obdproxy;

import com.hp.myidea.obdproxy.RosterEntryInfo;

interface IRosterListener {
	void entryAdded(in RosterEntryInfo contact);
	void entryRemoved(in RosterEntryInfo contact);
	void statusChanged(in RosterEntryInfo contact);
}
