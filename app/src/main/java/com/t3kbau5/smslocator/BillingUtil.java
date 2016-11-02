package com.t3kbau5.smslocator;

import java.lang.reflect.Method;
import java.util.ArrayList;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.android.vending.billing.IInAppBillingService;

public class BillingUtil {

	Context context;
	IInAppBillingService serv;
	ServiceConnection servConn;
	
	public boolean connected = false;
	public boolean hasPremium = false;
	public String payload;
	
	public static int REQUEST_CODE = 2014;
	
	public BillingUtil(Context context, boolean bind){
		this.context = context;
		if(bind) bind();
	}
	
	public BillingUtil(Context context){
		this.context = context;
	}
	
	public IInAppBillingService bind(){
    	
    	servConn = new ServiceConnection() {
    		   @Override
    		   public void onServiceDisconnected(ComponentName name) {
    		       serv = null;
    		   }

    		   @Override
    		   public void onServiceConnected(ComponentName name, 
    		      IBinder service) {
    		       serv = IInAppBillingService.Stub.asInterface(service);
    		       connected = true;
    		       try {
					hasPremium = checkPremium();
				} catch (RemoteException e) {
					hasPremium = false;
					e.printStackTrace();
				}
    		   }
    		};
    		
    	context.bindService(new Intent("com.android.vending.billing.InAppBillingService.BIND").setPackage("com.android.vending"), servConn, Context.BIND_AUTO_CREATE);
    	return serv;
    }
	
	public void unbind(){
		context.unbindService(servConn);
		connected = false;
	}
    
    public boolean checkPremium() throws RemoteException{
    	if(!connected) bind();
    	Bundle ownedItems = serv.getPurchases(3, context.getPackageName(), "inapp", null);
    	
    	int response = ownedItems.getInt("RESPONSE_CODE");
    	if (response == 0) {
    	   ArrayList<String> ownedSkus =
    	      ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
    	   ArrayList<String>  purchaseDataList =
    	      ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
    	   ArrayList<String>  signatureList =
    	      ownedItems.getStringArrayList("INAPP_DATA_SIGNATURE");
    	   String continuationToken = 
    	      ownedItems.getString("INAPP_CONTINUATION_TOKEN");
    	   
    	   
    	   for (int i = 0; i < purchaseDataList.size(); i++) {
    	      String purchaseData = purchaseDataList.get(i);
    	      //String signature = signatureList.get(i);
    	      String sku = ownedSkus.get(i);
    	  
    	      if(sku.equals("premium")){
    	    	  return true;
    	      }
    	   } 

    	   // if continuationToken != null, call getPurchases again 
    	   // and pass in the token to retrieve more items
    	}

    	return false;
    }
    
    public void buyPremium(Activity act) throws RemoteException, SendIntentException{
    	if(!connected) bind();
    	if(checkPremium()) return;
    	payload = Utils.randomBase64();
    	Bundle buyIntentBundle = serv.getBuyIntent(3, context.getPackageName(), "premium", "inapp", payload);
    	
    	PendingIntent pi = buyIntentBundle.getParcelable("BUY_INTENT");
    	act.startIntentSenderForResult(pi.getIntentSender(),
    			   REQUEST_CODE, new Intent(), Integer.valueOf(0), Integer.valueOf(0),
    			   Integer.valueOf(0));
    	
    }
    
    public boolean isConnected(){
    	return connected;
    }
    
    public boolean hasPremium(){
    	return hasPremium;
    }
	
}
