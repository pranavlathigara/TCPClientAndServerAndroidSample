package site.withoutcaps.tcpclientserversample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class MainFragment extends Fragment {

    private static final String TAG = "MainFragment";
    private static final String BUTTONTXT = "BUTTONTXT";
    private TextView console;
    private Button startBtn;
    public static MainFragment newInstance(String buttonTxt) {
        MainFragment fragment = new MainFragment();
        Bundle args = new Bundle();
        args.putString(BUTTONTXT, buttonTxt);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_main, container, false);
        console = (TextView) v.findViewById(R.id.console_txt);
        startBtn = (Button) v.findViewById(R.id.start_btn);
        if (getArguments() != null)
            startBtn.setText(getArguments().getString(BUTTONTXT));
        return v;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    public TextView getConsole() {
        return console;
    }

    public Button getStartBtn() {
        return startBtn;
    }
}

