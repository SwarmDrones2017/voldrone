package com.example.sofiane.testdrone;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.parrot.arsdk.ARSDK;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DICTIONARY_KEY_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_ERROR_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerArgumentDictionary;
import com.parrot.arsdk.arcontroller.ARControllerDictionary;
import com.parrot.arsdk.arcontroller.ARControllerException;
import com.parrot.arsdk.arcontroller.ARDeviceController;
import com.parrot.arsdk.arcontroller.ARDeviceControllerListener;
import com.parrot.arsdk.arcontroller.ARFeatureARDrone3;
import com.parrot.arsdk.arcontroller.ARFeatureCommon;
import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_ENUM;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDevice;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceNetService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryException;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiver;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiverDelegate;
import com.parrot.arsdk.arsal.ARSALPrint;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements ARDeviceControllerListener, ARDiscoveryServicesDevicesListUpdatedReceiverDelegate {

    private ArrayList<BebopDrone> mBebopDrone = new ArrayList<BebopDrone>();
    private final static String TAG = "MainActivity";

    /////////Start discovery:
    private ARDiscoveryService mArdiscoveryService;
    private ServiceConnection mArdiscoveryServiceConnection;

    private void initDiscoveryService() {
        // create the service connection
        if (mArdiscoveryServiceConnection == null) {
            mArdiscoveryServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    mArdiscoveryService = ((ARDiscoveryService.LocalBinder) service).getService();

                    startDiscovery();
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    mArdiscoveryService = null;
                }
            };
        }

        if (mArdiscoveryService == null) {
            // if the discovery service doesn't exists, bind to it
            Intent i = new Intent(getApplicationContext(), ARDiscoveryService.class);
            getApplicationContext().bindService(i, mArdiscoveryServiceConnection, Context.BIND_AUTO_CREATE);
        } else {
            // if the discovery service already exists, start discovery
            startDiscovery();
        }
    }

    private void startDiscovery() {
        if (mArdiscoveryService != null) {
            mArdiscoveryService.start();
        }
    }

    List<ARDiscoveryDeviceService> deviceList;
    ///////////The libARDiscovery will let you know when BLE and Wifi devices have been found on network:
    ARDiscoveryServicesDevicesListUpdatedReceiver mArdiscoveryServicesDevicesListUpdatedReceiver;


    // your class should implement ARDiscoveryServicesDevicesListUpdatedReceiverDelegate
    private void registerReceivers() {
        mArdiscoveryServicesDevicesListUpdatedReceiver = new ARDiscoveryServicesDevicesListUpdatedReceiver(this);
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastMgr.registerReceiver(mArdiscoveryServicesDevicesListUpdatedReceiver, new IntentFilter(ARDiscoveryService.kARDiscoveryServiceNotificationServicesDevicesListUpdated));
    }

    @Override
    public void onServicesDevicesListUpdated() {
        Log.d(TAG, "onServicesDevicesListUpdated ...");

        if (mArdiscoveryService != null) {
            deviceList = mArdiscoveryService.getDeviceServicesArray();

            // Do what you want with the device list
            for (int i = 0; i < deviceList.size(); i++) {
                sortie.append(deviceList.get(i).getName() + "\n");
            }
        }
    }

    ///////////////Once you have the ARService you want to use, transform it into an ARDiscoveryDevice (you will need it at the next step)
    private ARDiscoveryDevice createDiscoveryDevice(ARDiscoveryDeviceService service) {
        ARDiscoveryDevice device = null;
        //if ((service != null) && (ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_ARDRONE.equals(ARDiscoveryService.getProductFromProductID(service.getProductID())))) {

        Log.d(TAG, "La ligne n'est pas suaté");
        try {
            device = new ARDiscoveryDevice();
        } catch (ARDiscoveryException e) {
            e.printStackTrace();
            Log.e(TAG, "Error: " + e.getError());
        }
        ARDiscoveryDeviceNetService netDeviceService = (ARDiscoveryDeviceNetService) service.getDevice();
        device.initWifi(ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_ARDRONE, netDeviceService.getName(), netDeviceService.getIp(), netDeviceService.getPort());
        Log.d(TAG, "Après");

        //} catch (ARDiscoveryException e) {
        //  e.printStackTrace();
        // Log.e(TAG, "Error: " + e.getError());
        //}
        //}

        return device;
    }

    ///////////////Clean everything:
    private void unregisterReceivers() {
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());

        localBroadcastMgr.unregisterReceiver(mArdiscoveryServicesDevicesListUpdatedReceiver);
    }

    private void closeServices() {
        Log.d(TAG, "closeServices ...");

        if (mArdiscoveryService != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mArdiscoveryService.stop();

                    getApplicationContext().unbindService(mArdiscoveryServiceConnection);
                    mArdiscoveryService = null;
                }
            }).start();
        }
    }


    ArrayList<ARDeviceController> deviceController = new ArrayList<ARDeviceController>();
    Button btest;
    Button bdecolle;
    Button burgence;
    Button batteri;
    Button bavance;
    Button bstop;
    TextView sortie;
    MainActivity objectMain = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sortie = (TextView) findViewById(R.id.sortie);
        btest = (Button) findViewById(R.id.btest);
        btest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Bouton appuyé !");
                if (deviceList != null && deviceList.size() != 0) {
                    try {
                        for (int i = 0; i < deviceList.size(); i++) {
                            sortie.append(deviceList.get(i).getName() + "\n");
                        }
                        ArrayList<ARDiscoveryDevice> device = new ArrayList<ARDiscoveryDevice>();
                        for (int i = 0; i < deviceList.size(); i++) {
                            device.add(createDiscoveryDevice(deviceList.get(i)));

                        }

                        if (device != null) {
                            for (int i = 0; i < device.size(); i++) {
                                deviceController.add(new ARDeviceController(device.get(i)));
                                deviceController.get(i).addListener(objectMain);
                            }

                        }
                    } catch (ARControllerException e) {
                        e.printStackTrace();
                    }
                } else {
                    initDiscoveryService();
                }
            }
        });
        bdecolle = (Button) findViewById(R.id.bdecolle);
        bdecolle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //ARCONTROLLER_ERROR_ENUM error = deviceController.start();
                //deviceController.getFeatureARDrone3().sendPilotingTakeOff();
                for (int i = 0; i < deviceController.size(); i++) {
                    ARCONTROLLER_ERROR_ENUM error = deviceController.get(i).start();
                    getPilotingState(deviceController.get(i));
                    takeoff(deviceController.get(i));
                }
            }
        });
        burgence = (Button) findViewById(R.id.burgence);
        burgence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < deviceController.size(); i++) {
                    deviceController.get(i).getFeatureARDrone3().sendPilotingEmergency();
                }

            }
        });
        batteri = (Button) findViewById(R.id.batteri);
        batteri.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < deviceController.size(); i++) {
                    land(deviceController.get(i));
                }
            }
        });

        bavance = (Button) findViewById(R.id.bavance);
        bavance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < deviceController.size(); i++) {
                    deviceController.get(i).getFeatureARDrone3().setPilotingPCMDFlag((byte) 1);
                    deviceController.get(i).getFeatureARDrone3().setPilotingPCMDPitch((byte) 50);
                }

            }
        });

        bstop = (Button) findViewById(R.id.bstop);
        bstop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < deviceController.size(); i++) {
                    deviceController.get(i).getFeatureARDrone3().setPilotingPCMDFlag((byte) 1);
                    deviceController.get(i).getFeatureARDrone3().setPilotingPCMDPitch((byte) 0);
                }
            }
        });

        //BebopDrone bebop = new BebopDrone()

        ARSDK.loadSDKLibs();
        initDiscoveryService();
        registerReceivers();
    }

    @Override
    public void onStateChanged(ARDeviceController deviceController, ARCONTROLLER_DEVICE_STATE_ENUM newState, ARCONTROLLER_ERROR_ENUM error) {
        switch (newState) {
            case ARCONTROLLER_DEVICE_STATE_RUNNING:
                Log.d(TAG, "Etat : RUNNING");
                break;
            case ARCONTROLLER_DEVICE_STATE_STOPPED:
                Log.d(TAG, "Etat : STOPé");
                break;
            case ARCONTROLLER_DEVICE_STATE_STARTING:
                Log.d(TAG, "Etat : Commence");
                break;
            case ARCONTROLLER_DEVICE_STATE_STOPPING:
                Log.d(TAG, "Etat : Entraint d'être stopé");
                break;

            default:
                break;
        }
    }

    @Override
    public void onExtensionStateChanged(ARDeviceController deviceController, ARCONTROLLER_DEVICE_STATE_ENUM newState, ARDISCOVERY_PRODUCT_ENUM product, String name, ARCONTROLLER_ERROR_ENUM error) {

    }

    @Override
    public void onCommandReceived(ARDeviceController deviceController, ARCONTROLLER_DICTIONARY_KEY_ENUM commandKey, ARControllerDictionary elementDictionary) {
        if (elementDictionary != null) {
            // if the command received is a battery state changed
            if (commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_COMMON_COMMONSTATE_BATTERYSTATECHANGED) {
                ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);

                if (args != null) {
                    Integer batValue = (Integer) args.get(ARFeatureCommon.ARCONTROLLER_DICTIONARY_KEY_COMMON_COMMONSTATE_BATTERYSTATECHANGED_PERCENT);

                    // do what you want with the battery level
                    Log.d(TAG, "Batteri : " + batValue);
                }
            }
        } else {
            Log.e(TAG, "elementDictionary is null");
        }
    }

    private ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM getPilotingState(ARDeviceController deviceController) {
        ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM flyingState = ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.eARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_UNKNOWN_ENUM_VALUE;
        if (deviceController != null) {
            try {
                ARControllerDictionary dict = deviceController.getCommandElements(ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED);
                if (dict != null) {
                    ARControllerArgumentDictionary<Object> args = dict.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
                    if (args != null) {
                        Integer flyingStateInt = (Integer) args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE);
                        flyingState = ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.getFromValue(flyingStateInt);
                    }
                }
            } catch (ARControllerException e) {
                e.printStackTrace();
            }
        }
        return flyingState;
    }

    private void takeoff(ARDeviceController deviceController) {
        if (ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED.equals(getPilotingState(deviceController))) {
            ARCONTROLLER_ERROR_ENUM error = deviceController.getFeatureARDrone3().sendPilotingTakeOff();

            if (!error.equals(ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK)) {
                ARSALPrint.e(TAG, "Error while sending take off: " + error);
            }
        }
    }

    private void land(ARDeviceController deviceController) {
        ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM flyingState = getPilotingState(deviceController);
        if (ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING.equals(flyingState) ||
                ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING.equals(flyingState)) {
            ARCONTROLLER_ERROR_ENUM error = deviceController.getFeatureARDrone3().sendPilotingLanding();

            if (!error.equals(ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK)) {
                ARSALPrint.e(TAG, "Error while sending take off: " + error);
            }
        }
    }

}

