package com.bavya.stocker.activity;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.bavya.stocker.R;


public class AddStockFragment extends DialogFragment implements View.OnClickListener {

    private AutoCompleteTextView mTextViewSymbol;
    private EditText mTextViewPicker;
    private Button mAdd, mCancel;

    private OnAddStockListener mAddStockListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v =  inflater.inflate(R.layout.dialog_add_stock, container, false);
        String[] symbols = getResources().getStringArray(R.array.symbols_array);
        mTextViewSymbol = (AutoCompleteTextView) v.findViewById(R.id.autoCompleteADSymbol);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getActivity(), android.R.layout.simple_list_item_1, symbols);
        mTextViewSymbol.setAdapter(adapter);

        String[] changes = getResources().getStringArray(R.array.changes_array);
        mTextViewPicker = (EditText) v.findViewById(R.id.pickerAD);
        mTextViewPicker.setText("1");

        mAdd = (Button) v.findViewById(R.id.buttonAddAD);
        mCancel = (Button) v.findViewById(R.id.buttonCancelAD);
        mAdd.setOnClickListener(this);
        mCancel.setOnClickListener(this);
        return v;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog d = getDialog();
        if (d != null) {
            int width = getResources().getDimensionPixelSize(R.dimen.dialog_width);
            d.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.buttonAddAD) {
            if (mAddStockListener != null) {
                Editable symbol = mTextViewSymbol.getText();
                Editable change = mTextViewPicker.getText();
                if (symbol == null || symbol.toString().trim().length() == 0) {
                    Toast.makeText(getActivity(), "Incorrect data", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    mAddStockListener.onAddStock(symbol.toString(),
                            Integer.valueOf(change.toString()));
                    dismiss();
                } catch (NumberFormatException e) {
                    Toast.makeText(getActivity(), "Incorrect data", Toast.LENGTH_SHORT).show();
                } catch (NullPointerException e) {
                    Toast.makeText(getActivity(), "Incorrect data", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (v.getId() == R.id.buttonCancelAD) {
            dismiss();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mAddStockListener = (OnAddStockListener) activity;
    }

    public interface OnAddStockListener {
        public void onAddStock(String symbol, int change);
    }
}
