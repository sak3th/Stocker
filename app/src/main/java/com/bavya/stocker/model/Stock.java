package com.bavya.stocker.model;


import android.os.Parcel;
import android.os.Parcelable;

public class Stock implements Parcelable {

    public String name, symbol, currency, price;
    public int change;

    public Stock(yahoofinance.Stock s) {
        name = new String(s.getName().split(" ")[0].trim().split(",")[0]);
        symbol = new String(s.getSymbol());
        currency = currencyToSymbol(s.getCurrency());
        price = new String("" + s.getQuote().getPrice());
    }

    public Stock() {}

    public Stock(Stock s) {
        name = new String(s.name);
        symbol = new String(s.symbol);
        currency = new String(s.currency);
        price = new String(s.price);
        change = s.change;
    }

    public static String currencyToSymbol(String currency) {
        switch (currency) {
            case "USD" :
                return "$";
            case "EUR" :
                return "€";
            default:
                return "#";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stock stock = (Stock) o;
        return symbol.equals(stock.symbol);
    }

    @Override
    public int hashCode() {
        return symbol.hashCode();
    }

    @Override
    public String toString() {
        return "Stock{" +
                "name='" + name + '\'' +
                ", symbol='" + symbol + '\'' +
                ", currency='" + currency + '\'' +
                ", price='" + price + '\'' +
                ", change=" + change +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(symbol);
        dest.writeString(currency);
        dest.writeString(price);
        dest.writeInt(change);
    }

    public static final Parcelable.Creator<Stock> CREATOR
            = new Parcelable.Creator<Stock>() {
        public Stock createFromParcel(Parcel in) {
            return new Stock(in);
        }

        public Stock[] newArray(int size) {
            return new Stock[size];
        }
    };

    private Stock(Parcel in) {
        name = in.readString();
        symbol = in.readString();
        currency = in.readString();
        price = in.readString();
        change = in.readInt();
    }
}
