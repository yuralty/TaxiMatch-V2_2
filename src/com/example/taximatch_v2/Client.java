package com.example.taximatch_v2;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

//使用了Handler概念的Client类，类中有一个子线程

public class Client {

	Socket client;
	String hostip;
	int port;
	int id;
	int ack = 0;

	String msg, rec;

	private Handler mMainHandler;
	private Handler mChildHandler;

	// 构造函数中建立与服务端连接，参数为服务端IP地址、端口号
	public Client(String hip, int portn, Handler main, int userID) {
		hostip = hip;
		port = portn;
		id = userID;
		mMainHandler = main;
		new ClientChildThread().start();
		new RecThread().start();
	}
	
	class RecThread extends Thread {
		
		BufferedReader in;
		
		public void run() {
			while(true) {
				if(client!=null){
					try {
						in = new BufferedReader(new InputStreamReader(
								client.getInputStream()));
					} catch (IOException e) {
						e.printStackTrace();
						System.out.println(e.getMessage());
					}
					
					try {
						rec = in.readLine();
						Message toMain = mMainHandler.obtainMessage();// 使用UI线程的Handler，返回信息
						toMain.obj = rec;
						mMainHandler.sendMessage(toMain);
					} catch (IOException e) {
						e.printStackTrace();
						System.out.println(e.getMessage());
					}
				}
			}
		}
	}
	

	class ClientChildThread extends Thread {

		BufferedReader in;
		DataOutputStream out;

		public void run() {

			Looper.prepare();// 准备Looper，监听从主线程发来的信息

			try {
				client = new Socket(hostip, port);
				in = new BufferedReader(new InputStreamReader(
						client.getInputStream()));
				out = new DataOutputStream(client.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println(e.getMessage());
			}

			mChildHandler = new Handler() {
				public void handleMessage(Message msg) {
					String tmp = (String) msg.obj;
					System.out.println("in child handleMessage");

					// 解析不同的消息，执行相应的操作--发送 或 接收
					if (tmp.split(" ")[0].equals("send")) {
						try {
							System.out.println("it is send msg");
							ChildSendMsg(tmp.substring(5));
						} catch (IOException e) {
							e.printStackTrace();
							System.out.println(e.getMessage());
						}
					}

					if (tmp.equals("receive")) {
						try {
							rec = ChildReceiveMsg();
							Message toMain = mMainHandler.obtainMessage();// 使用UI线程的Handler，返回信息
							toMain.obj = rec;
							mMainHandler.sendMessage(toMain);
						} catch (IOException e) {
							e.printStackTrace();
							System.out.println(e.getMessage());
						}
					}
				}
			};

			Looper.loop();

		}

		// finalized函数，类似于析构函数
		protected void finalized() {
			try {
				client.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println(e.getMessage());
			}
		}

		// 把一个字符串发送到服务端
		public void ChildSendMsg(String msg) throws IOException {
			out.writeBytes(msg + '\n');
			out.flush();
		}

		// 接收服务端发来的信息，返回一个String类型
		public String ChildReceiveMsg() throws IOException {

			String tmp = in.readLine();
			return tmp;

		}
	}

	// 把一个字符串发送到服务端，调用子线程的发送函数
	public void SendMsg(String msg) {
		if (client != null) {
			Message toChild = mChildHandler.obtainMessage();
			toChild.obj = "send" + " " + msg;
			mChildHandler.sendMessage(toChild);
		}
	}


	// 下面是乘客发送消息的命令
	public void send_1() throws IOException, InterruptedException {
		msg = "req" + " " + "pasid" + " " + "end";
		SendMsg(msg);
	}

	public void send_4(double lnt, double lat) throws IOException,
			InterruptedException {
		msg = "req" + " " + id + " " + "pasreq" + " " + lnt + " " + lat + " "
				+ "end";
		SendMsg(msg);
	}

	public void send_6() throws IOException, InterruptedException {
		msg = "req" + " " + id + " " + "pasdel" + " " + "end";
		SendMsg(msg);
	}

	public void send_14() throws IOException {
		msg = "req" + " " + id + " " + "fin" + " " + "end";
		SendMsg(msg);
	}
	
	public void send_15() throws IOException {
		msg = "req" + " " + id + " " + "pasdeny" + " " + "end";
		SendMsg(msg);
	}
	
	public void send_22() throws IOException {
		msg = "req" + " " + id + " " + "paspick" + " " + "end";
		SendMsg(msg);
	}

	// 下面是司机发送消息的命令
	public void send_2() throws IOException {
		msg = "req" + " " + "taxid" + " " + "end";
		SendMsg(msg);
	}

	public void send_7(double lnt, double lat) throws IOException {
		msg = "req" + " " + id + " " + "taxemp" + " " + lnt + " " + lat + " "
				+ "end";
		SendMsg(msg);
	}

	public void send_8() throws IOException {
		msg = "req" + " " + id + " " + "taxful" + " " + "end";
		SendMsg(msg);
	}
	
	public void send_10(int dri, int psg) throws IOException {
		msg = "req" + " " + dri + " " + psg + " " + "taxyes" + " " + "end";
		SendMsg(msg);
	}
	
	public void send_11(int dri, int psg) throws IOException {
		msg = "req" + " " + dri + " " + psg + " " + "taxno" + " " + "end";
		SendMsg(msg);
	}
	
	public void send_23() throws IOException {
		msg = "req" + " " + id + " " + "taxpick" + " " + "end";
		SendMsg(msg);
	}
	
	public void send_24(double lnt, double lat) throws IOException {
		msg = "req" + " " + id + " " + "realtime" + " " + lnt + " " + lat + " " + "end";
		SendMsg(msg);
	}
	

}
