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

import static com.inventum.vhp_service_app.activity.MainActivity.comm;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link HomeFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    private TextView textDeviceName;
    private TextView textStatus;
    private TextView textInfo;

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
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
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        textStatus = view.findViewById(R.id.text_status);
        textDeviceName = view.findViewById(R.id.text_device_name);
        textInfo = view.findViewById(R.id.text_info);
        setDeviceAttached();
        return view;
    }

    public void setDeviceNone() {
        textStatus.setText(R.string.home_status_none);
        textDeviceName.setText("");
        textInfo.setText("");
    }

    public void setDeviceAttached() {
        if (comm != null) {
            textStatus.setText(R.string.home_status_attached);
            String name = "DeviceID: " + comm.getDevice().getDeviceId() + "\n" +
                    "DeviceName: " + comm.getDevice().getDeviceName() + "\n" +
                    "VendorID: " + comm.getDevice().getVendorId() + "\n" +
                    "ProductID: " + comm.getDevice().getProductId();
            textDeviceName.setText(name);
            String info = comm.getPort().getManufacturer() + "\n" +
                    comm.getPort().getProduct();
            textInfo.setText(info);
        }
        else {
            setDeviceNone();
        }
    }

    public void setDeviceDetached() {
        textStatus.setText(R.string.home_status_detached);
        textDeviceName.setText("");
        textInfo.setText("");
    }

    public void setDeviceDenied() {
        textStatus.setText(R.string.home_status_denied);
        textDeviceName.setText("");
        textInfo.setText("");
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
