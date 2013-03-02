package com.example.taximatch_v2;

import java.util.ArrayList;
import java.util.List;

import android.graphics.drawable.Drawable;
import android.os.Message;

import com.amap.mapapi.core.GeoPoint;
import com.amap.mapapi.core.PoiItem;
import com.amap.mapapi.map.MapView;
import com.amap.mapapi.map.PoiOverlay;
import com.example.taximatch_v2.MainActivity.myPoiOverlay;

public class MessageUtil {

	// 处理接收消息的函数
	public int parseMsg(Message msg) {
		String str = (String) msg.obj;
		String[] split = str.split(" ");

		if (split.length == 3)
			return 3;
		else if (split[2].equals("neartaxi"))
			return 5;
		else if (split[3].equals("order"))
			return 9;
		else if (split[3].equals("arrangesuccess"))
			return 12;
		else if (split[2].equals("arrangefail"))
			return 13;
		else if (split[2].equals("taxlongtime"))
			return 20;
		else if(split[2].equals("ack"))
			return 0;
		else
			return -1;
	}
	
	// 解析ACK，返回消息类型id
	public int parseACK(Message msg) {
		String str = (String) msg.obj;
		String[] split = str.split(" ");
		return Integer.parseInt(split[3]);
	}

	// 解析消息3，返回id
	public int parseRecMsg_3(Message msg) {
		String str = (String) msg.obj;
		String[] split = str.split(" ");
		return Integer.parseInt(split[1]);

	}

	// 解析消息5的texi坐标并绘制在地图上
	public void parseRecMsg_5(Message msg, MapView mMapView, Drawable marker,
			PoiOverlay mPoiOverlay) {
		String str = (String) msg.obj;
		String[] split = str.split(" ");
		List<PoiItem> list = new ArrayList<PoiItem>();
		for (int i = 3; i < split.length - 1; i += 2) {
			int lnt, lat;
			lnt = (int) (Double.parseDouble(split[i]) * 1E6);
			lat = (int) (Double.parseDouble(split[i + 1]) * 1E6);
			GeoPoint pt = new GeoPoint(lat, lnt);
			int id = (i - 1) / 2;
			PoiItem pi = new PoiItem("" + id, pt, "car No." + id, "Status Free");
			list.add(pi);
		}
		if (mPoiOverlay != null) {
			try {
				mPoiOverlay.removeFromMap();
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		mPoiOverlay = new PoiOverlay(marker, list);
		mPoiOverlay.addToMap(mMapView);

	}

	// 解析消息9，返回乘客id
	public int parseRecMsg_9(Message msg, MapView mMapView, Drawable marker,
			PoiOverlay mPoiOverlay) {
		String str = (String) msg.obj;
		String[] split = str.split(" ");
		int psg = Integer.parseInt(split[2]);
		List<PoiItem> list = new ArrayList<PoiItem>();
		int lnt, lat;
		lnt = (int) (Double.parseDouble(split[4]) * 1E6);
		lat = (int) (Double.parseDouble(split[5]) * 1E6);
		GeoPoint pt = new GeoPoint(lat, lnt);
		PoiItem pi = new PoiItem("" + psg, pt, "passenger ID: " + psg,
				"Passenger Location");
		list.add(pi);
		if (mPoiOverlay != null) {
			try {
				mPoiOverlay.removeFromMap();
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		mPoiOverlay = new PoiOverlay(marker, list);
		mPoiOverlay.addToMap(mMapView);
		return psg;
	}

	// 解析消息9,12中的乘客和司机id
	public PDpair parsePair(Message msg) {
		String str = (String) msg.obj;
		String[] split = str.split(" ");
		int psg, dri;
		psg = Integer.parseInt(split[2]);
		dri = Integer.parseInt(split[1]);
		PDpair pair = new PDpair(psg, dri);
		return pair;
	}

	public class PDpair {
		public int psg;
		public int dri;

		PDpair(int psg, int dri) {
			this.psg = psg;
			this.dri = dri;
		}
	}

}
