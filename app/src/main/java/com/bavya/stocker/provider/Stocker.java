package com.bavya.stocker.provider;


import android.net.Uri;
import android.provider.BaseColumns;

public class Stocker {
    private static final String TAG = "Stocker";

    private Stocker() {}


    public static final class Stocks implements BaseColumns {
        private Stocks() {}
        public static final String AUTHORITY = "com.bavya.stocker.provider";
        /**
         * The {@code content://} style URL for this table.
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/stocks");
        /**
         * The default sort order for this table.
         */
        public static final String DEFAULT_SORT_ORDER = "name ASC";
        /**
         * Stock name.
         * <P>Type: TEXT</P>
         */
        public static final String NAME = "name";
        /**
         * Stock symbol.
         * <P>Type: TEXT</P>
         */
        public static final String SYMBOL = "symbol";
        /**
         * Stock currency.
         * <P>Type: TEXT</P>
         */
        public static final String CURRENCY = "currency";
        /**
         * Stock price.
         * <P>Type: TEXT</P>
         */
        public static final String PRICE = "price";
        /**
         * Stock change.
         * <P>Type: INTEGER</P>
         */
        public static final String CHANGE = "change";

        public static String[] PROJECTION_ALL = { NAME, SYMBOL, CURRENCY, PRICE, CHANGE };

    }
}
