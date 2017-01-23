package site.withoutcaps.tcpclientserversample;

import android.Manifest;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import site.withoutcaps.tcpclientserversample.TCP.TCPClient;
import site.withoutcaps.tcpclientserversample.TCP.TCPServer;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final int PERMISSIONS_REQUEST_INTERNET = 0;
    private static final String TAG = "MainActivity";
    private static final int PAGE_CLIENT = 0;
    private static final int PAGE_SERVER = 1;

    private int currentPage;
    private DrawerLayout mDrawerLayout;
    private MainFragment tcpClientFragment;
    private MainFragment tcpServerFragment;

    private TCPClient tcpClient;
    private TCPServer tcpServer;

    private EditText messageTxt;
    private Button clientBtn;
    private Button serverBtn;
    private MenuItem disconnectBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ActionBar ab = getSupportActionBar();
        ab.setHomeAsUpIndicator(R.drawable.ic_menu);
        ab.setDisplayHomeAsUpEnabled(true);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        messageTxt = (EditText) findViewById(R.id.message_txt);

        setupDrawerContent(navigationView);
        setupViewPager(viewPager);
        tabLayout.setupWithViewPager(viewPager);

        askForPremmisions();

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                currentPage = position;
                if (position == PAGE_CLIENT) {
                    updateUI(tcpClient != null ? tcpClient.isConnected() : false);
                    disconnectBtn.setVisible(tcpClient != null ? tcpClient.isConnected() : false);
                    disconnectBtn.setTitle(getResources().getString(R.string.client_disconnect_btn));
                } else if (position == PAGE_SERVER) {
                    updateUI(tcpServer != null ? tcpServer.isServerRunning() : false);
                    disconnectBtn.setVisible(tcpServer != null ? tcpServer.isServerRunning() : false);
                    disconnectBtn.setTitle(getResources().getString(R.string.server_disconnect_btn));
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        findViewById(R.id.send_btn).setOnClickListener(this);
        new UIListenersThread().execute();
    }

    private void updateUI(boolean state) {
        messageTxt.setFocusable(state);
        messageTxt.setClickable(state);
        messageTxt.setCursorVisible(state);
        messageTxt.setFocusableInTouchMode(state);
    }

    public void click(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog, null);
        final EditText ipTxt = (EditText) v.findViewById(R.id.ip_txt);

        if (currentPage == PAGE_CLIENT) {
            clientBtn = (Button) view;

            builder.setView(v)
                    .setTitle(getResources().getString(R.string.client_dialog_title))
                    .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            String address = ipTxt.getText().toString();
                            if (address.contains(".") && address.contains(":"))
                                new tcpClientThread().execute(address);
                        }
                    })
                    .setNegativeButton(getResources().getString(R.string.cancel), null);
        } else if (currentPage == PAGE_SERVER) {
            serverBtn = (Button) view;
            ipTxt.setHint("1024");
            builder.setView(v)
                    .setTitle(getResources().getString(R.string.server_dialog_title))
                    .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            if (ipTxt.getText().toString().length() > 0 && Integer.parseInt(ipTxt.getText().toString()) > 1023)
                                new tcpServerThread().execute(ipTxt.getText().toString());
                            else
                                Toast.makeText(getApplicationContext(), "Ports lower then 1024 are not allowed", Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNegativeButton(getResources().getString(R.string.cancel), null);
        }

        builder.create().show();
    }

    private void askForPremmisions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.INTERNET)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, PERMISSIONS_REQUEST_INTERNET);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_INTERNET: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "granted: ");

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {
                    Log.d(TAG, "fuck: ");

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (tcpClient != null)
            tcpClient.stopClient();
        if (tcpServer != null)
            tcpServer.closeServer();
    }

    @Override
    public void onClick(View v) {
        if (currentPage == PAGE_CLIENT) {
            if (tcpClient.isConnected()) {
                tcpClient.sendLn(messageTxt.getText().toString());
                tcpClientFragment.getConsole().append("Client: " + messageTxt.getText().toString() + System.getProperty("line.separator"));
            }
        } else if (currentPage == PAGE_SERVER) {
            if (tcpServer.isServerRunning()) {
                tcpServer.broadcastln(messageTxt.getText().toString());
                tcpServerFragment.getConsole().append("Server: " + messageTxt.getText().toString() + System.getProperty("line.separator"));
                Log.d(TAG, "Client count: " + tcpServer.getClientsCount());
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        disconnectBtn = menu.findItem(R.id.action_disconnect);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            case R.id.action_disconnect:
                if (currentPage == PAGE_CLIENT) {
                    tcpClient.stopClient();
                } else if (currentPage == PAGE_SERVER) {
                    Log.d(TAG, "Closing server");
                    tcpServer.closeServer();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupViewPager(ViewPager viewPager) {
        Adapter adapter = new Adapter(getSupportFragmentManager());
        tcpClientFragment = MainFragment.newInstance(getResources().getString(R.string.connect));
        tcpServerFragment = MainFragment.newInstance(getResources().getString(R.string.listen));

        adapter.addFragment(tcpClientFragment, "TCP Client");
        adapter.addFragment(tcpServerFragment, "TCP Server");

        viewPager.setAdapter(adapter);
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        menuItem.setChecked(true);
                        mDrawerLayout.closeDrawers();
                        return true;
                    }
                });
    }

    //------------------------------------[TCP Listeners Thread]------------------------------------//

    public class UIListenersThread extends AsyncTask<String, String, Void> {
        @Override
        protected Void doInBackground(String... params) {
            tcpServer = new TCPServer();
            tcpClient = new TCPClient();
            tcpServer.setOnServerStartListener(new TCPServer.OnServerStart() {
                @Override
                public void serverStarted(int port) {
                    publishProgress("serverStarted");    //I DONT ADVISE TO USE THIS ADVANCED TECNIQUE :)

                }
            });
            tcpServer.setOnServerClosedListener(new TCPServer.OnServerClose() {
                @Override
                public void serverClosed(int port) {
                    publishProgress("serverClosed");    //I DONT ADVISE TO USE THIS ADVANCED TECNIQUE :)
                }
            });
            tcpServer.setOnConnectListener(new TCPServer.OnConnect() {
                @Override
                public void connected(Socket socket, InetAddress localAddress, int port, SocketAddress localSocketAddress, int clientIndex) {
                    publishProgress("Client " + localAddress + " Connected, Index: " + clientIndex);
                }
            });

            tcpServer.setOnDisconnectListener(new TCPServer.OnDisconnect() {
                @Override
                public void disconnected(Socket socket, InetAddress localAddress, int port, SocketAddress localSocketAddress, int clientIndex) {
                    publishProgress("Client " + clientIndex + " Disconnected");

                }
            });
            tcpClient.setOnConnectListener(new TCPClient.OnConnect() {
                @Override
                public void connected(Socket socket, String ip, int port) {
                    publishProgress("Connected to: " + ip + ":" + port);
                }
            });

            tcpClient.setOnDisconnectListener(new TCPClient.OnDisconnect() {
                @Override
                public void disconnected(String ip, int port) {
                    publishProgress("Disconnected: " + ip + ":" + port);
                }
            });
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            switch (values[0]) {
                case "serverStarted":
                    serverBtn.setVisibility(View.INVISIBLE);
                    disconnectBtn.setVisible(true);
                    tcpServerFragment.getConsole().append(Html.fromHtml("<font color=#9E9E9E>Server Started" + "</font><br>"));
                    break;
                case "serverClosed":
                    serverBtn.setVisibility(View.VISIBLE);
                    disconnectBtn.setVisible(false);
                    tcpServerFragment.getConsole().append(Html.fromHtml("<font color=#9E9E9E>Server Closed" + "</font><br>"));
                    break;
                default:
            }
            if (values[0].contains("Client ") && values[0].contains(" Disconnected")) {
                messageTxt.setFocusableInTouchMode(tcpServer.getClientsCount() > 0);
                tcpServerFragment.getConsole().append(Html.fromHtml("<font color=#C62828>" + values[0] + "</font><br>"));
            } else if (values[0].contains("Client ") && values[0].contains(" Connected, Index: ")) {
                messageTxt.setFocusableInTouchMode(true);
                tcpServerFragment.getConsole().append(Html.fromHtml("<font color=#558B2F>" + values[0] + "</font><br>"));
            } else if (values[0].contains("Disconnected: ")) {
                clientBtn.setVisibility(View.VISIBLE);
                updateUI(false);
                tcpClientFragment.getConsole().append(Html.fromHtml("<font color=#C62828>" + values[0] + "</font><br>"));
            } else if (values[0].contains("Connected")) {
                clientBtn.setVisibility(View.INVISIBLE);
                updateUI(true);
                tcpClientFragment.getConsole().append(Html.fromHtml("<font color=#558B2F>" + values[0] + "</font><br>"));
            }
        }
    }
    //------------------------------------[TCP Server Thread]------------------------------------//

    public class tcpServerThread extends AsyncTask<String, String, Void> {

        @Override
        protected Void doInBackground(String... port) {
            tcpServer.setOnMessageReceivedListener(new TCPServer.OnMessageReceived() {
                @Override
                public void messageReceived(String message, int clientIndex) {
                    publishProgress("Client " + clientIndex + ": " + message);
                }
            });

            tcpServer.startServer(port[0]);
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            Log.d(TAG, "onProgressUpdate: " + values[0]);
            tcpServerFragment.getConsole().append(Html.fromHtml("<font color=#283593>" + values[0] + "</font><br>"));
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            serverBtn.setVisibility(View.VISIBLE);
        }
    }


    //------------------------------------[TCP Client Thread]------------------------------------//
    public class tcpClientThread extends AsyncTask<String, String, Void> {

        @Override
        protected Void doInBackground(String... ip) {
            tcpClient.setOnMessageReceivedListener(new TCPClient.OnMessageReceived() {
                @Override
                public void messageReceived(String message) {
                    publishProgress("Server: " + message);
                }
            });

            try {
                String[] address = ip[0].split(":");
                tcpClient.connect(address[0], address[1]);
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            tcpClientFragment.getConsole().append(Html.fromHtml("<font color=#283593>" + values[0] + "</font><br>"));
            Log.d(TAG, values[0]);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            clientBtn.setVisibility(View.VISIBLE);
        }
    }

    //------------------------------------[Fragment Pager Adapter]------------------------------------//

    static class Adapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragments = new ArrayList<>();
        private final List<String> mFragmentTitles = new ArrayList<>();

        public Adapter(FragmentManager fm) {
            super(fm);
        }

        public void addFragment(Fragment fragment, String title) {
            mFragments.add(fragment);
            mFragmentTitles.add(title);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitles.get(position);
        }
    }
}
