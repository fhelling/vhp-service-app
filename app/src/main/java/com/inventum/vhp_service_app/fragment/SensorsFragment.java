package com.inventum.vhp_service_app.fragment;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.inventum.vhp_service_app.R;

import org.json.JSONException;
import org.json.JSONObject;

import static com.inventum.vhp_service_app.activity.MainActivity.comm;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SensorsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SensorsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SensorsFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private TextView textAirin;
    private TextView textAirout;
    private TextView textChreturn;
    private TextView textChsupply;
    private TextView textHotgas;
    private TextView textEvap;

    private boolean running = false;

    private OnFragmentInteractionListener mListener;

    public SensorsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SensorsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SensorsFragment newInstance(String param1, String param2) {
        SensorsFragment fragment = new SensorsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_sensors, container, false);
        textAirin = view.findViewById(R.id.text_airin);
        textAirout = view.findViewById(R.id.text_airout);
        textChreturn = view.findViewById(R.id.text_chreturn);
        textChsupply = view.findViewById(R.id.text_chsupply);
        textHotgas = view.findViewById(R.id.text_hotgas);
        textEvap = view.findViewById(R.id.text_evap);
        running = true;
        if (comm != null) {
            getData();
        }
        return view;
    }

    private void getData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (running) {
                    String resp = comm.sendReceive("^json device");
                    resp = resp.replace("^", "");
                    try {
                        JSONObject jObject = new JSONObject(resp);
                        final String airin = jObject.getString("AirIn");
                        final String airout = jObject.getString("AirOut");
                        final String chreturn = jObject.getString("ChReturn");
                        final String chsupply = jObject.getString("ChSupply");
                        final String hotgas = jObject.getString("Hotgas");
                        final String evap = jObject.getString("Evap");
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textAirin.setText(airin);
                                textAirout.setText(airout);
                                textChreturn.setText(chreturn);
                                textChsupply.setText(chsupply);
                                textHotgas.setText(hotgas);
                                textEvap.setText(evap);
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public void stop() {
        running = false;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
