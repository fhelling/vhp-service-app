package com.inventum.vhp_service_app.activity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
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
import java.util.Objects;

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
            getDevice();
        }
    }

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

        if (!CURRENT_TAG.equals(TAG_SENSORS)) {
            SensorsFragment fragment = (SensorsFragment) getSupportFragmentManager().findFragmentByTag(TAG_SENSORS);
            if (fragment != null) {
                fragment.stop();
            }
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
                return new HomeFragment();
            case 1:
                // terminal
                return new TerminalFragment();
            case 2:
                // sensors
                return new SensorsFragment();
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
        drawer.addDrawerListener(actionBarDrawerToggle);

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
    }

    private void getDevice() {
        if (comm == null) {
            HashMap<String, UsbDevice> deviceList = manager.getDeviceList();

            for (UsbDevice device : deviceList.values()) {
                if (checkDevice(device) && checkPermission(device)) {
                    comm = new UsbCommunication(manager, device);
                    return;
                }
            }
        }
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
                    HomeFragment homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag(TAG_HOME);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (comm == null) {
                            comm = new UsbCommunication(manager, device);
                            homeFragment.setDeviceDetached();
                        }
                    }
                    else {
                        homeFragment.setDeviceDenied();
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
            HomeFragment homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag(TAG_HOME);
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                // Correct device and permission granted?
                if (checkDevice(device) && checkPermission(device)) {
                    comm = new UsbCommunication(manager, device);
                    if (CURRENT_TAG.equals(TAG_HOME)) {
                        homeFragment.setDeviceAttached();
                    }
                }

            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                //UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                //if (comm != null && device.equals(comm.getDevice())) {
                if (comm != null) {
                    if (CURRENT_TAG.equals(TAG_HOME)) {
                        homeFragment.setDeviceDetached();
                    }
                    clearAll();
                }
            }
        }
    };

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
