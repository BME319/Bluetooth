/* Copyright (c) 2011 - Andago
 * 
 * author: 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package cn.edu.zju.bme319.cordova;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;

import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import android.R.string;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;





public class ExtraInfo extends CordovaPlugin {

	private final static String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB"; // SPP服务UUID号

	public static final String ACTION_DISCOVER_DEVICES="listDevices";
	public static final String ACTION_LIST_BOUND_DEVICES="listBoundDevices";
	public static final String ACTION_IS_BT_ENABLED="isBTEnabled";
	public static final String ACTION_ENABLE_BT="enableBT";
	public static final String ACTION_DISABLE_BT="disableBT";
	public static final String ACTION_PAIR_BT="pairBT";
	public static final String ACTION_UNPAIR_BT="unPairBT";
	public static final String ACTION_STOP_DISCOVERING_BT="stopDiscovering";
	public static final String ACTION_IS_BOUND_BT="isBound";
	public static final String ACTION_BT_CONNECT="connect";
	public static final String ACTION_BT_GETDATA="getData";
	
	private static BluetoothAdapter btadapter;	
	//private static BluetoothPlugin BluetoothPlugin;
	
	private ArrayList<BluetoothDevice> found_devices;
	private boolean discovering = false;
	private Context context;
	private static BluetoothSocket bluetoothSocket = null;
	private String deviceAddress = "";
	private Method m;
	private boolean isConnection = false;
	private static InputStream inputStream = null;
	static private Handler handler;
	boolean bRun = true;
	boolean bThread = false;
	private String returnBTData = "";
	private int sendCommandFlag = 0;
	 private ConnectedThread mConnectedThread;

	 
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) 
            throws JSONException {
        Activity activity = this.cordova.getActivity();
        context = (Context) this.context;
		
		 // Register for broadcasts when a device is discovered
      IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
      context.registerReceiver(mReceiver, filter);
      
      
      // Register for broadcasts when discovery starts
      filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
      context.registerReceiver(mReceiver, filter);

      
      // Register for broadcasts when discovery has finished
      filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
      context.registerReceiver(mReceiver, filter);  
      
             
      // Register for broadcasts when connectivity state changes
      filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
      context.registerReceiver(mReceiver, filter);  
      
      Looper.prepare();

      found_devices = new ArrayList<BluetoothDevice>(); 
		
        if (btadapter == null) {
	    	btadapter = BluetoothAdapter.getDefaultAdapter();
        }

        if (action.equals("getExtra")) {
   
        	callbackContext.success("123");
            return true;
        }
        else if (ACTION_DISCOVER_DEVICES.equals(action)) {
        	try {
				
				Log.d("BluetoothPlugin", "We're in "+ACTION_DISCOVER_DEVICES);
				
				found_devices.clear();
				discovering=true;
				
		        if (btadapter.isDiscovering()) {
		        	btadapter.cancelDiscovery();
		        }
		        
		        Log.i("BluetoothPlugin","Discovering devices...");        
				btadapter.startDiscovery();		
				
				while (discovering){}
				
				String devicesFound=null;
				int count=0;
				devicesFound="[";
				for (BluetoothDevice device : found_devices) {
					Log.i("BluetoothPlugin",device.getName() + " "+device.getAddress()+" "+device.getBondState());
				   if ((device.getName()!=null) && (device.getBluetoothClass()!=null)){
					   devicesFound = devicesFound + " { \"name\" : \"" + device.getName() + "\" ," +
					   		"\"address\" : \"" + device.getAddress() + "\" ," +
							"\"class\" : \"" + device.getBluetoothClass().getDeviceClass() + "\" }";
					   if (count<found_devices.size()-1) devicesFound = devicesFound + ",";
				   }else Log.i("BluetoothPlugin",device.getName() + " Problems retrieving attributes. Device not added ");
				   count++;
				}	
				
				devicesFound= devicesFound + "] ";				
				
				Log.d("BluetoothPlugin - "+ACTION_DISCOVER_DEVICES, "Returning: "+ devicesFound);
				callbackContext.success(devicesFound);
				//result = new PluginResult(Status.OK, devicesFound);
			} catch (Exception Ex) {
				Log.d("BluetoothPlugin - "+ACTION_DISCOVER_DEVICES, "Got Exception "+ Ex.getMessage());
				callbackContext.error("discoveryError");
				//result = new PluginResult(Status.ERROR);
			}
			

//			try {
//				
//				Log.d("BluetoothPlugin", "We're in "+ACTION_DISCOVER_DEVICES);
//
//				// Create a BroadcastReceiver for ACTION_FOUND
////				final BroadcastReceiver mReceiver = new BroadcastReceiver() {
////				    public void onReceive(Context context, Intent intent) {
////				        String action = intent.getAction();
////				        // When discovery finds a device
////				        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
////				            // Get the BluetoothDevice object from the Intent
////				            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
////				            // Add the name and address to an array adapter to show in a ListView
////				            mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
////				        }
////				    }
////				};
////				// Register the BroadcastReceiver
////				IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
////				registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
////				
////				 // 关闭再进行的服务查找
////		        if (btadapter.isDiscovering()) {
////		        	btadapter.cancelDiscovery();
////		        	Log.d("BluetoothPlugin", "We're in "+"12");
////		        }
////		        //并重新开始
////		        Log.d("BluetoothPlugin", "We're in "+"1234");
////		        btadapter.startDiscovery();
////		        Log.d("BluetoothPlugin", "We're in "+"123456");
////					
////				found_devices.clear();
////				//discovering=true;
////				
////		        //if (btadapter.isDiscovering()) {
////		        //	btadapter.cancelDiscovery();
////		        //}
////		        
////				SendCommand(0);
////				
////		        Log.i("BluetoothPlugin","Discovering devices...");        
////				//btadapter.startDiscovery();	
////				
////				while (discovering){}
////				
////				String devicesFound=null;
////				int count=0;
////				devicesFound="[";
////				for (BluetoothDevice device : found_devices) {
////					Log.i("BluetoothPlugin",device.getName() + " "+device.getAddress()+" "+device.getBondState());
////				   if ((device.getName()!=null) && (device.getBluetoothClass()!=null)){
////					   devicesFound = devicesFound + " { \"name\" : \"" + device.getName() + "\" ," +
////					   		"\"address\" : \"" + device.getAddress() + "\" ," +
////							"\"class\" : \"" + device.getBluetoothClass().getDeviceClass() + "\" }";
////					   if (count<found_devices.size()-1) devicesFound = devicesFound + ",";
////				   }else Log.i("BluetoothPlugin",device.getName() + " Problems retrieving attributes. Device not added ");
////				   count++;
////				}	
////				
////				devicesFound= devicesFound + "] ";				
////				
////				Log.d("BluetoothPlugin - "+ACTION_DISCOVER_DEVICES, "Returning: "+ devicesFound);
////				callbackContext.success(devicesFound);
//				//result = new PluginResult(Status.OK, devicesFound);
//				return true;
//			} catch (Exception Ex) {
//				Log.d("BluetoothPlugin - "+ACTION_DISCOVER_DEVICES, "Got Exception "+ Ex.getMessage());
//				//result = new PluginResult(Status.ERROR);
//				callbackContext.error("discoverError");
//				return false;
//			}
			
		
		} else 	if (ACTION_IS_BT_ENABLED.equals(action)) {
			try {							
				Log.d("BluetoothPlugin", "We're in "+ACTION_IS_BT_ENABLED);
				
				boolean isEnabled = btadapter.isEnabled();
				
				Log.d("BluetoothPlugin - "+ACTION_IS_BT_ENABLED, "Returning "+ "is Bluetooth Enabled? "+isEnabled);
				callbackContext.success(""+isEnabled);
				//result = new PluginResult(Status.OK, isEnabled);
				return true;
			} catch (Exception Ex) {
				Log.d("BluetoothPlugin - "+ACTION_IS_BT_ENABLED, "Got Exception "+ Ex.getMessage());
				callbackContext.error("isBTEnabledError");
				//result = new PluginResult(Status.ERROR);
				return false;
			}
			
		} else 	if (ACTION_ENABLE_BT.equals(action)) {
			try {							
				Log.d("BluetoothPlugin", "We're in "+ACTION_ENABLE_BT);
				
				boolean enabled = false;
				
				Log.d("BluetoothPlugin", "Enabling Bluetooth...");
				
				if (btadapter.isEnabled())
				{
				  enabled = true;
				} else {
				  enabled = btadapter.enable();
				}

				
				Log.d("BluetoothPlugin - "+ACTION_ENABLE_BT, "Returning "+ "Result: "+enabled);
				callbackContext.success("" + enabled);
				//result = new PluginResult(Status.OK, enabled);
				return true;
			} catch (Exception Ex) {
				Log.d("BluetoothPlugin - "+ACTION_ENABLE_BT, "Got Exception "+ Ex.getMessage());
				callbackContext.error("EnableBTError");
				//result = new PluginResult(Status.ERROR);
				return false;
			}			
			
		} else 	if (ACTION_DISABLE_BT.equals(action)) {
			try {							
				Log.d("BluetoothPlugin", "We're in "+ACTION_DISABLE_BT);
				
				boolean disabled = false;
				
				Log.d("BluetoothPlugin", "Disabling Bluetooth...");
				
				if (btadapter.isEnabled())
				{
					disabled = btadapter.disable();
				} else {
					disabled = true;
				}				
								
				Log.d("BluetoothPlugin - "+ACTION_DISABLE_BT, "Returning "+ "Result: "+disabled);
				callbackContext.success("" + disabled);
				//result = new PluginResult(Status.OK, disabled);
				return true;
			} catch (Exception Ex) {
				Log.d("BluetoothPlugin - "+ACTION_DISABLE_BT, "Got Exception "+ Ex.getMessage());
				callbackContext.error("DisableBTError");
				//result = new PluginResult(Status.ERROR);
				return false;
			}
					
		} else 	if (ACTION_PAIR_BT.equals(action)) {
			try {							
				Log.d("BluetoothPlugin", "We're in "+ACTION_PAIR_BT);
				
				String addressDevice = args.getString(0);
				
				if (btadapter.isDiscovering()) {
		        	btadapter.cancelDiscovery();
		        }

				BluetoothDevice device = btadapter.getRemoteDevice(addressDevice);
				boolean paired = false;
							
				Log.d("BluetoothPlugin","Pairing with Bluetooth device with name " + device.getName()+" and address "+device.getAddress());
		          	
				try {
					Method m = device.getClass().getMethod("createBond");
					paired = (Boolean) m.invoke(device);					
				} catch (Exception e) 
				{
					e.printStackTrace();
				}  
				
				
				Log.d("BluetoothPlugin - "+ACTION_PAIR_BT, "Returning "+ "Result: "+paired);
				callbackContext.success("" + paired);
				//result = new PluginResult(Status.OK, paired);
				return true;
			} catch (Exception Ex) {
				Log.d("BluetoothPlugin - "+ACTION_PAIR_BT, "Got Exception "+ Ex.getMessage());
				callbackContext.error("pairBTError");
				//result = new PluginResult(Status.ERROR);
				return false;
			}
			
						
		} else 	if (ACTION_UNPAIR_BT.equals(action)) {
			try {							
				Log.d("BluetoothPlugin", "We're in "+ACTION_UNPAIR_BT);
				
				String addressDevice = args.getString(0);
				
				if (btadapter.isDiscovering()) {
		        	btadapter.cancelDiscovery();
		        }

				BluetoothDevice device = btadapter.getRemoteDevice(addressDevice);
				boolean unpaired = false;
							
				Log.d("BluetoothPlugin","Unpairing Bluetooth device with " + device.getName()+" and address "+device.getAddress());
		          	
				try {
					Method m = device.getClass().getMethod("removeBond");
					unpaired = (Boolean) m.invoke(device);					
				} catch (Exception e) 
				{
					e.printStackTrace();
				}  
				
				
				Log.d("BluetoothPlugin - "+ACTION_UNPAIR_BT, "Returning "+ "Result: "+unpaired);
				callbackContext.success("" + unpaired);
				//result = new PluginResult(Status.OK, unpaired);
				return true;
			} catch (Exception Ex) {
				Log.d("BluetoothPlugin - "+ACTION_UNPAIR_BT, "Got Exception "+ Ex.getMessage());
				callbackContext.error("unpairBTError");
				//result = new PluginResult(Status.ERROR);
				return false;
			}
							
		} else 	if (ACTION_LIST_BOUND_DEVICES.equals(action)) {
			try {							
				Log.d("BluetoothPlugin", "We're in "+ACTION_LIST_BOUND_DEVICES);
				
				Log.d("BluetoothPlugin","Getting paired devices...");
				//获得所有已配对对象
				Set<BluetoothDevice> pairedDevices = btadapter.getBondedDevices();
				int count =0;	
				String resultBoundDevices="[ ";
				if (pairedDevices.size() > 0) {					
					for (BluetoothDevice device : pairedDevices) 
					{						
						Log.i("BluetoothPlugin",device.getName() + " "+device.getAddress()+" "+device.getBondState());
						
						if ((device.getName()!=null) && (device.getBluetoothClass()!=null)){
							resultBoundDevices = resultBoundDevices + " { \"name\" : \"" + device.getName() + "\" ," +
				   				"\"address\" : \"" + device.getAddress() + "\" ," +
				   				"\"class\" : \"" + device.getBluetoothClass().getDeviceClass() + "\" }";
							 if (count<pairedDevices.size()-1) resultBoundDevices = resultBoundDevices + ",";					   
						} else Log.i("BluetoothPlugin",device.getName() + " Problems retrieving attributes. Device not added ");
						 count++;
					 }			    
					
				}
				
				resultBoundDevices= resultBoundDevices + "] ";
				
				Log.d("BluetoothPlugin - "+ACTION_LIST_BOUND_DEVICES, "Returning "+ resultBoundDevices);
				callbackContext.success("" + resultBoundDevices);
				//result = new PluginResult(Status.OK, resultBoundDevices);
				return true;
			} catch (Exception Ex) {
				Log.d("BluetoothPlugin - "+ACTION_LIST_BOUND_DEVICES, "Got Exception "+ Ex.getMessage());
				callbackContext.error("resultBoundDevicesError");
				//result = new PluginResult(Status.ERROR);
				return false;
			}	
				
			
		} else 	if (ACTION_STOP_DISCOVERING_BT.equals(action)) {
			try {							
				Log.d("BluetoothPlugin", "We're in "+ACTION_STOP_DISCOVERING_BT);
				
				boolean stopped = true;
				
				Log.d("BluetoothPlugin", "Stop Discovering Bluetooth Devices...");
				
				if (btadapter.isDiscovering())
				{
					Log.i("BluetoothPlugin","Stop discovery...");	
					stopped = btadapter.cancelDiscovery();
		        	discovering=false;
				}				
				
			
				Log.d("BluetoothPlugin - "+ACTION_STOP_DISCOVERING_BT, "Returning "+ "Result: "+stopped);
				callbackContext.success("" + stopped);
				//result = new PluginResult(Status.OK, stopped);
				return true;
			} catch (Exception Ex) {
				Log.d("BluetoothPlugin - "+ACTION_STOP_DISCOVERING_BT, "Got Exception "+ Ex.getMessage());
				callbackContext.error("stoppedError");
				//result = new PluginResult(Status.ERROR);
				return false;
			}
			
			
		} else 	if (ACTION_IS_BOUND_BT.equals(action)) {
			try {							
				Log.d("BluetoothPlugin", "We're in "+ACTION_IS_BOUND_BT);
				String addressDevice = args.getString(0);
				BluetoothDevice device = btadapter.getRemoteDevice(addressDevice);
				Log.i("BluetoothPlugin","BT Device in state "+device.getBondState());	
				
				boolean state = false;
				
				if (device!=null && device.getBondState()==12) 
					state =  true;
				else
					state = false;
				
				Log.d("BluetoothPlugin","Is Bound with " + device.getName()+" - address "+device.getAddress());	          				
				
				Log.d("BluetoothPlugin - "+ACTION_IS_BOUND_BT, "Returning "+ "Result: "+state);
				callbackContext.success("" + state);
				//result = new PluginResult(Status.OK, state);
				return true;	
			} catch (Exception Ex) {
				Log.d("BluetoothPlugin - "+ACTION_IS_BOUND_BT, "Got Exception "+ Ex.getMessage());
				callbackContext.error("boundBTError");
				//result = new PluginResult(Status.ERROR);
				return false;
			}		
		
		
		} else if(ACTION_BT_CONNECT.equals(action)){
			try
			{
				Log.d("BluetoothPlugin", "We're in "+ACTION_BT_CONNECT);
				deviceAddress = "8C:DE:52:99:26:23";
				Log.d("BluetoothPlugin", "We're in "+deviceAddress);
				BluetoothDevice device = btadapter.getRemoteDevice(deviceAddress);
				
				//m = device.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
				//bluetoothSocket = (BluetoothSocket) m.invoke(device, Integer.valueOf(1));
				//SendCommand(0);
				// 用服务号得到socket
				try {
					bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID
							.fromString(MY_UUID));
				} catch (IOException e) {
					//Toast.makeText(this, "连接失败！", Toast.LENGTH_SHORT).show();
				}
				bluetoothSocket.connect();
				sendCommandFlag = 0;
				SendCommand(sendCommandFlag);
				
				
				
				if(bluetoothSocket.isConnected())
				{
					this.isConnection = true;
					String str = "";
					// 打开接收线程
					try {
						inputStream = bluetoothSocket.getInputStream(); // 得到蓝牙数据输入流
						str = ""+1;
					} catch (IOException e) {
						//Toast.makeText(this, "接收数据失败！", Toast.LENGTH_SHORT).show();
						//return;
						str=""+2;
					}
					if (bThread == false) {
						ReadThread.start();
						bThread = true;
						str = ""+3;
					} else {
						bRun = true;
						str = ""+4;
					}
					
					//result = new PluginResult(Status.OK, str);
					 //result.setKeepCallback(true);
			           // callbackContext.sendPluginResult(result);
					callbackContext.success("" + str);
				}
				else {
					//result = new PluginResult(Status.OK, "failure");
					callbackContext.error("Could not connect to ");

				}
				return true;
			} catch (Exception Ex)
			{
				// TODO: handle exception
				Log.d("BluetoothPlugin - "+ACTION_BT_CONNECT, "Got Exception "+ Ex.getMessage());
				//result = new PluginResult(Status.ERROR);
				callbackContext.error("Could not connect to ");
				return false;
			}
		}else if(ACTION_BT_GETDATA.equals(action)){
			try
			{
				Log.d("BluetoothPlugin", "We're in "+ACTION_BT_GETDATA);
				sendCommandFlag = 1;
				SendCommand(sendCommandFlag);	
//				if (bluetoothSocket != null) {
//						if(bluetoothSocket.isConnected()){
//							SendCommand(1);	
//						}
//				}
//					
				//result = new PluginResult(Status.OK, spoValue);
				Log.v("Get1 ", returnBTData);
				while(returnBTData == "")
				{
					Log.v("Get2 ", returnBTData);
				}
				Log.v("Get3 ", returnBTData);
				callbackContext.success("" + returnBTData);
				//ReadThread.cancel();
				
				bluetoothSocket.close();
				bluetoothSocket = null;
				
				return true;
			} catch (Exception Ex)
			{
				// TODO: handle exception
				Log.d("BluetoothPlugin - "+ACTION_BT_GETDATA, "Got Exception "+ Ex.getMessage());
				callbackContext.error("" + "getDataError");
				//result = new PluginResult(Status.ERROR);
				return false;
			}
		}
		
		else {
//			result = new PluginResult(Status.INVALID_ACTION);
//			Log.d("BluetoothPlugin", "Invalid action : "+action+" passed");
//			return result;
			callbackContext.error("" + "actionError");
		}
        return false;
    }
    

    

    
	// 发送按键响应
		public void SendCommand(int i) {
		
			try {
				OutputStream os = bluetoothSocket.getOutputStream(); // 蓝牙连接输出流
				/*
				 * byte[] bos = edit0.getText().toString().getBytes(); for (i = 0; i
				 * < bos.length; i++) { if (bos[i] == 0x0a) n++; } byte[] bos_new =
				 * new byte[bos.length + n]; n = 0; for (i = 0; i < bos.length; i++)
				 * { // 手机中换行为0a,将其改为0d 0a后再发送 if (bos[i] == 0x0a) { bos_new[n] =
				 * 0x0d; n++; bos_new[n] = 0x0a; } else { bos_new[n] = bos[i]; }
				 * n++; }
				 */

				// os.write(bos_new);
				// byte[] tempBytes = HexString2Bytes("4F FF FF FF 02 FF FF B2");
				char[] scanCommand = { 0x4F, 0xFF, 0xFF, 0xFF, 0x02, 0xFF, 0xFF,
						0xB2 };
				char[] fetchDataCommand = { 0x5F, 0x03, 0x00, 0x00, 0x03, 0x01,
						0x36, 0xFE, 0x96 };
				/*
				 * if (edit0.getText().toString() != "5") { //test =
				 * fetchDataCommand; char[] test = { 0x5F, 0x03, 0x00, 0x00, 0x03,
				 * 0x01, 0x36, 0xFE, 0x96}; for (int k = 0; k < test.length; k++) {
				 * new DataOutputStream(os).writeByte(test[k]); } }else { //test =
				 * scanCommand; char[] test = {0x4F, 0xFF, 0xFF, 0xFF, 0x02,
				 * 0xFF,0xFF,0xB2}; for (int k = 0; k < test.length; k++) { new
				 * DataOutputStream(os).writeByte(test[k]); } }
				 */

				char[] test = scanCommand;
				if (i != 0)
					test = fetchDataCommand;
				// char[] test = edit0.getText().toString().toCharArray();
				for (int k = 0; k < test.length; k++) {
					new DataOutputStream(os).writeByte(test[k]);
				}

			} catch (IOException e) {
			}
		}
	public void setDiscovering(boolean state){
		discovering=state;
	}
	
	public void addDevice(BluetoothDevice device){		
		if (!found_devices.contains(device))
		{
			Log.i("BluetoothPlugin","Device stored ");
			found_devices.add(device);
		}
	}
	


	
	
    @Override
	public void onDestroy() {
		// TODO Auto-generated method stub
    	Log.i("BluetoothPlugin","onDestroy "+this.getClass());
    	context.unregisterReceiver(mReceiver);
    	super.onDestroy();
	}
	
 // 接收数据线程
 	Thread ReadThread = new Thread() {

 		public void run() {
 			int num = 0;
 			byte[] buffer = new byte[1024];
 			byte[] buffer_new = new byte[1024];
 			int i = 0;
 			int n = 0;
 			bRun = true;
 			// 接收线程
 			while (true) {
 				try {
 					while (inputStream.available() == 0) {
 						while (bRun == false) {
 						}
 					}
 					while (true) {
 						num = inputStream.read(buffer); // 读入数据
 						n = 0;

 						String s0 = new String(buffer, 0, num);
 						String ret = "";
 						String temp = "";
 						for (int l = 0; l < num; l++) {
 							
// 							String _s = Integer.toHexString(buffer[j] & 0xFF);
//							int spoValue = Integer.parseInt(_s, 16);
//							_s = Integer.toHexString(buffer[j + 1] & 0xFF);
//							int pulseValue = Integer.parseInt(_s, 16);

 							ret += " " + Integer.toHexString(buffer[l] & 0xFF);
 							temp += "|" + Integer.parseInt(Integer.toHexString(buffer[l] & 0xFF),16);
 							
 							Log.v("Get ",
 									"  Index: "
 											+ l
 											+ " HEX: "
 											+ Integer.toHexString(buffer[l] & 0xFF)
 											+ " Dec: " + (buffer[l] & 0xFF));
 						}
 						Log.v("Get ", ret); // Show return Data in String Format
 						Log.v("Get ", temp);
 						
 						if(sendCommandFlag == 0)
 						{
 							//连接操作
 							temp = "";
 						}
 						else
 						{
 							returnBTData = temp;
 						}
 						/*
 						 * fmsg += s0; // 保存收到数据 for (i = 0; i < num; i++) { if
 						 * ((buffer[i] == 0x0d) && (buffer[i + 1] == 0x0a)) {
 						 * buffer_new[n] = 0x0a; i++; } else { buffer_new[n] =
 						 * buffer[i]; } n++; } String s = new String(buffer_new,
 						 * 0, n); smsg += s; // 写入接收缓存
 						 */
 						if (inputStream.available() == 0)
 							break; // 短时间没有数据才跳出进行显示
 					}
 					// 发送显示消息，进行显示刷新
 					//handler.sendMessage(handler.obtainMessage());
 				} catch (IOException e) {
 				}
 			}
 		}
 	};


 	  /** BroadcastReceiver to receive bluetooth events */    
 		private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
 		    public void onReceive(Context context, Intent intent) 
 		    {
 		    	
 		        String action = intent.getAction();
 		        Log.i("BluetoothPlugin","Action: "+action);
 		        
 		        // When discovery finds a device	        
 		        if (BluetoothDevice.ACTION_FOUND.equals(action)) 
 		        {	        	
 		            // Get the BluetoothDevice object from the Intent
 		            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
 		            Log.i("BluetoothPlugin","Device found "+device.getName()+ " "+device.getBondState()+" " + device.getAddress());
 		       
 		            if (device.getBondState() != BluetoothDevice.BOND_BONDED) {	
 		            	Log.i("BluetoothPlugin","Device not paired");
 		            	addDevice(device);
 		            }else Log.i("BluetoothPlugin","Device already paired");	         
 		         
 		        // When discovery starts	
 		        }else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
 		        	
 		        	Log.i("BluetoothPlugin","Discovery started");
 		        	setDiscovering(true);
 		        	
 		        // When discovery finishes	
 	            }else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
 	            	
 	            	Log.i("BluetoothPlugin","Discovery finilized");
 	            	setDiscovering(false); 	   
 		
 	            }
 		    }
 	    };

 	    /**
 	     * This thread runs during a connection with a remote device.
 	     * It handles all incoming and outgoing transmissions.
 	     */
 	    private class ConnectedThread extends Thread {
 	        private final BluetoothSocket mmSocket;
 	        private final InputStream mmInStream;
 	        private final OutputStream mmOutStream;

 	        public ConnectedThread(BluetoothSocket socket) {
 	            Log.d("BluetoothPlugin - ", "create ConnectedThread: " + "secure");
 	            mmSocket = socket;
 	            InputStream tmpIn = null;
 	            OutputStream tmpOut = null;

 	            // Get the BluetoothSocket input and output streams
 	            try {
 	                tmpIn = socket.getInputStream();
 	                tmpOut = socket.getOutputStream();
 	            } catch (IOException e) {
 	                Log.e("BluetoothPlugin - ", "temp sockets not created", e);
 	            }

 	            mmInStream = tmpIn;
 	            mmOutStream = tmpOut;
 	        }

 	        public void run() {
 	            Log.i("BluetoothPlugin - ", "BEGIN mConnectedThread");
 	            byte[] buffer = new byte[1024];
 	            int bytes;

 	            // Keep listening to the InputStream while connected
 	            while (true) {
 	                try {
 	                    // Read from the InputStream
 	                    bytes = mmInStream.read(buffer);
 	                    Log.i("BluetoothPlugin - ", "test111111");
 	               	 	Log.d("BluetoothPlugin - ", "pulseValue:" + bytes);
 	                    // Send the obtained bytes to the UI Activity
 	                    //handler.obtainMessage(BluetoothChat.MESSAGE_READ, bytes, -1, buffer)
 	                    //handler.obtainMessage(2, bytes, -1, buffer)
 	                            //.sendToTarget();
 	                    if (bytes > 0) {
 							int i = 0;
 							while ((buffer[i] & 0xFF) == 00) {
 								i++;
 							}
 							int j = i;
 							String _s = Integer.toHexString(buffer[j] & 0xFF);
 							int spoValue = Integer.parseInt(_s, 16);
 							_s = Integer.toHexString(buffer[j + 1] & 0xFF);
 							int pulseValue = Integer.parseInt(_s, 16);
 							
 							 Log.d("BluetoothPlugin - ", "pulseValue:" + pulseValue);
 							Message message = new Message();
 						message.arg1 = spoValue;
 							message.arg2 = pulseValue;
 						handler.sendMessage(message);

// 							System.out.println(Integer.toString(spoValue));
// 							System.out.println(Integer.toString(pulseValue));
 							// String s2 = new String(buf_Pulsedata);
 						}
 	                } catch (IOException e) {
 	                    Log.e("BluetoothPlugin - ", "disconnected", e);
 	                    //connectionLost();
 	                    // Start the service over to restart listening mode
 	                    //BluetoothChatService.this.start();
 	                    break;
 	                }
 	            }
 	        }

 	        /**
 	         * Write to the connected OutStream.
 	         * @param buffer  The bytes to write
 	         */
 	        public void write(byte[] buffer) {
 	            try {
 	                mmOutStream.write(buffer);

 	                // Share the sent message back to the UI Activity
 	                //handler.obtainMessage(BluetoothChat.MESSAGE_WRITE, -1, -1, buffer)
 	                handler.obtainMessage(3, -1, -1, buffer)
 	                        .sendToTarget();
 	            } catch (IOException e) {
 	                Log.e("BluetoothPlugin - ", "Exception during write", e);
 	            }
 	        }

 	        public void cancel() {
 	            try {
 	                mmSocket.close();
 	            } catch (IOException e) {
 	                Log.e("BluetoothPlugin - ", "close() of connect socket failed", e);
 	            }
 	        }
 	    }

 	   
 	    


    /** BroadcastReceiver to receive bluetooth events */    
//	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
//	    public void onReceive(Context context, Intent intent) 
//	    {
//	    	
//	        String action = intent.getAction();
//	        Log.i("BluetoothPlugin","Action: "+action);
//	        
//	        // When discovery finds a device	        
//	        if (BluetoothDevice.ACTION_FOUND.equals(action)) 
//	        {	        	
//	            // Get the BluetoothDevice object from the Intent
//	            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//	            Log.i("BluetoothPlugin","Device found "+device.getName()+ " "+device.getBondState()+" " + device.getAddress());
//	       
//	            if (device.getBondState() != BluetoothDevice.BOND_BONDED) {	
//	            	Log.i("BluetoothPlugin","Device not paired");
//	            	addDevice(device);
//	            }else Log.i("BluetoothPlugin","Device already paired");	         
//	         
//	        // When discovery starts	
//	        }else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
//	        	
//	        	Log.i("BluetoothPlugin","Discovery started");
//	        	setDiscovering(true);
//	        	
//	        // When discovery finishes	
//            }else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
//            	
//            	Log.i("BluetoothPlugin","Discovery finilized");
//            	setDiscovering(false); 	   
//	
//            }
//	    }
//    };

    
}


