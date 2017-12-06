package com.inventum.vhp_service_app.activity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.inventum.vhp_service_app.R;
import com.inventum.vhp_service_app.fragment.HomeFragment;
import com.inventum.vhp_service_app.fragment.SensorsFragment;
import com.inventum.vhp_service_app.fragment.TerminalFragment;
import com.inventum.vhp_service_app.other.UsbCommunication;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements
        HomeFragment.OnFragmentInteractionListener,
        SensorsFragment.OnFragmentInteractionListener,
        TerminalFragment.OnFragmentInteractionListener {

    private static final int targetVendorID = 1155;
    private static final int targetProductID = 22336;

    private NavigationView navigationView;
    private DrawerLayout drawer;
    private View navHeader;
    private ImageView imgLogo;
    private Toolbar toolbar;

    public static int navItemIndex = 0;

    private static final String TAG_HOME = "home";
    private static final String TAG_TERMINAL = "terminal";
    private static final String TAG_SENSORS = "sensors";
    public static String CURRENT_TAG = TAG_HOME;

    private String[] activityTitles;

    private boolean shouldLoadHomeFragOnBackPress = true;
    private Handler mHandler;

    public static UsbCommunication comm = null;

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    PendingIntent mPermissionIntent;
    private UsbManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mHandler = new Handler();

        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        navHeader = navigationView.getHeaderView(0);
        imgLogo = navHeader.findViewById(R.id.img_logo);

        // load nav menu header data
        imgLogo.setImageResource(R.drawable.inventum_logo);

        // initializing navigation menu
        setUpNavigationView();



        activityTitles = getResources().getStringArray(R.array.nav_item_activity_titles);

//        textResponse = findViewById(R.id.textresponse);
//
//        editCommand = findViewById(R.id.editcommand);
//
//        buttonSend = findViewById(R.id.buttonsend);
//        buttonSend.setOnClickListener(buttonSendOnClickListener);
//        buttonSend.setEnabled(false);

        //register the broadcast receiver
        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        registerReceiver(mUsbReceiver, new IntentFilter(ACTION_USB_PERMISSION));
        registerReceiver(mUsbDeviceReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));
        registerReceiver(mUsbDeviceReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));

        if (savedInstanceState == null) {
            navItemIndex = 0;
            CURRENT_TAG = TAG_HOME;
            loadHomeFragment();
            //textStatus.setText("No device connected");
            getDevice();
        }
    }

//    OnClickListener buttonSendOnClickListener = new OnClickListener() {
//        @Override
//        public void onClick(View view) {
//            if (comm == null) {
//                textResponse.setText("No device connected");
//                return;
//            }
//            final String cmd = editCommand.getText().toString();
//            if (cmd.equals("")) return;
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    final String resp = comm.sendReceive(cmd);
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            textResponse.setText(resp);
//                        }
//                    });
//                }
//            }).start();
//        }
//    };

    @Override
    protected void onDestroy() {
        clearAll();
        super.onDestroy();
    }

    private void loadHomeFragment() {
        // selecting appropriate nav menu item
        selectNavMenu();

        // set toolbar title
        setToolbarTitle();

        // if user select the current navigation menu again, don't do anything
        // just close the navigation drawer
        if (getSupportFragmentManager().findFragmentByTag(CURRENT_TAG) != null) {
            drawer.closeDrawers();
            return;
        }

        // Sometimes, when fragment has huge data, screen seems hanging
        // when switching between navigation menus
        // So using runnable, the fragment is loaded with cross fade effect
        // This effect can be seen in GMail app
        Runnable mPendingRunnable = new Runnable() {
            @Override
            public void run() {
                // update the main content by replacing fragments
                Fragment fragment = getHomeFragment();
                FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
                fragmentTransaction.replace(R.id.frame, fragment, CURRENT_TAG);
                fragmentTransaction.commitAllowingStateLoss();
            }
        };

        // If mPendingRunnable is not null, then add to the message queue
        if (mPendingRunnable != null) {
            mHandler.post(mPendingRunnable);
        }

        //Closing drawer on item click
        drawer.closeDrawers();

        // refresh toolbar menu
        invalidateOptionsMenu();
    }

    private Fragment getHomeFragment() {
        switch (navItemIndex) {
            case 0:
                // home
                HomeFragment homeFragment = new HomeFragment();
                return homeFragment;
            case 1:
                // terminal
                TerminalFragment terminalFragment = new TerminalFragment();
                return terminalFragment;
            case 2:
                // sensors
                SensorsFragment sensorsFragment = new SensorsFragment();
                return sensorsFragment;
            default:
                return new HomeFragment();
        }
    }

    private void setToolbarTitle() {
        getSupportActionBar().setTitle(activityTitles[navItemIndex]);
    }

    private void selectNavMenu() {
        navigationView.getMenu().getItem(navItemIndex).setChecked(true);
    }

    private void setUpNavigationView() {
        //Setting Navigation View Item Selected Listener to handle the item click of the navigation menu
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {

            // This method will trigger on item Click of navigation menu
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {

                //Check to see which item was being clicked and perform appropriate action
                switch (menuItem.getItemId()) {
                    //Replacing the main content with ContentFragment Which is our Inbox View;
                    case R.id.nav_home:
                        navItemIndex = 0;
                        CURRENT_TAG = TAG_HOME;
                        break;
                    case R.id.nav_terminal:
                        navItemIndex = 1;
                        CURRENT_TAG = TAG_TERMINAL;
                        break;
                    case R.id.nav_sensors:
                        navItemIndex = 2;
                        CURRENT_TAG = TAG_SENSORS;
                        break;
                    case R.id.nav_about_us:
                        // launch new intent instead of loading fragment
                        startActivity(new Intent(MainActivity.this, AboutUsActivity.class));
                        drawer.closeDrawers();
                        return true;
                    default:
                        navItemIndex = 0;
                }

                //Checking if the item is in checked state or not, if not make it in checked state
                if (menuItem.isChecked()) {
                    menuItem.setChecked(false);
                } else {
                    menuItem.setChecked(true);
                }
                menuItem.setChecked(true);

                loadHomeFragment();

                return true;
            }
        });

        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.openDrawer, R.string.closeDrawer) {

            @Override
            public void onDrawerClosed(View drawerView) {
                // Code here will be triggered once the drawer closes as we dont want anything to happen so we leave this blank
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                // Code here will be triggered once the drawer open as we dont want anything to happen so we leave this blank
                super.onDrawerOpened(drawerView);
            }
        };

        //Setting the actionbarToggle to drawer layout
        drawer.setDrawerListener(actionBarDrawerToggle);

        //calling sync state is necessary or else your hamburger icon wont show up
        actionBarDrawerToggle.syncState();
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawers();
            return;
        }

        // This code loads home fragment when back key is pressed
        // when user is in other fragment than home
        if (shouldLoadHomeFragOnBackPress) {
            // checking if user is on other navigation menu
            // rather than home
            if (navItemIndex != 0) {
                navItemIndex = 0;
                CURRENT_TAG = TAG_HOME;
                loadHomeFragment();
                return;
            }
        }

        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        // show menu only when home fragment is selected
        if (navItemIndex == 0) {
            getMenuInflater().inflate(R.menu.main, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_logout) {
            Toast.makeText(getApplicationContext(), "Logout user!", Toast.LENGTH_LONG).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void clearAll() {
        if (comm != null) {
            comm.stop();
            comm = null;
        }
        //textDeviceName.setText("");
        //textInfo.setText("");
    }

    private void getDevice() {
        if (comm == null) {
            HashMap<String, UsbDevice> deviceList = manager.getDeviceList();

            for (UsbDevice device : deviceList.values()) {
                if (checkDevice(device) && checkPermission(device)) {
                    comm = new UsbCommunication(manager, device);
                    setDeviceInfo();
                    break;
                }
            }
        }
    }

    private void setDeviceInfo() {
        if (comm == null) return;

        //textStatus.setText("Device found");
        String s = "DeviceID: " + comm.getDevice().getDeviceId() + "\n" +
                "DeviceName: " + comm.getDevice().getDeviceName() + "\n" +
                "VendorID: " + comm.getDevice().getVendorId() + "\n" +
                "ProductID: " + comm.getDevice().getProductId();
        //textDeviceName.setText(s);
        //textInfo.setText(comm.getPort().getManufacturer() + "\n" + comm.getPort().getProduct());
    }

    private boolean checkDevice(UsbDevice device) {
        return comm == null &&
                device.getVendorId() == targetVendorID &&
                device.getProductId() == targetProductID;
    }

    private boolean checkPermission(UsbDevice device) {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if (manager.hasPermission(device)) {
            return true;
        }
        manager.requestPermission(device, mPermissionIntent);
        return false;
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (comm == null) {
                            comm = new UsbCommunication(manager, device);
                            setDeviceInfo();
                        }
                    }
                    else {
                        //textStatus.setText("Permission denied for device " + device);
                        clearAll();
                    }
                }
            }
        }
    };

    private final BroadcastReceiver mUsbDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                // Correct device and permission granted?
                if (checkDevice(device) && checkPermission(device)) {
                    comm = new UsbCommunication(manager, device);
                    setDeviceInfo();
                }

            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (comm != null && device.equals(comm.getDevice())) {
                    //textStatus.setText("Device detached");
                    clearAll();
                }
            }
        }
    };

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
