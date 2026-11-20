package com.metanoia.app.data.contacts;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.metanoia.domain.model.ContactPhone;
import com.metanoia.domain.model.DeviceContactCandidate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Lector en modo solo lectura de contactos de todas las cuentas del dispositivo.
 */
public final class DeviceContactsReader {

    private final Context appContext;

    public DeviceContactsReader(@NonNull Context context) {
        this.appContext = Objects.requireNonNull(context, "context").getApplicationContext();
    }

    public static boolean hasPermission(@NonNull Context context) {
        return ContextCompat.checkSelfPermission(
                context.getApplicationContext(),
                Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED;
    }

    @NonNull
    public List<DeviceContactCandidate> readAllContacts() {
        if (!hasPermission(appContext)) {
            return Collections.emptyList();
        }
        try {
            ContentResolver resolver = appContext.getContentResolver();
            Map<Long, ContactSeed> seeds = readContactSeeds(resolver);
            if (seeds.isEmpty()) {
                return Collections.emptyList();
            }
            Map<Long, List<ContactPhone>> phonesByContact = readPhonesByContact(resolver);
            Map<Long, String> birthdaysByContact = readBirthdaysByContact(resolver);

            List<DeviceContactCandidate> out = new ArrayList<>(seeds.size());
            for (Map.Entry<Long, ContactSeed> entry : seeds.entrySet()) {
                long contactId = entry.getKey();
                ContactSeed seed = entry.getValue();
                out.add(new DeviceContactCandidate(
                        seed.lookupKey,
                        seed.displayName,
                        phonesByContact.get(contactId),
                        birthdaysByContact.get(contactId)
                ));
            }
            return Collections.unmodifiableList(out);
        } catch (SecurityException ex) {
            return Collections.emptyList();
        }
    }

    private static Map<Long, ContactSeed> readContactSeeds(ContentResolver resolver) {
        Map<Long, ContactSeed> seeds = new LinkedHashMap<>();
        String[] projection = {
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.LOOKUP_KEY,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
        };
        try (Cursor cursor = resolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                null,
                null,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " COLLATE NOCASE ASC"
        )) {
            if (cursor == null) {
                return seeds;
            }
            int idIdx = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID);
            int lookupIdx = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY);
            int nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY);
            while (cursor.moveToNext()) {
                long id = cursor.getLong(idIdx);
                String lookupKey = cursor.isNull(lookupIdx) ? "" : cursor.getString(lookupIdx);
                String displayName = cursor.isNull(nameIdx) ? "" : cursor.getString(nameIdx);
                seeds.put(id, new ContactSeed(lookupKey, displayName));
            }
        }
        return seeds;
    }

    private static Map<Long, List<ContactPhone>> readPhonesByContact(ContentResolver resolver) {
        Map<Long, List<ContactPhone>> phonesMap = new HashMap<>();
        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE
        };
        try (Cursor cursor = resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY + " DESC, "
                        + ContactsContract.CommonDataKinds.Phone.IS_PRIMARY + " DESC"
        )) {
            if (cursor == null) {
                return phonesMap;
            }
            int idIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
            int numIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER);
            int typeIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE);
            while (cursor.moveToNext()) {
                String rawNumber = cursor.isNull(numIdx) ? "" : cursor.getString(numIdx);
                if (rawNumber.trim().isEmpty()) {
                    continue;
                }
                long contactId = cursor.getLong(idIdx);
                int type = cursor.getInt(typeIdx);
                boolean isMobile = type == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                        || type == ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE;
                phonesMap.computeIfAbsent(contactId, ignored -> new ArrayList<>())
                        .add(new ContactPhone(rawNumber, isMobile));
            }
        }
        return phonesMap;
    }

    private static Map<Long, String> readBirthdaysByContact(ContentResolver resolver) {
        Map<Long, String> birthdays = new HashMap<>();
        String[] projection = {
                ContactsContract.CommonDataKinds.Event.CONTACT_ID,
                ContactsContract.CommonDataKinds.Event.START_DATE
        };
        String selection = ContactsContract.Data.MIMETYPE + " = ? AND "
                + ContactsContract.CommonDataKinds.Event.TYPE + " = ?";
        String[] args = {
                ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
                String.valueOf(ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY)
        };
        try (Cursor cursor = resolver.query(
                ContactsContract.Data.CONTENT_URI,
                projection,
                selection,
                args,
                null
        )) {
            if (cursor == null) {
                return birthdays;
            }
            int idIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event.CONTACT_ID);
            int dateIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event.START_DATE);
            while (cursor.moveToNext()) {
                long contactId = cursor.getLong(idIdx);
                String startDate = cursor.isNull(dateIdx) ? "" : cursor.getString(dateIdx);
                if (!startDate.trim().isEmpty() && !birthdays.containsKey(contactId)) {
                    birthdays.put(contactId, startDate.trim());
                }
            }
        }
        return birthdays;
    }

    private static final class ContactSeed {
        final String lookupKey;
        final String displayName;

        ContactSeed(String lookupKey, String displayName) {
            this.lookupKey = lookupKey;
            this.displayName = displayName;
        }
    }
}
