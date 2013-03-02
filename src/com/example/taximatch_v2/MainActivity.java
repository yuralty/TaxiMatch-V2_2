package com.example.taximatch_v2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.cn.apis.util.Constants;
import com.amap.mapapi.core.GeoPoint;
import com.amap.mapapi.core.PoiItem;
import com.amap.mapapi.map.MapActivity;
import com.amap.mapapi.map.MapController;
import com.amap.mapapi.map.MapView;
import com.amap.mapapi.map.MyLocationOverlay;
import com.amap.mapapi.map.PoiOverlay;

public class MainActivity extends MapActivity implements ActionBar.TabListener {

	int ClientType = 0; // pas=1, dri=2
	int userID = -1;
	int psgID;
	boolean ConnectState = false;
	boolean DriverBusy = false;
	boolean DriverTimeout = false;
	boolean DriverReceiveArrangment = false;
	Button bpConnect, bdConnect, bDisconnect, bsExit;
	Button bpCallTaxi, bpReport, bpExit;
	Button bdFree, bdBusy, bdExit;

	TextView tvID, tvMatch;

	Client client;
	String serverIP = "192.168.1.26";
	int port = 5679;
	MessageUtil mMessageUtil = new MessageUtil();

	private MapView mMapView;
	private MapController mMapController;
	private GeoPoint myLocation;
	private GeoPoint applyLocation;
	private MyLocationOverlay mLocationOverlay;
	private myPoiOverlay mPoiOverlay;

	private PassengerDenyThread passengerDenyThread;
	private DriverUpdateLocationThread driverUpdateLocationThread;

	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.what == Constants.FIRST_LOCATION) {
				mMapController.animateTo(mLocationOverlay.getMyLocation());

				if (ClientType == 2) {
					try {
						myLocation = mLocationOverlay.getMyLocation();
						if(locationValid(myLocation)) {
							double lnt = ((double) myLocation.getLongitudeE6()) / 1000000;
							double lat = ((double) myLocation.getLatitudeE6()) / 1000000;
							client.send_7(lnt, lat);
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						Toast.makeText(getApplicationContext(), e.getMessage(),
								Toast.LENGTH_LONG).show();
					}
				}

			}
		}
	};

	public class myPoiOverlay extends PoiOverlay {

		boolean tapped = false;

		myPoiOverlay(Drawable marker, List<PoiItem> list) {
			super(marker, list);
		}

		@Override
		public boolean onTap(GeoPoint gp, MapView mv) {
			// TODO Auto-generated method stub
			if (!tapped) {
				tapped = true;
				AlertDialog.Builder builder = new Builder(MainActivity.this);
				builder.setMessage("Accept or Not?");
				builder.setPositiveButton("Yes", new Dialog.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
						if (!DriverTimeout) {
							try {
								client.send_10(userID, psgID);
							} catch (IOException e) {
								e.printStackTrace();
								Toast.makeText(getApplicationContext(),
										e.getMessage(), Toast.LENGTH_LONG)
										.show();
							}
						} else {
							myPoiOverlay.this.removeFromMap();
							Toast.makeText(getApplicationContext(),
									"Accept Fail. Error: Timeout",
									Toast.LENGTH_LONG).show();
						}
					}
				});
				builder.setNegativeButton("No", new Dialog.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
						if (!DriverTimeout) {
							try {
								client.send_11(userID, psgID);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								Toast.makeText(getApplicationContext(),
										e.getMessage(), Toast.LENGTH_LONG)
										.show();
							}
						} else {
							Toast.makeText(getApplicationContext(),
									"Refuse Fail. Error: Timeout",
									Toast.LENGTH_LONG).show();
						}
						myPoiOverlay.this.removeFromMap();
					}
				});
				builder.create().show();
			}
			DriverReceiveArrangment = false;
			return super.onTap(gp, mv);
		}
	}

	// 处理接受消息的handler
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			int msgType = mMessageUtil.parseMsg(msg);
			
			if (msgType == 0) {
				int lastMsgType = mMessageUtil.parseACK(msg);
				if(lastMsgType == 1 || lastMsgType == 2) {
					Toast.makeText(getApplicationContext(), "发送ID申请成功",
							Toast.LENGTH_SHORT).show();
				}
				else if(lastMsgType == 4) {
					Toast.makeText(getApplicationContext(), "发送叫车申请成功",
							Toast.LENGTH_SHORT).show();
				}
				else if(lastMsgType == 6) {
					Toast.makeText(getApplicationContext(), "发送撤销订单申请成功",
							Toast.LENGTH_SHORT).show();
				}
				else if(lastMsgType == 7) {
					Toast.makeText(getApplicationContext(), "发送空车报告成功",
							Toast.LENGTH_SHORT).show();
				}
				else if(lastMsgType == 8) {
					Toast.makeText(getApplicationContext(), "发送载客报告成功",
							Toast.LENGTH_SHORT).show();
				}
				else if(lastMsgType == 10) {
					Toast.makeText(getApplicationContext(), "发送接受指派报告成功",
							Toast.LENGTH_SHORT).show();
				}
				else if(lastMsgType == 11) {
					Toast.makeText(getApplicationContext(), "发送拒绝指派报告成功",
							Toast.LENGTH_SHORT).show();
				}
				else if(lastMsgType == 15) {
					Toast.makeText(getApplicationContext(), "发送乘客逃单报告成功",
							Toast.LENGTH_SHORT).show();
				}
				else if(lastMsgType == 22) {
					Toast.makeText(getApplicationContext(), "发送乘客被接到报告成功",
							Toast.LENGTH_SHORT).show();
				}
				else if(lastMsgType == 23) {
					Toast.makeText(getApplicationContext(), "发送接到乘客报告成功",
							Toast.LENGTH_SHORT).show();
				}
			}

			else if (msgType == 3) {
				client.id = userID = mMessageUtil.parseRecMsg_3(msg);
				if (tvID != null)
					tvID.setText("ID: " + userID);
				Toast.makeText(getApplicationContext(), "获得了ID： " + userID,
						Toast.LENGTH_LONG).show();
			}

			else if (msgType == 5) {
				Drawable marker = getResources().getDrawable(
						R.drawable.da_marker_red);
				marker.setBounds(0, 0, marker.getIntrinsicWidth(),
						marker.getIntrinsicHeight());
				mMessageUtil.parseRecMsg_5(msg, mMapView, marker, mPoiOverlay);
				Toast.makeText(getApplicationContext(), "获得了附近的出租车信息",
						Toast.LENGTH_LONG).show();
			}

			else if (msgType == 9) {
				DriverTimeout = false;
				DriverReceiveArrangment = true;
				Drawable marker = getResources().getDrawable(
						R.drawable.da_marker_red);
				marker.setBounds(0, 0, marker.getIntrinsicWidth(),
						marker.getIntrinsicHeight());
				String str = (String) msg.obj;
				String[] split = str.split(" ");
				psgID = Integer.parseInt(split[2]);
				List<PoiItem> list = new ArrayList<PoiItem>();
				int lnt, lat;
				lnt = (int) (Double.parseDouble(split[4]) * 1E6);
				lat = (int) (Double.parseDouble(split[5]) * 1E6);
				GeoPoint pt = new GeoPoint(lat, lnt);
				PoiItem pi = new PoiItem("" + psgID, pt, "passenger ID: "
						+ psgID, "Passenger Location");
				list.add(pi);
				if (mPoiOverlay != null) {
					try {
						mPoiOverlay.removeFromMap();
					} catch (Exception e) {
					}
				}
				mPoiOverlay = new myPoiOverlay(marker, list);
				mPoiOverlay.addToMap(mMapView);

				Toast.makeText(getApplicationContext(), "接收到终端的安排",
						Toast.LENGTH_LONG).show();

			}

			else if (msgType == 12) {
				AlertDialog.Builder builder = new Builder(MainActivity.this);
				builder.setMessage("arrange success");
				builder.setNegativeButton("yeah!", null);
				builder.create().show();
				passengerDenyThread = new PassengerDenyThread();
				passengerDenyThread.start();
			}

			else if (msgType == 13) {
				AlertDialog.Builder builder = new Builder(MainActivity.this);
				builder.setMessage("arrange fail");
				builder.setNegativeButton("uh...", null);
				builder.create().show();
			}

			else if (msgType == 20) {
				DriverTimeout = true;
				if (mPoiOverlay != null) {
					mPoiOverlay.removeFromMap();
					Toast.makeText(getApplicationContext(), "取消了申请",
							Toast.LENGTH_LONG).show();
				}

			}

			else {
				Toast.makeText(getApplicationContext(), "收到了未知消息",
						Toast.LENGTH_LONG).show();
			}
		}
	};

	// tab属性设置
	private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Set up the action bar to show tabs.
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// For each of the sections in the app, add a tab to the action bar.
		actionBar.addTab(actionBar.newTab().setText(R.string.title_settings)
				.setTabListener(this));
		actionBar.addTab(actionBar.newTab().setText(R.string.title_map)
				.setTabListener(this));
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		// Restore the previously serialized current tab position.
		if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
			getActionBar().setSelectedNavigationItem(
					savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		// Serialize the current tab position.
		outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getActionBar()
				.getSelectedNavigationIndex());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public void onTabSelected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, show the tab contents in the
		// container view.
		if (tab.getPosition() == 0) {
			setupSettings();
		} else if (tab.getPosition() == 1) {
			setupClient();
			this.mLocationOverlay.enableMyLocation();
		}
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	// 连接设置界面，包含乘客连接，司机连接，取消连接和退出四个按钮
	private void setupSettings() {
		OnClickListener pConnectListener = new OnClickListener() {
			public void onClick(View v) {
				if (!ConnectState) {
					EditText etIP = (EditText) findViewById(R.id.editTextIP);
					EditText etPort = (EditText) findViewById(R.id.editTextPort);
					serverIP = etIP.getText().toString();
					port = Integer.parseInt(etPort.getText().toString());
					try {
						client = new Client(serverIP, port, mHandler, userID);
						ClientType = 1;
						ConnectState = true;
						Toast.makeText(getApplicationContext(),
								"Connect to Server Success", Toast.LENGTH_LONG)
								.show();
					} catch (Exception e) {
						Log.i("error", "" + e.getMessage());
						Toast.makeText(getApplicationContext(), e.getMessage(),
								Toast.LENGTH_LONG).show();
					}

				} else {
					Toast.makeText(getApplicationContext(),
							"Already Connected", Toast.LENGTH_LONG).show();

				}
			}
		};

		OnClickListener dConnectListener = new OnClickListener() {
			public void onClick(View v) {
				if (!ConnectState) {
					EditText etIP = (EditText) findViewById(R.id.editTextIP);
					EditText etPort = (EditText) findViewById(R.id.editTextPort);
					serverIP = etIP.getText().toString();
					port = Integer.parseInt(etPort.getText().toString());
					try {
						client = new Client(serverIP, port, mHandler, userID);
						ClientType = 2;
						ConnectState = true;
						Toast.makeText(getApplicationContext(),
								"Connect to Server Success", Toast.LENGTH_LONG)
								.show();
					} catch (Exception e) {
						Log.i("error", "" + e.getMessage());
						Toast.makeText(getApplicationContext(), e.getMessage(),
								Toast.LENGTH_LONG).show();
					}

				} else {
					Toast.makeText(getApplicationContext(),
							"Already Connected", Toast.LENGTH_LONG).show();
				}
			}
		};

		OnClickListener disconnectListener = new OnClickListener() {
			public void onClick(View v) {
				if (ConnectState) {
					try {
						client = null;
						ClientType = 0;
						ConnectState = false;
						Toast.makeText(getApplicationContext(),
								"Disconnect Success", Toast.LENGTH_LONG).show();
					} catch (Exception e) {
						Toast.makeText(getApplicationContext(), e.getMessage(),
								Toast.LENGTH_LONG).show();
					}
				} else {
					Toast.makeText(getApplicationContext(), "Not Connected",
							Toast.LENGTH_LONG).show();

				}
			}
		};

		this.setContentView(R.layout.settings);
		bpConnect = (Button) findViewById(R.id.bpConnect);
		bdConnect = (Button) findViewById(R.id.bdConnect);
		bDisconnect = (Button) findViewById(R.id.bDisconnect);
		bsExit = (Button) findViewById(R.id.bsExit);
		bpConnect.setOnClickListener(pConnectListener);
		bdConnect.setOnClickListener(dConnectListener);
		bDisconnect.setOnClickListener(disconnectListener);
		bsExit.setOnClickListener(exitListener);
	}

	// 地图界面，根据客户类型进入不同的界面
	private void setupClient() {
		if (ClientType == 1) {
			this.setContentView(R.layout.passengerlayout);

			if (userID == -1) {
				try {
					client.send_1();
				} catch (Exception e) {
					Toast.makeText(getApplicationContext(), e.getMessage(),
							Toast.LENGTH_LONG).show();
				}
			}

			tvID = (TextView) findViewById(R.id.pId);
			if(userID != -1) {
				tvID.setText("ID: " + userID);
			}
			bpCallTaxi = (Button) findViewById(R.id.bpCallTaxi);
			bpReport = (Button) findViewById(R.id.bpReport);
			bpExit = (Button) findViewById(R.id.bpExit);
			bpCallTaxi.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					myLocation = mLocationOverlay.getMyLocation();

					if (myLocation == null) {
						Toast.makeText(getApplicationContext(),
								"Location NULL", Toast.LENGTH_LONG).show();
						return;
					} else {
						try {
							applyLocation = mLocationOverlay.getMyLocation();
							double lnt = ((double) myLocation.getLongitudeE6()) / 1000000;
							double lat = ((double) myLocation.getLatitudeE6()) / 1000000;
							client.send_4(lnt, lat);
							new PassengerDenyThread().start();
						} catch (Exception e) {
							e.printStackTrace();
							Toast.makeText(getApplicationContext(),
									e.getMessage(), Toast.LENGTH_LONG).show();
						}
					}
				}
			});
			
			bpReport.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View arg0) {
					AlertDialog.Builder builder = new Builder(MainActivity.this);
					builder.setMessage("Report picked or not");
					builder.setPositiveButton("Yes", new Dialog.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
							try {
								client.send_22();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					});
					builder.setNegativeButton("No", new Dialog.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
					});
					builder.create().show();
					
				}
			});

			bpExit.setOnClickListener(exitListener);

			mMapView = (MapView) findViewById(R.id.mapView);
			mMapView.setBuiltInZoomControls(true);
			mMapController = mMapView.getController();

			// 初始化地图中心点
			if (mLocationOverlay != null) {
				myLocation = mLocationOverlay.getMyLocation();
				mMapController.setCenter(myLocation);
			} else {
				mMapController.setCenter(new GeoPoint((int) (33.90923 * 1E6),
						(int) (121.397428 * 1E6)));
			}

			mMapController.setZoom(16);

			// 绑定位置监视
			mLocationOverlay = new MyLocationOverlay(this, mMapView);
			mMapView.getOverlays().add(mLocationOverlay);
			mLocationOverlay.runOnFirstFix(new Runnable() {
				public void run() {
					handler.sendMessage(Message.obtain(handler,
							Constants.FIRST_LOCATION));
				}
			});

		}

		// 司机界面操作
		else if (ClientType == 2) {

			if (userID == -1) {
				try {
					client.send_2();
				} catch (Exception e) {
					Log.i("error", "" + e.getMessage());
					Toast.makeText(getApplicationContext(), e.getMessage(),
							Toast.LENGTH_LONG).show();
				}
			}

			this.setContentView(R.layout.driverlayout);
			bdFree = (Button) findViewById(R.id.bdFree);
			bdBusy = (Button) findViewById(R.id.bdReport);
			bdExit = (Button) findViewById(R.id.bdExit);
			tvID = (TextView) findViewById(R.id.dId);
			if(userID != -1) {
				tvID.setText("ID: " + userID);
			}

			bdFree.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (!DriverReceiveArrangment) {
						DriverBusy = false;
						driverUpdateLocationThread = new DriverUpdateLocationThread();
						driverUpdateLocationThread.start();
						try {
							myLocation = mLocationOverlay.getMyLocation();
							double lnt = ((double) myLocation.getLongitudeE6()) / 1000000;
							double lat = ((double) myLocation.getLatitudeE6()) / 1000000;
							client.send_7(lnt, lat);
						} catch (Exception e) {
							e.printStackTrace();
						}
						Toast.makeText(getApplicationContext(),
								"改变状态为空车", Toast.LENGTH_LONG)
								.show();
					} else {
						Toast.makeText(getApplicationContext(),
								"State ChangedFail", Toast.LENGTH_LONG).show();
					}

				}
			});
			bdBusy.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
//					if (!DriverReceiveArrangment) {
//						DriverBusy = true;
//						driverUpdateLocationThread.flag = false;
//						Toast.makeText(
//								getApplicationContext(),
//								driverUpdateLocationThread.getState()
//										.toString(), Toast.LENGTH_LONG).show();
//						try {
//							client.send_8();
//						} catch (Exception e) {
//							e.printStackTrace();
//						}
//						Toast.makeText(getApplicationContext(),
//								"改变状态为载客", Toast.LENGTH_LONG)
//								.show();
//					} else {
//						Toast.makeText(getApplicationContext(),
//								"State ChangedFail", Toast.LENGTH_LONG).show();
//					}
					AlertDialog.Builder builder = new Builder(MainActivity.this);
					builder.setMessage("Report picked or not");
					builder.setPositiveButton("Yes", new Dialog.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
							try {
								client.send_23();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					});
					builder.setNegativeButton("No", new Dialog.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
					});
					builder.create().show();

				}
			});

			bdExit.setOnClickListener(exitListener);

			mMapView = (MapView) findViewById(R.id.mapView);
			mMapView.setBuiltInZoomControls(true);
			mMapController = mMapView.getController();

			// 初始化地图中心点
			if (mLocationOverlay != null) {
				myLocation = mLocationOverlay.getMyLocation();
				mMapController.setCenter(myLocation);
			} else {
				mMapController.setCenter(new GeoPoint((int) (33.90923 * 1E6),
						(int) (121.397428 * 1E6)));
			}

			mMapController.setZoom(16);

			// 绑定位置监视
			mLocationOverlay = new MyLocationOverlay(this, mMapView);
			mMapView.getOverlays().add(mLocationOverlay);

			mLocationOverlay.runOnFirstFix(new Runnable() {
				public void run() {
					handler.sendMessage(Message.obtain(handler,
							Constants.FIRST_LOCATION));
				}
			});
			driverUpdateLocationThread = new DriverUpdateLocationThread();
			driverUpdateLocationThread.start();

		}

	}

	private OnClickListener exitListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (client != null) {
				try {
					client.send_14();
				} catch (IOException e) {
					e.printStackTrace();
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			android.os.Process.killProcess(android.os.Process.myPid());
		}
	};

	class PassengerDenyThread extends Thread {

		public boolean flag = true;
		public void run() {
			
			while (flag) {
				
				myLocation = mLocationOverlay.getMyLocation();
				if(myLocation != null) {
					int latDifference = Math.abs(applyLocation.getLatitudeE6()
							- myLocation.getLatitudeE6());
					int lntDifference = Math.abs(applyLocation.getLongitudeE6()
							- myLocation.getLongitudeE6());
					if (latDifference > 10000 || lntDifference > 10000) {
						try {
							client.send_15();
						} catch (IOException e) {
							e.printStackTrace();
						}
						flag = false;
					}
				}
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}

	class DriverUpdateLocationThread extends Thread {
		public boolean flag = true;

		public void run() {
			while (flag) {

				try {
					myLocation = mLocationOverlay.getMyLocation();
					if (myLocation != null) {
						double lnt = ((double) myLocation.getLongitudeE6()) / 1000000;
						double lat = ((double) myLocation.getLatitudeE6()) / 1000000;
						client.send_24(lnt, lat); //稍后改为24
					}

				} catch (IOException e) {
					e.printStackTrace();
				}
				try {
					Thread.sleep(15000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private boolean locationValid(GeoPoint gp) {
		if(gp == null) return false;
		if(gp.getLongitudeE6() < 0) return false;
		if(gp.getLatitudeE6() < 0) return false;
		return true;
	}

	@Override
	protected void onPause() {
		if (mLocationOverlay != null) {
			this.mLocationOverlay.disableMyLocation();
		}
		super.onPause();
	}

	@Override
	protected void onResume() {
		if (mLocationOverlay != null) {
			this.mLocationOverlay.enableMyLocation();
		}
		super.onResume();
	}

}
