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
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.bavya.stocker.R;
import com.bavya.stocker.model.Stock;


public class EditStockFragment extends DialogFragment implements View.OnClickListener {

    private AutoCompleteTextView mTextViewSymbol;
    private EditText mTextViewPicker;
    private Button mUpdate, mCancel, mDelete;

    private OnUpdateStockListener mUpdateStockListener;

    private Stock mStock;


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("key_stock", mStock);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mStock = getArguments().getParcelable("key_stock");
        View v =  inflater.inflate(R.layout.dialog_edit_del_stock, container, false);
        mTextViewSymbol = (AutoCompleteTextView) v.findViewById(R.id.autoCompleteADSymbol);
        mTextViewSymbol.setText(mStock.symbol);
        mTextViewSymbol.setClickable(false);
        mTextViewSymbol.setCursorVisible(false);
        mTextViewSymbol.setEnabled(false);

        mTextViewPicker = (EditText) v.findViewById(R.id.pickerAD);
        mTextViewPicker.setText(String.valueOf(mStock.change));
        mTextViewPicker.setSelectAllOnFocus(true);

        mUpdate = (Button) v.findViewById(R.id.buttonUpdateAD);
        mCancel = (Button) v.findViewById(R.id.buttonCancelAD);
        mDelete = (Button) v.findViewById(R.id.buttonDeleteAD);
        mUpdate.setOnClickListener(this);
        mCancel.setOnClickListener(this);
        mDelete.setOnClickListener(this);
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
        if (v.getId() == R.id.buttonUpdateAD) {
            if (mUpdateStockListener != null) {
                Editable symbol = mTextViewSymbol.getText();
                Editable change = mTextViewPicker.getText();
                try {
                    mUpdateStockListener.onUpdateStock(symbol.toString(),
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
        } else if (v.getId() == R.id.buttonDeleteAD) {
            mUpdateStockListener.onDeleteStock(mStock);
            dismiss();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mUpdateStockListener = (OnUpdateStockListener) activity;
    }

    public interface OnUpdateStockListener {
        public void onUpdateStock(String symbol, int change);
        public void onDeleteStock(Stock stock);
    }
}
